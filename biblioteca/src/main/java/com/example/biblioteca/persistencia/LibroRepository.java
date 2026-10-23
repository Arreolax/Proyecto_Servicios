package com.example.biblioteca.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibroRepository
        extends JpaRepository<LibroEntity, Integer> {

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            update LibroEntity l
            set l.disponible = false
            where l.id = :id
              and l.disponible = true
            """)
    int reservarSiDisponible(@Param("id") int id);
}
