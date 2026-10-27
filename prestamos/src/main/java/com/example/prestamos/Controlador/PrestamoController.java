package com.example.prestamos.controlador;

import com.example.prestamos.modelo.OperacionRespuesta;
import com.example.prestamos.modelo.Prestamo;
import com.example.prestamos.modelo.SolicitudPrestamo;
import com.example.prestamos.servicio.PrestamoService;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestamos")
@CrossOrigin(origins = {
    "http://localhost:5500",
    "http://127.0.0.1:5500"
})
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @PostMapping
    public Prestamo registrar(
            @RequestBody SolicitudPrestamo solicitud) {

        return prestamoService.registrar(solicitud);
    }

    @GetMapping
    public List<Prestamo> obtenerTodos() {
        return prestamoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public Prestamo buscarPorId(
            @PathVariable("id") int id) {

        return prestamoService.buscarPorId(id);
    }

    @GetMapping("/operaciones/{operacionId}")
    public OperacionRespuesta consultarOperacion(
            @PathVariable("operacionId") UUID operacionId) {

        return prestamoService.consultarOperacion(operacionId);
    }
}
