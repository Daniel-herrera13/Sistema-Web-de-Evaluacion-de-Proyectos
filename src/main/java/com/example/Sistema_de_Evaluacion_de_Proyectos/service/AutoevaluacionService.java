package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.AutoevaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EvaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema.Modalidad;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumnoDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionAlumnoDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutoevaluacionService {
    private final ProyectoRepository proyectos;
    private final GrupoAlumnoRepository miembros;
    private final EvaluacionAlumnoRepository evaluaciones;
    private final EvaluacionAlumnoDetalleRepository detalles;
    private final EsquemaService esquema;
    private final AccesoService acceso;

    @Transactional(readOnly = true)
    public AutoevaluacionDto.Formulario formulario(Long proyectoId, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        exigirMiembro(u, p);
        return armar(p, u, criteriosActivos(p));
    }

    @Transactional
    public AutoevaluacionDto.Formulario guardar(Long proyectoId, AutoevaluacionDto.Guardar r, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        exigirMiembro(u, p);
        if (Boolean.TRUE.equals(p.getResultadosPublicados()))
            throw Http.conflicto("Los resultados ya fueron publicados; no se puede modificar la evaluación");

        List<CriterioEsquema> crit = criteriosActivos(p);
        GrupoAlumno objetivo = miembros.findByGrupoIdAndAlumnoId(p.getGrupo().getId(), r.evaluadoId())
                .orElseThrow(() -> Http.invalido("El evaluado no pertenece a tu grupo"));

        Map<Long, CriterioEsquema> porId = crit.stream()
                .collect(Collectors.toMap(CriterioEsquema::getId, c -> c));
        Set<Long> enviados = new HashSet<>();
        for (EvaluacionDto.NotaItem n : r.notas()) {
            if (!porId.containsKey(n.criterioId()))
                throw Http.invalido("El criterio " + n.criterioId() + " no pertenece a la autoevaluación");
            Notas.validar(n.nota());
            if (!enviados.add(n.criterioId()))
                throw Http.invalido("El criterio " + n.criterioId() + " está repetido");
        }
        if (enviados.size() != crit.size())
            throw Http.invalido("Debes calificar todos los criterios");

        EvaluacionAlumno e = evaluaciones
                .findByProyectoIdAndEvaluadorIdAndEvaluadoId(proyectoId, u.getId(), r.evaluadoId())
                .orElseGet(() -> {
                    EvaluacionAlumno nueva = new EvaluacionAlumno();
                    nueva.setProyecto(p);
                    nueva.setEvaluador(u);
                    nueva.setEvaluado(objetivo.getAlumno());
                    return evaluaciones.save(nueva);
                });

        Map<Long, EvaluacionAlumnoDetalle> actuales = new HashMap<>();
        for (EvaluacionAlumnoDetalle d : detalles.findByEvaluacionId(e.getId()))
            actuales.put(d.getCriterio().getId(), d);

        for (EvaluacionDto.NotaItem n : r.notas()) {
            EvaluacionAlumnoDetalle d = actuales.get(n.criterioId());
            if (d == null) {
                d = new EvaluacionAlumnoDetalle();
                d.setEvaluacion(e);
                d.setCriterio(porId.get(n.criterioId()));
            }
            d.setNota(n.nota());
            detalles.save(d);
        }
        return armar(p, u, crit);
    }

    // ---------- auxiliares ----------
    private AutoevaluacionDto.Formulario armar(Proyecto p, Usuario u, List<CriterioEsquema> crit) {
        // Solo se muestran las notas que ESTE alumno puso; nunca lo que otros pusieron
        Map<Long, List<AutoevaluacionDto.NotaGuardada>> mias = new HashMap<>();
        for (EvaluacionAlumno e : evaluaciones.findByProyectoIdAndEvaluadorId(p.getId(), u.getId())) {
            mias.put(e.getEvaluado().getId(), detalles.findByEvaluacionId(e.getId()).stream()
                    .map(d -> new AutoevaluacionDto.NotaGuardada(d.getCriterio().getId(), d.getNota()))
                    .toList());
        }

        List<AutoevaluacionDto.Objetivo> objetivos = new ArrayList<>();
        for (GrupoAlumno m : miembros.findByGrupoId(p.getGrupo().getId())) {
            Usuario a = m.getAlumno();
            objetivos.add(new AutoevaluacionDto.Objetivo(a.getId(), a.getNombre() + " " + a.getApellido(),
                    a.getId().equals(u.getId()), mias.containsKey(a.getId()),
                    mias.getOrDefault(a.getId(), List.of())));
        }
        objetivos.sort(Comparator.comparing(AutoevaluacionDto.Objetivo::propio).reversed()
                .thenComparing(AutoevaluacionDto.Objetivo::nombre));

        boolean completo = !objetivos.isEmpty() && objetivos.stream().allMatch(AutoevaluacionDto.Objetivo::evaluado);
        List<EsquemaDto.Criterio> criterios = crit.stream()
                .map(c -> new EsquemaDto.Criterio(c.getId(), c.getNombre(), c.getDescripcion(), c.getPonderacion()))
                .toList();
        return new AutoevaluacionDto.Formulario(p.getId(), p.getTitulo(), criterios, objetivos, completo);
    }

    private List<CriterioEsquema> criteriosActivos(Proyecto p) {
        Long materiaId = p.getGrupo().getAsignacion().getMateria().getId();
        if (esquema.deMateria(materiaId).getPesoAutoevaluacion().signum() <= 0)
            throw Http.conflicto("El docente no activó la autoevaluación para esta materia");
        List<CriterioEsquema> crit = esquema.criterios(materiaId, Modalidad.AUTOEVALUACION);
        if (crit.isEmpty())
            throw Http.conflicto("El docente aún no definió los criterios de la autoevaluación");
        return crit;
    }

    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private void exigirMiembro(Usuario u, Proyecto p) {
        if (!miembros.existsByGrupoIdAndAlumnoId(p.getGrupo().getId(), u.getId()))
            throw Http.prohibido("No perteneces al grupo de este proyecto");
    }
}