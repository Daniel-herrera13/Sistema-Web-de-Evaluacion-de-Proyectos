package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasFiltro;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO.AvanceVotos;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO.PromedioCriterio;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO.PromedioItem;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO.RangoNota;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO.ResumenVotos;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EstadisticasRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EstadisticasRepository.Agregado;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estadísticas para las gráficas del reporte (votos y calificaciones).
 *
 * Reglas:
 * - Solo ADMIN y DOCENTE. Cualquier otro rol recibe AccessDeniedException (403).
 * - ADMIN ve todo; DOCENTE solo los proyectos que AccesoService le permite ver.
 * - Solo se promedian evaluaciones enviadas.
 * - Una nota es definitiva solo cuando todos los evaluadores invitados enviaron; si no, es provisional.
 * - Solo se devuelven agregados: nunca nombres de evaluadores ni notas individuales.
 */
@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private static final String[] ETIQUETAS_RANGO = {"0–5", "5–6", "6–7", "7–8", "8–9", "9–10"};

    private final EstadisticasRepository repository;
    private final AccesoService accesoService;

    @Transactional(readOnly = true)
    public EstadisticasReporteDTO obtener(EstadisticasFiltro filtro, Authentication auth) {
        boolean admin = tieneAutoridad(auth, "ROLE_ADMIN");
        boolean docente = tieneAutoridad(auth, "ROLE_DOCENTE");
        if (!admin && !docente) {
            throw new AccessDeniedException("Solo ADMIN y DOCENTE pueden consultar las estadísticas del reporte");
        }

        // Alcance por rol. null = sin restricción (ADMIN).
        Collection<Long> permitidos = null;
        if (!admin) {
            // Único punto de integración con AccesoService (ver notas de entrega).
            permitidos = accesoService.proyectoIdsVisibles(auth);
            if (permitidos == null || permitidos.isEmpty()) {
                return EstadisticasReporteDTO.vacio();
            }
        }

        // Consulta 1: proyectos filtrados (con su grupo).
        List<Object[]> base = repository.proyectosFiltrados(filtro, permitidos);
        if (base.isEmpty()) {
            return EstadisticasReporteDTO.vacio();
        }
        List<Long> ids = base.stream().map(r -> ((Number) r[0]).longValue()).toList();

        // Consultas 2 y 3: invitados y evaluaciones enviadas, agrupadas por proyecto.
        Map<Long, Long> invitados = repository.invitadosPorProyecto(ids);
        Map<Long, Agregado> enviadas = repository.enviadasPorProyecto(ids);

        List<Fila> filas = new ArrayList<>(base.size());
        for (Object[] r : base) {
            long id = ((Number) r[0]).longValue();
            Agregado ag = enviadas.getOrDefault(id, Agregado.VACIO);
            filas.add(new Fila(
                    id,
                    (String) r[1],
                    r[2] == null ? null : ((Number) r[2]).longValue(),
                    (String) r[3],
                    invitados.getOrDefault(id, 0L),
                    ag.cantidad(),
                    ag.suma()));
        }

        return new EstadisticasReporteDTO(
                resumen(filas),
                votosPorProyecto(filas),
                votosPorGrupo(filas),
                distribucion(filas),
                promediosPorProyecto(filas),
                promediosPorGrupo(filas),
                criterios(filtro.proyectoId(), filas),
                filtro.proyectoId(),
                true,
                filas.stream().anyMatch(f -> f.enviadas() > 0));
    }

    // ---------------------------------------------------------------- votos

    private ResumenVotos resumen(List<Fila> filas) {
        long totalInvitados = filas.stream().mapToLong(Fila::invitados).sum();
        long pendientes = filas.stream().mapToLong(Fila::pendientes).sum();
        double avance = totalInvitados == 0
                ? 0
                : redondear1((totalInvitados - pendientes) * 100.0 / totalInvitados);

        long definitivos = filas.stream().filter(Fila::definitiva).count();
        long provisionales = filas.stream().filter(f -> f.enviadas() > 0 && !f.definitiva()).count();
        long sinEvaluaciones = filas.stream().filter(f -> f.enviadas() == 0).count();

        return new ResumenVotos(filas.size(), totalInvitados, pendientes, avance,
                definitivos, provisionales, sinEvaluaciones);
    }

    private List<AvanceVotos> votosPorProyecto(List<Fila> filas) {
        return filas.stream()
                .map(f -> new AvanceVotos(f.id(), f.nombre(), f.invitados(),
                        f.enviadasContadas(), f.pendientes(), f.definitiva()))
                .toList();
    }

    private List<AvanceVotos> votosPorGrupo(List<Fila> filas) {
        List<AvanceVotos> resultado = new ArrayList<>();
        for (Map.Entry<Long, Acumulado> e : agruparPorGrupo(filas).entrySet()) {
            Acumulado a = e.getValue();
            resultado.add(new AvanceVotos(e.getKey(), a.nombre, a.invitados, a.enviadasContadas,
                    a.invitados - a.enviadasContadas, a.invitados > 0 && a.definitiva));
        }
        return resultado;
    }

    // --------------------------------------------------------- calificaciones

    /** Distribución de las notas finales (promedio de enviadas) de los proyectos, en 6 rangos. */
    private List<RangoNota> distribucion(List<Fila> filas) {
        long[] definitivos = new long[ETIQUETAS_RANGO.length];
        long[] provisionales = new long[ETIQUETAS_RANGO.length];

        for (Fila f : filas) {
            BigDecimal promedio = f.promedio();
            if (promedio == null) {
                continue; // sin evaluaciones enviadas: no entra en la distribución
            }
            int indice = indiceRango(promedio);
            if (f.definitiva()) {
                definitivos[indice]++;
            } else {
                provisionales[indice]++;
            }
        }

        List<RangoNota> rangos = new ArrayList<>();
        for (int i = 0; i < ETIQUETAS_RANGO.length; i++) {
            int desde = i == 0 ? 0 : i + 4;
            int hasta = i == 0 ? 5 : i + 5;
            rangos.add(new RangoNota(ETIQUETAS_RANGO[i], desde, hasta, definitivos[i], provisionales[i]));
        }
        return rangos;
    }

    /** 0-5 -> 0; 5-6 -> 1; ...; 9-10 (incluye 10) -> 5. */
    private static int indiceRango(BigDecimal nota) {
        if (nota.compareTo(BigDecimal.valueOf(5)) < 0) {
            return 0;
        }
        return Math.min(nota.intValue() - 4, ETIQUETAS_RANGO.length - 1);
    }

    private List<PromedioItem> promediosPorProyecto(List<Fila> filas) {
        return filas.stream()
                .filter(f -> f.enviadas() > 0)
                .map(f -> new PromedioItem(f.id(), f.nombre(), f.promedio(), !f.definitiva(),
                        f.enviadas(), f.invitados()))
                .sorted(PROMEDIO_DESC)
                .toList();
    }

    private List<PromedioItem> promediosPorGrupo(List<Fila> filas) {
        List<PromedioItem> resultado = new ArrayList<>();
        for (Map.Entry<Long, Acumulado> e : agruparPorGrupo(filas).entrySet()) {
            Acumulado a = e.getValue();
            if (a.enviadas == 0) {
                continue;
            }
            resultado.add(new PromedioItem(e.getKey(), a.nombre, media(a.suma, a.enviadas),
                    !(a.invitados > 0 && a.definitiva), a.enviadas, a.invitados));
        }
        resultado.sort(PROMEDIO_DESC);
        return resultado;
    }

    /** Promedio por criterio: solo si hay un proyecto seleccionado que pasó los filtros y el alcance del rol. */
    private List<PromedioCriterio> criterios(Long proyectoId, List<Fila> filas) {
        if (proyectoId == null) {
            return List.of();
        }
        Fila fila = filas.stream().filter(f -> f.id() == proyectoId).findFirst().orElse(null);
        if (fila == null || fila.enviadas() == 0) {
            return List.of();
        }

        // Consulta 4 (solo con proyecto seleccionado).
        List<PromedioCriterio> resultado = new ArrayList<>();
        for (Object[] r : repository.promediosPorCriterio(proyectoId)) {
            BigDecimal promedio = BigDecimal.valueOf(((Number) r[3]).doubleValue())
                    .setScale(2, RoundingMode.HALF_UP);
            resultado.add(new PromedioCriterio(
                    ((Number) r[0]).longValue(),
                    (String) r[1],
                    new BigDecimal(r[2].toString()),
                    promedio,
                    !fila.definitiva()));
        }
        return resultado;
    }

    // ---------------------------------------------------------------- auxiliares

    private static final Comparator<PromedioItem> PROMEDIO_DESC = Comparator
            .comparing(PromedioItem::promedio, Comparator.reverseOrder())
            .thenComparing(PromedioItem::nombre, Comparator.nullsLast(String::compareToIgnoreCase));

    private Map<Long, Acumulado> agruparPorGrupo(List<Fila> filas) {
        Map<Long, Acumulado> grupos = new LinkedHashMap<>();
        for (Fila f : filas) {
            if (f.grupoId() == null) {
                continue;
            }
            Acumulado a = grupos.computeIfAbsent(f.grupoId(), k -> new Acumulado(f.grupoNombre()));
            a.invitados += f.invitados();
            a.enviadas += f.enviadas();
            a.enviadasContadas += f.enviadasContadas();
            a.suma = a.suma.add(f.suma());
            a.definitiva &= f.definitiva();
        }
        return grupos;
    }

    private static boolean tieneAutoridad(Authentication auth, String autoridad) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> autoridad.equals(a.getAuthority()));
    }

    private static BigDecimal media(BigDecimal suma, long cantidad) {
        return suma.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private static double redondear1(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    /** Datos de un proyecto ya combinados (invitados + evaluaciones enviadas). */
    private record Fila(long id, String nombre, Long grupoId, String grupoNombre,
                        long invitados, long enviadas, BigDecimal suma) {

        long enviadasContadas() {
            return Math.min(enviadas, invitados);
        }

        long pendientes() {
            return invitados - enviadasContadas();
        }

        /** Definitiva solo cuando todos los invitados enviaron (y hay al menos uno). */
        boolean definitiva() {
            return invitados > 0 && enviadas >= invitados;
        }

        BigDecimal promedio() {
            return enviadas == 0 ? null : media(suma, enviadas);
        }
    }

    /** Acumulador por grupo. */
    private static final class Acumulado {
        final String nombre;
        long invitados;
        long enviadas;
        long enviadasContadas;
        BigDecimal suma = BigDecimal.ZERO;
        boolean definitiva = true;

        Acumulado(String nombre) {
            this.nombre = nombre;
        }
    }
}
