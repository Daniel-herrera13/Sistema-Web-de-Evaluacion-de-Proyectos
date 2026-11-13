package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GrupoDto {
    public record Request(@NotBlank @Size(max = 100) String nombre, @NotNull Long asignacionId) {}
    public record Response(Long id, String nombre, Long asignacionId, Long seccionId,
                           String asignacionNombre, long totalAlumnos) {}
    public record Alumno(Long id, String nombre, String apellido, String correo) {}
    public record AgregarAlumno(@NotNull Long alumnoId) {}
}