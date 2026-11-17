package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class EsquemaDto {

    public record Item(@NotBlank @Size(max = 150) String nombre,
                       @Size(max = 500) String descripcion,
                       @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal ponderacion) {}

    public record Guardar(@NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pesoDocenteJurado,
                          @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pesoAutoevaluacion,
                          @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pesoPublico,
                          @Valid List<Item> criteriosAutoevaluacion,
                          @Valid List<Item> criteriosPublico) {}

    public record Criterio(Long id, String nombre, String descripcion, BigDecimal ponderacion) {}

    /**
     * ciclo: el ciclo consultado (null = general).
     * heredado: ese ciclo no tiene esquema propio y se muestra el general.
     * enUso: ya hay evaluaciones que dependen de este esquema, así que no se puede cambiar.
     */
    public record Response(Long materiaId, BigDecimal pesoDocenteJurado, BigDecimal pesoAutoevaluacion,
                           BigDecimal pesoPublico, List<Criterio> criteriosAutoevaluacion,
                           List<Criterio> criteriosPublico, String ciclo, boolean heredado, boolean enUso) {}

    public record PublicoEstado(String codigo, String codigoValidacion, boolean abierto, long evaluaciones) {}
    public record CodigoValidacion(@NotBlank @Size(min = 4, max = 20)
                               @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Usa solo letras, números y guiones")
                               String codigo) {}
    public record ProyectoPublico(Long id, String titulo, String grupoNombre) {}
    public record MateriaOpcion(Long id, String nombre) {}
}
