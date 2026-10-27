package com.example.prestamos.modelo;

import java.util.UUID;

public record SolicitudPrestamo(
        UUID operacionId,
        Integer libroId,
        String matricula
) {
}

