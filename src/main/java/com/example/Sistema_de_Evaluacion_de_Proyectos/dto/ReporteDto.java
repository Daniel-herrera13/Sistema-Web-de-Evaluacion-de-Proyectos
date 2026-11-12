package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReporteDto {

    public record Filtros(Long materiaId, Long seccionId, Long grupoId, Long empresaId,
                          String estado, String ciclo, Boolean publicado) {}

    public record Fila(Long proyectoId, String titulo, String grupo, String seccion, String ciclo,
                       String materia, String empresa, String estado,
                       int invitados, int enviadas, BigDecimal notaFinal,
                       boolean definitiva, boolean publicado) {}

    public record Tabla(String titulo, List<String> cabeceras, List<List<Object>> filas) {}
}