package com.example.prestamos.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestamoRepository
        extends JpaRepository<PrestamoEntity, Integer> {
}
