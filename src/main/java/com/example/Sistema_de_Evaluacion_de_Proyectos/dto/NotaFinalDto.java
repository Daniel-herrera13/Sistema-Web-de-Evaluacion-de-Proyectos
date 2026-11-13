package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

public class NotaFinalDto {

    public record NotaAlumno(Long alumnoId, String nombre,
                             BigDecimal notaDocenteJurado, BigDecimal notaAutoevaluacion,
                             BigDecimal notaPublico, BigDecimal notaFinal, boolean definitiva) {}

    public record NotasProyecto(Long proyectoId, String titulo,
                                BigDecimal pesoDocenteJurado, BigDecimal pesoAutoevaluacion,
                                BigDecimal pesoPublico, int evaluacionesPublicas, boolean publicoAbierto,
                                boolean definitiva, List<NotaAlumno> alumnos) {}

    public record MiNota(Long proyectoId, String titulo, boolean publicado, NotaAlumno nota) {}
}