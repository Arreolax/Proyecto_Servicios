package com.example.biblioteca.modelo;

public class Libro {

    private final int id;
    private final String titulo;
    private final String autor;
    private final boolean disponible;

    public Libro(int id, String titulo, String autor, boolean disponible) {
        this.id = id;
        this.titulo = titulo;
        this.autor = autor;
        this.disponible = disponible;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public boolean isDisponible() {
        return disponible;
    }
}
