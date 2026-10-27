package com.example.prestamos.modelo;

import java.util.UUID;

public record ReservaRespuesta(
        UUID operacionId,
        int libroId,
        String estado
) {
}