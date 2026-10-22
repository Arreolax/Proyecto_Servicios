package com.example.biblioteca.controlador;

import com.example.biblioteca.modelo.Libro;
import com.example.biblioteca.servicio.LibroService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException; 

@RestController
@RequestMapping("/api/libros")
public class ReservaController {

    private final LibroService libroService;

    public ReservaController(LibroService libroService) {
        this.libroService = libroService;
    }

    @PostMapping("/{id}/reserva")
    public Libro reservar(@PathVariable("id") int id) {
        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser mayor que cero"
            );
        }

        return libroService.reservar(id);
    }
}
