package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class EvaluacionDto {

    public record NotaItem(
            @NotNull Long criterioId,
            @NotNull
            @DecimalMin(value = "0.0", message = "La nota mínima es 0")
            @DecimalMax(value = "10.0", message = "La nota máxima es 10")
            @Digits(integer = 2, fraction = 2, message = "La nota admite hasta 2 decimales")
            BigDecimal nota) {}

    public record Guardar(@Size(max = 5000) String comentarios, @Valid List<NotaItem> notas) {}

    public record Detalle(Long criterioId, String criterio, BigDecimal ponderacion, BigDecimal nota) {}

    public record Response(Long id, Long proyectoId, String proyectoTitulo,
                           Long evaluadorId, String evaluador,
                           String estado, BigDecimal calificacionFinal,
                           String comentarios, LocalDateTime fecha, List<Detalle> detalles) {}
}