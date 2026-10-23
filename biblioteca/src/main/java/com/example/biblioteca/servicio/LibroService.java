package com.example.biblioteca.servicio;

import com.example.biblioteca.modelo.Libro;
import com.example.biblioteca.persistencia.LibroEntity;
import com.example.biblioteca.persistencia.LibroRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibroService {

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional(readOnly = true)
    public List<Libro> obtenerTodos() {

        return libroRepository
                .findAll(Sort.by("id"))
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Libro> buscarPorId(int id) {

        return libroRepository
                .findById(id)
                .map(this::convertir);
    }

    @Transactional
    public Libro reservar(int id) {

        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser mayor que cero"
            );
        }

        int filasModificadas =
                libroRepository.reservarSiDisponible(id);

        if (filasModificadas == 0) {

            if (!libroRepository.existsById(id)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Libro no encontrado"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro ya está prestado o reservado"
            );
        }

        LibroEntity reservado = libroRepository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el libro después de reservarlo"
                ));

        return convertir(reservado);
    }

    private Libro convertir(LibroEntity entidad) {

        return new Libro(
                entidad.getId(),
                entidad.getTitulo(),
                entidad.getAutor(),
                entidad.isDisponible()
        );
    }
}
