package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PublicoDto {

    public record Formulario(String proyecto, String grupo, String empresa,
                             List<EsquemaDto.Criterio> criterios) {}

    public record Validar(@NotBlank @Size(min = 4, max = 20) String codigoValidacion) {}

    public record Evaluar(@NotBlank @Size(min = 4, max = 20) String codigoValidacion,
                          @NotEmpty @Valid List<EvaluacionDto.NotaItem> notas) {}
}
