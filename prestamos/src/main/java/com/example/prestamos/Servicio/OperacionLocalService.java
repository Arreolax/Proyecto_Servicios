package com.example.prestamos.servicio;

import com.example.prestamos.modelo.LibroRespuesta;
import com.example.prestamos.modelo.OperacionRespuesta;
import com.example.prestamos.modelo.Prestamo;
import com.example.prestamos.modelo.SolicitudPrestamo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperacionLocalService {

    private final JdbcTemplate jdbc;

    private static final String SQL_OPERACION = """
            SELECT
                o.operacion_id,
                o.libro_id,
                o.matricula,
                o.estado,
                o.codigo_error,
                o.mensaje,
                p.id AS prestamo_id
            FROM public.operaciones_prestamo o
            LEFT JOIN public.prestamos p
                ON p.operacion_id = o.operacion_id
            WHERE o.operacion_id = ?
            """;

    private static final RowMapper<OperacionRespuesta> MAPA_OPERACION =
            (fila, numero) -> new OperacionRespuesta(
                    fila.getObject("operacion_id", UUID.class),
                    fila.getInt("libro_id"),
                    fila.getString("matricula"),
                    fila.getString("estado"),
                    fila.getObject("prestamo_id", Integer.class),
                    fila.getObject("codigo_error", Integer.class),
                    fila.getString("mensaje")
            );

    private static final RowMapper<Prestamo> MAPA_PRESTAMO =
            (fila, numero) -> new Prestamo(
                    fila.getInt("id"),
                    fila.getInt("libro_id"),
                    fila.getString("titulo_libro"),
                    fila.getString("matricula"),
                    fila.getObject("fecha_prestamo", LocalDate.class),
                    fila.getString("estado")
            );

    public OperacionLocalService(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperacionRespuesta iniciar(SolicitudPrestamo solicitud) {

        validar(solicitud);

        String matricula = solicitud.matricula().trim();

        jdbc.update(
                """
                INSERT INTO public.operaciones_prestamo (
                    operacion_id,
                    libro_id,
                    matricula,
                    estado
                )
                VALUES (?, ?, ?, 'PENDIENTE')
                ON CONFLICT (operacion_id) DO NOTHING
                """,
                solicitud.operacionId(),
                solicitud.libroId(),
                matricula
        );

        OperacionRespuesta operacion =
                leerOperacion(solicitud.operacionId(), true);

        if (operacion.libroId() != solicitud.libroId()
                || !operacion.matricula().equals(matricula)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El UUID ya pertenece a otra solicitud"
            );
        }

        return operacion;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Prestamo confirmar(
            UUID operacionId,
            LibroRespuesta libro) {

        OperacionRespuesta operacion =
                leerOperacion(operacionId, true);

        if ("CONFIRMADA".equals(operacion.estado())) {
            return leerPrestamo(operacion.prestamoId());
        }

        if ("RECHAZADA".equals(operacion.estado())) {
            throw rechazo(operacion);
        }

        if (libro == null
                || libro.id() != operacion.libroId()
                || libro.titulo() == null
                || libro.titulo().length() > 200) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Los datos del libro no permiten confirmar el préstamo"
            );
        }

        Integer prestamoId = jdbc.queryForObject(
                """
                INSERT INTO public.prestamos (
                    libro_id,
                    titulo_libro,
                    matricula,
                    fecha_prestamo,
                    estado,
                    operacion_id
                )
                VALUES (?, ?, ?, ?, 'ACTIVO', ?)
                RETURNING id
                """,
                Integer.class,
                operacion.libroId(),
                libro.titulo(),
                operacion.matricula(),
                LocalDate.now(),
                operacionId
        );

        jdbc.update(
                """
                UPDATE public.operaciones_prestamo
                SET estado = 'CONFIRMADA',
                    codigo_error = NULL,
                    mensaje = NULL
                WHERE operacion_id = ?
                """,
                operacionId
        );

        return leerPrestamo(prestamoId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperacionRespuesta rechazar(
            UUID operacionId,
            int codigo,
            String mensaje) {

        OperacionRespuesta operacion =
                leerOperacion(operacionId, true);

        // No sobrescribir un resultado terminal.
        if ("PENDIENTE".equals(operacion.estado())) {

            jdbc.update(
                    """
                    UPDATE public.operaciones_prestamo
                    SET estado = 'RECHAZADA',
                        codigo_error = ?,
                        mensaje = ?
                    WHERE operacion_id = ?
                    """,
                    codigo,
                    mensaje,
                    operacionId
            );
        }

        return leerOperacion(operacionId, false);
    }

    @Transactional(readOnly = true)
    public OperacionRespuesta consultar(UUID operacionId) {
        return leerOperacion(operacionId, false);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> obtenerTodos() {

        return jdbc.query(
                """
                SELECT id, libro_id, titulo_libro,
                       matricula, fecha_prestamo, estado
                FROM public.prestamos
                ORDER BY id
                """,
                MAPA_PRESTAMO
        );
    }

    @Transactional(readOnly = true)
    public Prestamo buscarPorId(int id) {
        return leerPrestamo(id);
    }

    private OperacionRespuesta leerOperacion(
            UUID operacionId,
            boolean bloquear) {

        String sql = SQL_OPERACION
                + (bloquear ? " FOR UPDATE OF o" : "");

        List<OperacionRespuesta> filas = jdbc.query(
                sql,
                MAPA_OPERACION,
                operacionId
        );

        if (filas.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Operación no encontrada"
            );
        }

        return filas.get(0);
    }

    private Prestamo leerPrestamo(Integer id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Préstamo no encontrado"
            );
        }

        List<Prestamo> filas = jdbc.query(
                """
                SELECT id, libro_id, titulo_libro,
                       matricula, fecha_prestamo, estado
                FROM public.prestamos
                WHERE id = ?
                """,
                MAPA_PRESTAMO,
                id
        );

        if (filas.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Préstamo no encontrado"
            );
        }

        return filas.get(0);
    }

    private void validar(SolicitudPrestamo solicitud) {

        if (solicitud == null
                || solicitud.operacionId() == null
                || solicitud.libroId() == null
                || solicitud.libroId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Envía operacionId y un libroId positivo"
            );
        }

        if (solicitud.matricula() == null
                || solicitud.matricula().isBlank()
                || solicitud.matricula().trim().length() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La matrícula es obligatoria y admite hasta 50 caracteres"
            );
        }
    }

    private ResponseStatusException rechazo(
            OperacionRespuesta operacion) {

        return new ResponseStatusException(
                HttpStatus.valueOf(operacion.codigoError()),
                operacion.mensaje()
        );
    }
}
