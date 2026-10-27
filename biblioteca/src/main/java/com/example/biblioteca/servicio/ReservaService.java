package com.example.biblioteca.servicio;

import com.example.biblioteca.modelo.ReservaRespuesta;
import com.example.biblioteca.modelo.SolicitudReserva;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservaService {

    private final JdbcTemplate jdbc;

    public ReservaService(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaRespuesta reservar(
            UUID operacionId,
            SolicitudReserva solicitud) {

        if (operacionId == null
                || solicitud == null
                || solicitud.libroId() == null
                || solicitud.libroId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La operación y el identificador del libro son obligatorios"
            );
        }

        int libroId = solicitud.libroId();

        // 1. Recuperar una operación ya confirmada.
        List<ReservaRespuesta> existentes = buscar(operacionId);

        if (!existentes.isEmpty()) {
            return comprobarMismoLibro(
                    existentes.get(0),
                    libroId
            );
        }

        // 2. Comprobar que el libro exista.
        Boolean existe = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM public.libros
                    WHERE id = ?
                )
                """,
                Boolean.class,
                libroId
        );

        if (!Boolean.TRUE.equals(existe)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Libro no encontrado"
            );
        }

        // 3. Intentar registrar la identidad de la operación.
        int insertadas = jdbc.update(
                """
                INSERT INTO public.reservas (
                    operacion_id,
                    libro_id,
                    estado
                )
                VALUES (?, ?, 'CONFIRMADA')
                ON CONFLICT (operacion_id) DO NOTHING
                """,
                operacionId,
                libroId
        );

        // Otra solicitud pudo confirmar la misma operación
        // después de nuestra primera consulta.
        if (insertadas == 0) {

            List<ReservaRespuesta> confirmadas =
                    buscar(operacionId);

            if (confirmadas.isEmpty()) {
                throw new IllegalStateException(
                        "No fue posible recuperar la operación existente"
                );
            }

            return comprobarMismoLibro(
                    confirmadas.get(0),
                    libroId
            );
        }

        // 4. Reservar solamente si el libro continúa disponible.
        int modificadas = jdbc.update(
                """
                UPDATE public.libros
                SET disponible = FALSE
                WHERE id = ?
                  AND disponible = TRUE
                """,
                libroId
        );

        if (modificadas == 0) {
            // La excepción revierte también el INSERT anterior.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro ya está ocupado por otra operación"
            );
        }

        // Al finalizar correctamente se confirman
        // el registro de reserva y el cambio del libro.
        return new ReservaRespuesta(
                operacionId,
                libroId,
                "CONFIRMADA"
        );
    }

    @Transactional(readOnly = true)
    public ReservaRespuesta consultar(UUID operacionId) {

        List<ReservaRespuesta> resultados =
                buscar(operacionId);

        if (resultados.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe una reserva confirmada para esa operación"
            );
        }

        return resultados.get(0);
    }

    private List<ReservaRespuesta> buscar(UUID operacionId) {

        return jdbc.query(
                """
                SELECT operacion_id, libro_id, estado
                FROM public.reservas
                WHERE operacion_id = ?
                """,
                (fila, numeroFila) -> new ReservaRespuesta(
                        fila.getObject("operacion_id", UUID.class),
                        fila.getInt("libro_id"),
                        fila.getString("estado")
                ),
                operacionId
        );
    }

    private ReservaRespuesta comprobarMismoLibro(
            ReservaRespuesta reserva,
            int libroId) {

        if (reserva.libroId() != libroId) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El identificador de operación ya pertenece a otro libro"
            );
        }

        return reserva;
    }
}
