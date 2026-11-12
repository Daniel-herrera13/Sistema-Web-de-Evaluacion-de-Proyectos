package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProyectoDto {
    public record Request(@NotBlank @Size(max = 200) String titulo,
                          @Size(max = 2000) String descripcion,
                          @NotNull Long grupoId,
                          Long empresaId,
                          @Pattern(regexp = "ACTIVO|FINALIZADO|CANCELADO",
                                  message = "Estado válido: ACTIVO, FINALIZADO o CANCELADO") String estado) {}

    public record Response(Long id, String titulo, String descripcion,
                           Long grupoId, String grupoNombre, Long seccionId, Long materiaId,
                           Long empresaId, String empresaNombre, String estado,
                           Boolean resultadosPublicados) {}
}