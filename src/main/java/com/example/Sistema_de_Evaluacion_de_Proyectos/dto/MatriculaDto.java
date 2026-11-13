package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class MatriculaDto {
    private MatriculaDto() {}

    public record Matricular(@NotNull Long subseccionId, @NotEmpty List<Long> alumnoIds) {}

    public record Response(Long id, Long alumnoId, String nombre, String apellido, String correo,
                           Long subseccionId, String subseccionNombre) {}

    public record SubseccionResponse(Long id, String letra, Long seccionId, String nombre) {}
}
