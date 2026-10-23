package com.example.prestamos.persistencia;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prestamos", schema = "public")
public class PrestamoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "libro_id", nullable = false)
    private int libroId;

    @Column(name = "titulo_libro", nullable = false, length = 200)
    private String tituloLibro;

    @Column(nullable = false, length = 50)
    private String matricula;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDate fechaPrestamo;

    @Column(nullable = false, length = 20)
    private String estado;

    protected PrestamoEntity() {
        // Constructor requerido por JPA.
    }

    public PrestamoEntity(
            int libroId,
            String tituloLibro,
            String matricula,
            LocalDate fechaPrestamo,
            String estado) {

        this.libroId = libroId;
        this.tituloLibro = tituloLibro;
        this.matricula = matricula;
        this.fechaPrestamo = fechaPrestamo;
        this.estado = estado;
    }

    public Integer getId() {
        return id;
    }

    public int getLibroId() {
        return libroId;
    }

    public String getTituloLibro() {
        return tituloLibro;
    }

    public String getMatricula() {
        return matricula;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public String getEstado() {
        return estado;
    }
}
