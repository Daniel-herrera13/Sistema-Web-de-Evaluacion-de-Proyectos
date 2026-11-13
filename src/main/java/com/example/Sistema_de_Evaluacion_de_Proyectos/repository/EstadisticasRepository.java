package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasFiltro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas para las gráficas del reporte.
 *
 * - Usa JPQL dinámico (solo agrega los filtros presentes), así evita el problema de
 *   "parámetro null sin tipo" de PostgreSQL y funciona igual en MySQL y PostgreSQL.
 * - Todas las consultas devuelven escalares/agregados: no se tocan relaciones LAZY
 *   y por eso no hay N+1 (spring.jpa.open-in-view=false no afecta).
 * - Total por request: 3 consultas (4 si hay proyecto seleccionado), sin importar
 *   cuántos proyectos haya.
 */
@Repository
public class EstadisticasRepository {

    /** Valor del enum de Evaluacion.estado que significa "enviada". Ajústalo si tu enum usa otro nombre. */
    public static final String ESTADO_ENVIADA = "ENVIADA";

    /** Cantidad de evaluaciones enviadas y suma de sus calificaciones finales para un proyecto. */
    public record Agregado(long cantidad, BigDecimal suma) {
        public static final Agregado VACIO = new Agregado(0, BigDecimal.ZERO);
    }

    @PersistenceContext
    private EntityManager em;

    /**
     * Proyectos que cumplen los filtros. Columnas: [0] proyectoId, [1] proyectoNombre,
     * [2] grupoId (puede ser null), [3] grupoNombre (puede ser null).
     *
     * @param idsPermitidos null = sin restricción (ADMIN); colección = solo esos proyectos (DOCENTE)
     */
    public List<Object[]> proyectosFiltrados(EstadisticasFiltro f, Collection<Long> idsPermitidos) {
        StringBuilder jpql = new StringBuilder("""
                select p.id, p.titulo, g.id, g.nombre
                from Proyecto p
                left join p.grupo g
                where 1 = 1
                """);
        Map<String, Object> params = new HashMap<>();

        if (idsPermitidos != null) {
            jpql.append(" and p.id in (:idsPermitidos)");
            params.put("idsPermitidos", idsPermitidos);
        }
        if (f.materiaId() != null) {
            jpql.append(" and g.asignacion.materia.id = :materiaId");
            params.put("materiaId", f.materiaId());
        }
        if (f.seccionId() != null) {
            jpql.append(" and g.asignacion.subseccion.seccion.id = :seccionId");
            params.put("seccionId", f.seccionId());
        }
        if (f.grupoId() != null) {
            jpql.append(" and g.id = :grupoId");
            params.put("grupoId", f.grupoId());
        }
        if (f.empresaId() != null) {
            jpql.append(" and p.empresa.id = :empresaId");
            params.put("empresaId", f.empresaId());
        }
        if (f.estado() != null) {
            jpql.append(" and p.estado = :estado");
            params.put("estado", f.estado());
        }
        if (f.ciclo() != null) {
            jpql.append(" and g.asignacion.subseccion.seccion.ciclo = :ciclo");
            params.put("ciclo", f.ciclo());
        }
        if (f.publicado() != null) {
            jpql.append(" and p.resultadosPublicados = :publicado");
            params.put("publicado", f.publicado());
        }
        if (f.proyectoId() != null) {
            jpql.append(" and p.id = :proyectoId");
            params.put("proyectoId", f.proyectoId());
        }
        jpql.append(" order by p.titulo, p.id");

        TypedQuery<Object[]> query = em.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    /** Evaluadores invitados por proyecto: proyectoId -> cantidad. */
    public Map<Long, Long> invitadosPorProyecto(Collection<Long> proyectoIds) {
        List<Object[]> filas = em.createQuery("""
                        select ei.proyecto.id, count(ei.id)
                        from EvaluadorInvitado ei
                        where ei.proyecto.id in (:ids)
                        group by ei.proyecto.id
                        """, Object[].class)
                .setParameter("ids", proyectoIds)
                .getResultList();

        Map<Long, Long> resultado = new HashMap<>();
        for (Object[] fila : filas) {
            resultado.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    /** Evaluaciones enviadas por proyecto: proyectoId -> (cantidad, suma de calificacionFinal). */
    public Map<Long, Agregado> enviadasPorProyecto(Collection<Long> proyectoIds) {
        List<Object[]> filas = em.createQuery("""
                        select e.proyecto.id, count(e.id), coalesce(sum(e.calificacionFinal), 0)
                        from Evaluacion e
                        where e.proyecto.id in (:ids)
                          and e.estado = :enviada
                        group by e.proyecto.id
                        """, Object[].class)
                .setParameter("ids", proyectoIds)
                .setParameter("enviada", ESTADO_ENVIADA)
                .getResultList();

        Map<Long, Agregado> resultado = new HashMap<>();
        for (Object[] fila : filas) {
            resultado.put(
                    ((Number) fila[0]).longValue(),
                    new Agregado(((Number) fila[1]).longValue(), new BigDecimal(fila[2].toString())));
        }
        return resultado;
    }

    /**
     * Promedio por criterio de un proyecto (solo evaluaciones enviadas).
     * Columnas: [0] criterioId, [1] nombre, [2] ponderacion, [3] promedio de notaOtorgada.
     */
    public List<Object[]> promediosPorCriterio(Long proyectoId) {
        return em.createQuery("""
                        select c.id, c.nombre, c.ponderacion, avg(d.notaOtorgada)
                        from EvaluacionDetalle d
                        join d.criterio c
                        join d.evaluacion e
                        where e.proyecto.id = :proyectoId
                          and e.estado = :enviada
                        group by c.id, c.nombre, c.ponderacion
                        order by c.id
                        """, Object[].class)
                .setParameter("proyectoId", proyectoId)
                .setParameter("enviada", ESTADO_ENVIADA)
                .getResultList();
    }
}
