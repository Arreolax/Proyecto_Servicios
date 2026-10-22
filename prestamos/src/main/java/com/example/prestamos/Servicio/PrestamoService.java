package com.example.prestamos.servicio;

import com.example.prestamos.cliente.BibliotecaClient;
import com.example.prestamos.modelo.LibroRespuesta;
import com.example.prestamos.modelo.Prestamo;
import com.example.prestamos.modelo.SolicitudPrestamo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrestamoService {

    private final BibliotecaClient bibliotecaClient;

    private final Map<Integer, Prestamo> prestamos =
            new LinkedHashMap<>();

    private int siguienteId = 1;

    public PrestamoService(BibliotecaClient bibliotecaClient) {
        this.bibliotecaClient = bibliotecaClient;
    }

    public synchronized Prestamo registrar(
            SolicitudPrestamo solicitud) {

        // 1. Validar los datos recibidos.
        if (solicitud.libroId() == null
                || solicitud.libroId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del libro debe ser mayor que cero"
            );
        }

        if (solicitud.matricula() == null
                || solicitud.matricula().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La matrícula es obligatoria"
            );
        }

        // 2. Consultar al proveedor por HTTP.
        LibroRespuesta libro =
                bibliotecaClient.consultar(solicitud.libroId());

        // 3. Revisar la disponibilidad consultada.
        if (!libro.disponible()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro no está disponible para préstamo"
            );
        }

        // 4. Solicitar la reserva.
        // Biblioteca verifica otra vez la disponibilidad.
        LibroRespuesta reservado =
                bibliotecaClient.reservar(solicitud.libroId());

        // 5. Registrar el préstamo después de confirmar la reserva.
        Prestamo prestamo = new Prestamo(
                siguienteId++,
                reservado.id(),
                reservado.titulo(),
                solicitud.matricula().trim(),
                LocalDate.now(),
                "ACTIVO"
        );

        prestamos.put(prestamo.id(), prestamo);

        return prestamo;
    }

    public synchronized List<Prestamo> obtenerTodos() {
        return new ArrayList<>(prestamos.values());
    }

    public synchronized Prestamo buscarPorId(int id) {
        Prestamo prestamo = prestamos.get(id);

        if (prestamo == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Préstamo no encontrado"
            );
        }

        return prestamo;
    }
}
