package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.NotaFinalDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EsquemaEvaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumnoDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionPublicaDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionAlumnoDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionPublicaDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotaFinalService {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ProyectoRepository proyectos;
    private final GrupoAlumnoRepository miembros;
    private final EvaluacionAlumnoRepository evalAlumno;
    private final EvaluacionAlumnoDetalleRepository detAlumno;
    private final EvaluacionPublicaDetalleRepository detPublica;
    private final CalculoNotaService calculo;
    private final EsquemaService esquema;
    private final AccesoService acceso;

    /** Para admin y docente: desglose por alumno de un proyecto. */
    @Transactional(readOnly = true)
    public NotaFinalDto.NotasProyecto deProyecto(Long proyectoId, String correo) {
        Proyecto p = proyectos.findById(proyectoId)
                .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
        acceso.exigirAcceso(acceso.actor(correo), p);
        return notasProyecto(proyectoId);
    }

    /** Para el alumno: su propia nota, solo si los resultados están publicados. */
    @Transactional(readOnly = true)
    public List<NotaFinalDto.MiNota> miNota(String correo) {
        Usuario u = acceso.actor(correo);
        List<NotaFinalDto.MiNota> salida = new ArrayList<>();
        for (GrupoAlumno m : miembros.findByAlumnoId(u.getId())) {
            for (Proyecto p : proyectos.findByGrupoId(m.getGrupo().getId())) {
                if (!Boolean.TRUE.equals(p.getResultadosPublicados())) {
                    salida.add(new NotaFinalDto.MiNota(p.getId(), p.getTitulo(),
                            p.getGrupo().getAsignacion().getMateria().getNombre(), false, null));
                    continue;
                }
                NotaFinalDto.NotaAlumno mia = notasProyecto(p.getId()).alumnos().stream()
                        .filter(a -> a.alumnoId().equals(u.getId())).findFirst().orElse(null);
                salida.add(new NotaFinalDto.MiNota(p.getId(), p.getTitulo(),
                        p.getGrupo().getAsignacion().getMateria().getNombre(), true, mia));
            }
        }
        return salida;
    }

    @Transactional(readOnly = true)
    public NotaFinalDto.NotasProyecto notasProyecto(Long proyectoId) {
        Proyecto p = proyectos.findById(proyectoId)
                .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
        EsquemaEvaluacion esq = esquema.deProyecto(p);
        boolean usaDoc = esq.getPesoDocenteJurado().signum() > 0;
        boolean usaAuto = esq.getPesoAutoevaluacion().signum() > 0;
        boolean usaPub = esq.getPesoPublico().signum() > 0;

        // 1. Docente / jurado (nota del grupo)
        ResultadoDto.Resumen r = calculo.resumen(proyectoId);
        BigDecimal notaDoc = r.notaFinal();

        // 2. Público (nota del grupo)
        BigDecimal notaPub = null;
        int nPub = 0;
        if (usaPub) {
            Map<Long, BigDecimal> porEval = new HashMap<>();
            for (EvaluacionPublicaDetalle d : detPublica.findByEvaluacion_ProyectoId(proyectoId))
                porEval.merge(d.getEvaluacion().getId(),
                        ponderada(d.getNota(), d.getCriterio().getPonderacion()), BigDecimal::add);
            nPub = porEval.size();
            if (nPub > 0) notaPub = promedio(porEval.values());
        }
        boolean abierto = Boolean.TRUE.equals(p.getPublicoAbierto());

        // 3. Autoevaluación (nota individual: promedio de lo recibido)
        List<GrupoAlumno> grupo = miembros.findByGrupoId(p.getGrupo().getId());
        Map<Long, List<BigDecimal>> recibidas = new HashMap<>();
        boolean autoCompleta = true;
        if (usaAuto) {
            Map<Long, EvaluacionAlumno> evals = evalAlumno.findByProyectoId(proyectoId).stream()
                    .collect(Collectors.toMap(EvaluacionAlumno::getId, e -> e));
            Map<Long, BigDecimal> porEval = new HashMap<>();
            for (EvaluacionAlumnoDetalle d : detAlumno.findByEvaluacion_ProyectoId(proyectoId))
                porEval.merge(d.getEvaluacion().getId(),
                        ponderada(d.getNota(), d.getCriterio().getPonderacion()), BigDecimal::add);

            Map<Long, Integer> hechas = new HashMap<>();   // evaluadorId -> cuántas evaluaciones hizo
            for (Map.Entry<Long, BigDecimal> en : porEval.entrySet()) {
                EvaluacionAlumno e = evals.get(en.getKey());
                if (e.getEvaluador().getId().equals(e.getEvaluado().getId())) continue;
                recibidas.computeIfAbsent(e.getEvaluado().getId(), k -> new ArrayList<>()).add(en.getValue());
                hechas.merge(e.getEvaluador().getId(), 1, Integer::sum);
            }
            int evaluacionesEsperadas = Math.max(0, grupo.size() - 1);
            for (GrupoAlumno m : grupo)
                if (hechas.getOrDefault(m.getAlumno().getId(), 0) < evaluacionesEsperadas) autoCompleta = false;
        }

        boolean docOk = !usaDoc || r.definitiva();
        boolean autoOk = !usaAuto || (autoCompleta && !grupo.isEmpty());
        boolean pubOk = !usaPub || (nPub > 0 && !abierto);
        boolean todoOk = docOk && autoOk && pubOk;

        List<NotaFinalDto.NotaAlumno> alumnos = new ArrayList<>();
        for (GrupoAlumno m : grupo) {
            Usuario a = m.getAlumno();
            List<BigDecimal> rec = recibidas.get(a.getId());
            BigDecimal notaAuto = (usaAuto && rec != null && !rec.isEmpty()) ? promedio(rec) : null;

            BigDecimal total = BigDecimal.ZERO;
            boolean completo = true;
            if (usaDoc) {
                if (notaDoc == null) completo = false;
                else total = total.add(ponderada(notaDoc, esq.getPesoDocenteJurado()));
            }
            if (usaAuto) {
                if (notaAuto == null) completo = false;
                else total = total.add(ponderada(notaAuto, esq.getPesoAutoevaluacion()));
            }
            if (usaPub) {
                if (notaPub == null) completo = false;
                else total = total.add(ponderada(notaPub, esq.getPesoPublico()));
            }
            BigDecimal fin = completo ? total.setScale(2, RoundingMode.HALF_UP) : null;
            alumnos.add(new NotaFinalDto.NotaAlumno(a.getId(), a.getNombre() + " " + a.getApellido(),
                    usaDoc ? notaDoc : null, notaAuto, usaPub ? notaPub : null, fin, completo && todoOk));
        }

        return new NotaFinalDto.NotasProyecto(proyectoId, p.getTitulo(), esq.getPesoDocenteJurado(),
                esq.getPesoAutoevaluacion(), esq.getPesoPublico(), nPub, abierto, todoOk, alumnos);
    }

    private BigDecimal ponderada(BigDecimal nota, BigDecimal peso) {
        return nota.multiply(peso).divide(CIEN, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal promedio(Collection<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }
}
