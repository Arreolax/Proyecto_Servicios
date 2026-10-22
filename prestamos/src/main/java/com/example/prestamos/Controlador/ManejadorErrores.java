package com.example.prestamos.controlador;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ResponseStatusException error) {

        String mensaje = error.getReason() != null
                ? error.getReason()
                : "No fue posible realizar la operación";

        return ResponseEntity
                .status(error.getStatusCode())
                .body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(
            HttpMessageNotReadableException error) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "mensaje",
                        "Envía un JSON válido con libroId y matricula"
                ));
    }
}
