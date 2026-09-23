package com.universidad.tareas.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * Modelo de dominio compartido por la vista Thymeleaf (Parte 1) y la API REST (Parte 2).
 * Las reglas de Bean Validation se declaran una sola vez, aquí, y aplican a ambas capas.
 */
public class Tarea {

    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    @NotNull(message = "La prioridad es obligatoria")
    private Prioridad prioridad;

    // @DateTimeFormat(ISO.DATE): el <input type="date"> del navegador envía y espera "yyyy-MM-dd".
    // Sin esta anotación, Spring MVC usa el formato regional del servidor (p. ej. "dd/MM/yy")
    // y el formulario fallaría al enviar y no prellenaría la fecha al editar.
    // Jackson (API JSON) ignora esta anotación y usa ISO-8601 por defecto en Spring Boot.
    @NotNull(message = "La fecha límite es obligatoria")
    @FutureOrPresent(message = "La fecha límite no puede ser anterior a hoy")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaLimite;

    private boolean completada;

    // Constructor sin argumentos (requerido para el binding de formularios y la deserialización JSON)
    public Tarea() {}

    public Tarea(Long id, String titulo, String descripcion, Prioridad prioridad,
                 LocalDate fechaLimite, boolean completada) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
        this.completada = completada;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate fechaLimite) { this.fechaLimite = fechaLimite; }
    public boolean isCompletada() { return completada; }
    public void setCompletada(boolean completada) { this.completada = completada; }
}
