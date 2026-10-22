package com.example.integrador.controlador;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejar(
            ResponseStatusException error) {

        String mensaje = error.getReason() != null
                ? error.getReason()
                : "No fue posible completar la consulta";

        return ResponseEntity
                .status(error.getStatusCode())
                .body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarTipoInvalido(
            MethodArgumentTypeMismatchException error) {

        return ResponseEntity.badRequest().body(
                Map.of(
                        "mensaje",
                        "El identificador debe ser un entero válido"
                )
        );
    }
}
