package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class CriterioDto {
    public record Request(
            @NotBlank @Size(max = 150) String nombre,
            String descripcion,
            @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal ponderacion,
            Long materiaId, Long asignacionId, Long grupoId, Long proyectoId) {}

    public record Item(@NotBlank @Size(max = 150) String nombre,
                       @Size(max = 500) String descripcion,
                       @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal ponderacion) {}

    public record Conjunto(
            Long materiaId, Long asignacionId, Long grupoId, Long proyectoId,
            @NotEmpty List<Item> criterios) {}

    public record Response(Long id, String nombre, String descripcion, BigDecimal ponderacion,
                           String nivel, Long nivelId) {}

    public record Efectivos(String origen, BigDecimal suma, List<Response> criterios) {}
}