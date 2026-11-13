package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class AutoevaluacionDto {

    public record Guardar(@NotNull Long evaluadoId,
                          @NotEmpty @Valid List<EvaluacionDto.NotaItem> notas) {}

    public record NotaGuardada(Long criterioId, BigDecimal nota) {}

    public record Objetivo(Long alumnoId, String nombre, boolean propio, boolean evaluado,
                           List<NotaGuardada> notas) {}

    public record Formulario(Long proyectoId, String proyecto, List<EsquemaDto.Criterio> criterios,
                             List<Objetivo> objetivos, boolean completo) {}
}