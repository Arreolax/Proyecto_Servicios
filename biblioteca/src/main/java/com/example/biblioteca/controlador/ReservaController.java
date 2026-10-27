package com.example.biblioteca.controlador;

import com.example.biblioteca.modelo.ReservaRespuesta;
import com.example.biblioteca.modelo.SolicitudReserva;
import com.example.biblioteca.servicio.ReservaService;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PutMapping("/{operacionId}")
    public ReservaRespuesta reservar(
            @PathVariable("operacionId") UUID operacionId,
            @RequestBody SolicitudReserva solicitud) {

        ReservaRespuesta respuesta =
                reservaService.reservar(operacionId, solicitud);

        return respuesta;
    }

    @GetMapping("/{operacionId}")
    public ReservaRespuesta consultar(
            @PathVariable("operacionId") UUID operacionId) {

        return reservaService.consultar(operacionId);
    }
}
