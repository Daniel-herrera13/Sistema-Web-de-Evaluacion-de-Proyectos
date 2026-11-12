package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class InvitacionDto {
    public record Crear(@NotNull Long proyectoId, @NotNull Long evaluadorId) {}

    public record Response(Long id, Long proyectoId, String proyectoTitulo,
                           Long evaluadorId, String nombre, String apellido, String correo,
                           String rol, String invitadoPor, LocalDateTime fechaInvitacion) {}

    public record Asignado(Long proyectoId, String titulo, String grupo, String empresa,
                           String estadoProyecto, String estadoEvaluacion, Long evaluacionId) {}
}