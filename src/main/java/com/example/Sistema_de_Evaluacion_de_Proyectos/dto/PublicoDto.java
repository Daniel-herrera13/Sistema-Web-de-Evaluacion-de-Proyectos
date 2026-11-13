package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PublicoDto {

    public record Formulario(String proyecto, String grupo, String empresa,
                             List<EsquemaDto.Criterio> criterios) {}

    public record Evaluar(@NotEmpty @Valid List<EvaluacionDto.NotaItem> notas) {}
}