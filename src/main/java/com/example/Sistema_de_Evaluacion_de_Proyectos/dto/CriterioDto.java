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
    /** ciclo vacío o null = conjunto general. */
    public record Request(
            @NotBlank @Size(max = 150) String nombre,
            String descripcion,
            @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal ponderacion,
            Long materiaId, Long asignacionId, Long grupoId, Long proyectoId,
            @Size(max = 20) String ciclo) {}

    public record Item(@NotBlank @Size(max = 150) String nombre,
                       @Size(max = 500) String descripcion,
                       @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal ponderacion) {}

    public record Conjunto(
            Long materiaId, Long asignacionId, Long grupoId, Long proyectoId,
            @Size(max = 20) String ciclo,
            @NotEmpty @Valid List<Item> criterios) {}

    /** Copia el conjunto de un ciclo a otro, en el mismo nivel (materia, asignación, grupo o proyecto). */
    public record Duplicar(
            Long materiaId, Long asignacionId, Long grupoId, Long proyectoId,
            @Size(max = 20) String cicloOrigen,
            @NotBlank @Size(max = 20) String cicloDestino) {}

    public record Response(Long id, String nombre, String descripcion, BigDecimal ponderacion,
                           String nivel, Long nivelId, String ciclo) {}

    /** Criterios que realmente se aplican: origen = nivel de donde salen, ciclo = conjunto elegido (null = general). */
    public record Efectivos(String origen, BigDecimal suma, List<Response> criterios, String ciclo) {}

    /** Un conjunto existente en un nivel: ciclo null = general. */
    public record ConjuntoResumen(String ciclo, int cantidad, BigDecimal suma, boolean completo, boolean enUso) {}
}