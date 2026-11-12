package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

public class ResultadoDto {

    public record PorEvaluador(Long evaluadorId, String nombre, String rol, String estado,
                               BigDecimal nota, String comentario) {}

    public record PorCriterio(Long criterioId, String nombre, BigDecimal ponderacion, BigDecimal promedio) {}

    public record Resumen(Long proyectoId, String titulo,
                          Long grupoId, String grupo,
                          Long seccionId, String seccion, String ciclo,
                          Long materiaId, String materia,
                          String empresa, String estadoProyecto,
                          int invitados, int enviadas, boolean definitiva,
                          BigDecimal notaFinal, boolean publicado,
                          List<PorEvaluador> evaluadores, List<PorCriterio> criterios) {

        /** Vista del jurado: solo su propia evaluación, sin la nota del grupo. */
        public Resumen paraJurado(Long evaluadorId) {
            return new Resumen(proyectoId, titulo, grupoId, grupo, seccionId, seccion, ciclo,
                    materiaId, materia, empresa, estadoProyecto, invitados, enviadas, definitiva,
                    null, publicado,
                    evaluadores.stream().filter(e -> e.evaluadorId().equals(evaluadorId)).toList(),
                    List.of());
        }
    }

    public record MiResultado(Long proyectoId, String titulo, String grupo, boolean publicado,
                              BigDecimal notaFinal, List<PorCriterio> criterios, List<String> comentarios) {}

    public record Publicacion(Long proyectoId, boolean publicado, BigDecimal notaFinal) {}
}