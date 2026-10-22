package com.example.prestamos.modelo;

import java.time.LocalDate;

public record Prestamo(
        int id,
        int libroId,
        String tituloLibro,
        String matricula,
        LocalDate fechaPrestamo,
        String estado
) {
}
