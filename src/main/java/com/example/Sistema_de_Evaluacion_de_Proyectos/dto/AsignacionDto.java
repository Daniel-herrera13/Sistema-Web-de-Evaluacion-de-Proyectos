package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

public final class AsignacionDto {
    private AsignacionDto() {}

    public record Response(Long id, String materiaNombre, String seccionNombre,
                           String ciclo, String subseccionLetra, String docenteNombre) {}
}
