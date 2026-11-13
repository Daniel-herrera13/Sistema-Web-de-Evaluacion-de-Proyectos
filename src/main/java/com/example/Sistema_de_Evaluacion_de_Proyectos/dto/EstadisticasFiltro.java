package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

/**
 * Filtros de /api/reportes/estadisticas. Son los mismos que usa /api/reportes/proyectos
 * más un proyectoId opcional (para obtener el promedio por criterio de un proyecto).
 */
public record EstadisticasFiltro(
        Long materiaId,
        Long seccionId,
        Long grupoId,
        Long empresaId,
        String estado,
        String ciclo,
        Boolean publicado,
        Long proyectoId
) {
    public EstadisticasFiltro {
        estado = limpiar(estado);
        ciclo = limpiar(ciclo);
    }

    private static String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
