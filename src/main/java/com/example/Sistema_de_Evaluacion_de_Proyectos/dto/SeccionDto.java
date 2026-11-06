package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SeccionDto {
    private SeccionDto() {}

    public record Guardar(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
            @NotNull(message = "La materia es obligatoria") Long materiaId,
            @NotBlank(message = "El ciclo es obligatorio") @Size(max = 20) String ciclo) {}

    public record Respuesta(Long id, String nombre, Long materiaId, String materia, String ciclo) {
        public static Respuesta de(Seccion s) {
            return new Respuesta(s.getId(), s.getNombre(), s.getMateria().getId(),
                    s.getMateria().getNombre(), s.getCiclo());
        }
    }
}