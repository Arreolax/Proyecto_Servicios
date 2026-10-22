package com.example.biblioteca.controlador;

import com.example.biblioteca.modelo.Libro;
import com.example.biblioteca.servicio.LibroService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/libros")
@CrossOrigin(origins = {
    "http://localhost:5500",
    "http://127.0.0.1:5500"
})
public class LibroController {

    private final LibroService libroService;

    // Spring proporciona la instancia de LibroService.
    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    // GET /api/libros
    @GetMapping
    public List<Libro> obtenerTodos() {
        return libroService.obtenerTodos();
    }

    // GET /api/libros/1
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @PathVariable("id") String idTexto) {

        int id;

        try {
            id = Integer.parseInt(idTexto);
        } catch (NumberFormatException error) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "mensaje",
                    "El identificador debe ser un número entero positivo"
                )
            );
        }

        if (id <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "mensaje",
                    "El identificador debe ser mayor que cero"
                )
            );
        }

        Optional<Libro> resultado = libroService.buscarPorId(id);

        if (resultado.isPresent()) {
            return ResponseEntity.ok(resultado.get());
        }

        return ResponseEntity.status(404).body(
            Map.of("mensaje", "Libro no encontrado")
        );
    }
}
