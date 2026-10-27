package com.example.prestamos.modelo;

import java.util.UUID;

public record OperacionRespuesta(
        UUID operacionId,
        int libroId,
        String matricula,
        String estado,
        Integer prestamoId,
        Integer codigoError,
        String mensaje
) {
}