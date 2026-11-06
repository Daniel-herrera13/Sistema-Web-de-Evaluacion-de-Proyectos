package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MateriaDto {
    private MateriaDto() {}

    public record Guardar(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            String descripcion,
            @NotNull(message = "El docente principal es obligatorio") Long docentePrincipalId) {}

    public record Respuesta(Long id, String nombre, String descripcion,
                            Long docentePrincipalId, String docentePrincipal) {
        public static Respuesta de(Materia m) {
            return new Respuesta(m.getId(), m.getNombre(), m.getDescripcion(),
                    m.getDocentePrincipal().getId(),
                    m.getDocentePrincipal().getNombre() + " " + m.getDocentePrincipal().getApellido());
        }
    }
}