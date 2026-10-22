package com.example.integrador.controlador;

import com.example.integrador.modelo.LibroRespuesta;
import com.example.integrador.servicio.BibliotecaSoapClient;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integracion/libros")
@CrossOrigin(origins = {
    "http://localhost:5500",
    "http://127.0.0.1:5500"
})
public class IntegracionController {

    private final BibliotecaSoapClient bibliotecaSoapClient;

    public IntegracionController(
            BibliotecaSoapClient bibliotecaSoapClient) {

        this.bibliotecaSoapClient = bibliotecaSoapClient;
    }

    @GetMapping("/{id}")
    public LibroRespuesta consultar(
            @PathVariable("id") int id) {

        return bibliotecaSoapClient.consultar(id);
    }
}
