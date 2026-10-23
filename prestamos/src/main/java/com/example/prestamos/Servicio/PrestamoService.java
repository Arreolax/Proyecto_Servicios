package com.example.prestamos.servicio;

import com.example.prestamos.cliente.BibliotecaClient;

import com.example.prestamos.modelo.LibroRespuesta;
import com.example.prestamos.modelo.Prestamo;
import com.example.prestamos.modelo.SolicitudPrestamo;

import com.example.prestamos.persistencia.PrestamoEntity;
import com.example.prestamos.persistencia.PrestamoRepository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrestamoService {

    private final BibliotecaClient bibliotecaClient;
    private final PrestamoRepository prestamoRepository;

    public PrestamoService(
            BibliotecaClient bibliotecaClient,
            PrestamoRepository prestamoRepository) {

        this.bibliotecaClient = bibliotecaClient;
        this.prestamoRepository = prestamoRepository;
    }

    @Transactional
    public Prestamo registrar(SolicitudPrestamo solicitud) {

        // 1. Validar la solicitud antes de reservar.
        if (solicitud == null
                || solicitud.libroId() == null
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

        String matricula = solicitud.matricula().trim();

        if (matricula.length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La matrícula no debe superar 50 caracteres"
            );
        }

        // 2. Consultar el libro mediante la API de biblioteca.
        LibroRespuesta libro =
                bibliotecaClient.consultar(solicitud.libroId());

        if (!libro.disponible()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro no está disponible para préstamo"
            );
        }

        // 3. Solicitar la reserva.
        // Biblioteca toma la decisión definitiva.
        LibroRespuesta reservado =
                bibliotecaClient.reservar(solicitud.libroId());

        // 4. Preparar el registro persistente.
        PrestamoEntity entidad = new PrestamoEntity(
                reservado.id(),
                reservado.titulo(),
                matricula,
                LocalDate.now(),
                "ACTIVO"
        );

        // 5. Guardar el préstamo en su propia base de datos.
        PrestamoEntity guardado =
                prestamoRepository.saveAndFlush(entidad);

        // 6. Preparar la respuesta.
        return convertir(guardado);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> obtenerTodos() {

        return prestamoRepository
                .findAll(Sort.by("id"))
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public Prestamo buscarPorId(int id) {

        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del préstamo debe ser mayor que cero"
            );
        }

        return prestamoRepository.findById(id)
                .map(this::convertir)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Préstamo no encontrado"
                ));
    }

    private Prestamo convertir(PrestamoEntity entidad) {

        return new Prestamo(
                entidad.getId(),
                entidad.getLibroId(),
                entidad.getTituloLibro(),
                entidad.getMatricula(),
                entidad.getFechaPrestamo(),
                entidad.getEstado()
        );
    }
}
