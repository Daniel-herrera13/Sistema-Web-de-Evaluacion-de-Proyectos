package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotNull;

public final class AsignacionDto {
    private AsignacionDto() {}

    public record Response(Long id, Long materiaId, String materiaNombre, String seccionNombre,
                           String ciclo, String subseccionLetra, Long docenteId, String docenteNombre) {}

    /** subseccionId vacío (null) = aplicar la materia a todas las subsecciones de la sección. */
    public record Guardar(
            @NotNull(message = "La sección es obligatoria") Long seccionId,
            Long subseccionId,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotNull(message = "El docente es obligatorio") Long docenteId) {}

    public record Detalle(Long id, Long subseccionId, String subseccion,
                          Long materiaId, String materiaNombre,
                          Long docenteId, String docenteNombre) {}

    public record Resultado(int creadas, int omitidas) {}
}
