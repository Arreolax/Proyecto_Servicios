package com.example.biblioteca.modelo;

import java.util.UUID;

public record ReservaRespuesta(
        UUID operacionId,
        int libroId,
        String estado
) {
}
