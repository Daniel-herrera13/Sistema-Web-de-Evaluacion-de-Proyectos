package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SubseccionDto {

    public record Guardar(
            @NotNull(message = "La sección es obligatoria")
            Long seccionId,

            @NotNull(message = "La letra es obligatoria")
            @Pattern(regexp = "^[A-Za-z0-9]{1,5}$",
                    message = "La letra debe tener de 1 a 5 letras o números")
            String letra
    ) {}

    public record Respuesta(Long id, Long seccionId, String letra, String nombre) {}
}