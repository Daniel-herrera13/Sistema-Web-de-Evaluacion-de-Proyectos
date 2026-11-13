package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta de GET /api/reportes/estadisticas.
 * Solo contiene datos agregados: nunca nombres de evaluadores ni notas individuales.
 */
public record EstadisticasReporteDTO(
        ResumenVotos resumen,
        List<AvanceVotos> votosPorProyecto,
        List<AvanceVotos> votosPorGrupo,
        List<RangoNota> distribucion,
        List<PromedioItem> promediosPorProyecto,
        List<PromedioItem> promediosPorGrupo,
        List<PromedioCriterio> promediosPorCriterio,
        Long criteriosProyectoId,
        boolean hayDatos,
        boolean hayCalificaciones
) {

    /** Avance general de evaluaciones enviadas frente a pendientes. */
    public record ResumenVotos(
            long totalProyectos,
            long totalInvitados,
            long pendientes,
            double porcentajeAvance,
            long proyectosDefinitivos,
            long proyectosProvisionales,
            long proyectosSinEvaluaciones
    ) {
    }

    /** Votos de un proyecto o de un grupo. enviadas + pendientes = invitados. */
    public record AvanceVotos(
            Long id,
            String nombre,
            long invitados,
            long enviadas,
            long pendientes,
            boolean definitiva
    ) {
    }

    /** Un rango de la distribución de notas finales (límite inferior incluido; superior excluido salvo en 9-10). */
    public record RangoNota(
            String etiqueta,
            int desde,
            int hasta,
            long definitivos,
            long provisionales
    ) {
    }

    /** Promedio de un proyecto o de un grupo (solo evaluaciones enviadas). */
    public record PromedioItem(
            Long id,
            String nombre,
            BigDecimal promedio,
            boolean provisional,
            long evaluacionesEnviadas,
            long evaluadoresInvitados
    ) {
    }

    /** Promedio de un criterio para el proyecto seleccionado. */
    public record PromedioCriterio(
            Long criterioId,
            String nombre,
            BigDecimal ponderacion,
            BigDecimal promedio,
            boolean provisional
    ) {
    }

    public static EstadisticasReporteDTO vacio() {
        return new EstadisticasReporteDTO(
                new ResumenVotos(0, 0, 0, 0, 0, 0, 0),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                null, false, false);
    }
}
