package com.example.biblioteca.servicio;

import com.example.biblioteca.modelo.Libro;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LibroService {

    private final Map<Integer, Libro> libros = new LinkedHashMap<>();

    public LibroService() {
        libros.put(1, new Libro(
                1,
                "El principito",
                "Antoine de Saint-Exupéry",
                true
        ));

        libros.put(2, new Libro(
                2,
                "Don Quijote de la Mancha",
                "Miguel de Cervantes",
                false
        ));

        libros.put(3, new Libro(
                3,
                "Cien años de soledad",
                "Gabriel García Márquez",
                true
        ));
    }

    public synchronized List<Libro> obtenerTodos() {
        return new ArrayList<>(libros.values());
    }

    public synchronized Optional<Libro> buscarPorId(int id) {
        return Optional.ofNullable(libros.get(id));
    }

    public synchronized Libro reservar(int id) {
        Libro libro = libros.get(id);

        if (libro == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Libro no encontrado"
            );
        }

        if (!libro.isDisponible()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El libro ya está prestado o reservado"
            );
        }

        // Creamos una nueva versión del libro como no disponible.
        Libro reservado = new Libro(
                libro.getId(),
                libro.getTitulo(),
                libro.getAutor(),
                false
        );

        libros.put(id, reservado);

        return reservado;
    }
}
