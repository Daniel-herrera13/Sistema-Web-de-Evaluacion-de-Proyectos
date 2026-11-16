package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SeccionDto {
    private SeccionDto() {}

    public record Guardar(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 50) String nombre,
            @NotBlank(message = "El ciclo es obligatorio") @Size(max = 20) String ciclo) {}

    public record Respuesta(Long id, String nombre, String ciclo, List<String> materias) {
        /** Versión sin materias, por compatibilidad con código que ya la use. */
        public static Respuesta de(Seccion s) {
            return new Respuesta(s.getId(), s.getNombre(), s.getCiclo(), List.of());
        }
    }
}