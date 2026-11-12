package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.CriterioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EvaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Evaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluacionService {
    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("10");

    private final EvaluacionRepository evaluaciones;
    private final EvaluacionDetalleRepository detalles;
    private final ProyectoRepository proyectos;
    private final CriterioRepository criterios;
    private final CriterioService criterioService;
    private final CalculoNotaService calculo;
    private final AccesoService acceso;

    /** Formulario del evaluador: criterios heredados del proyecto con las notas que ya haya guardado. */
    @Transactional(readOnly = true)
    public EvaluacionDto.Response miEvaluacion(Long proyectoId, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        exigirInvitado(u, p);
        Evaluacion e = evaluaciones.findByProyectoIdAndEvaluadorId(proyectoId, u.getId()).orElse(null);
        return aDto(p, u, e, criteriosDe(p));
    }

    @Transactional
    public EvaluacionDto.Response guardarBorrador(Long proyectoId, EvaluacionDto.Guardar r, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        exigirInvitado(u, p);
        exigirEditable(p);

        Evaluacion e = evaluaciones.findByProyectoIdAndEvaluadorId(proyectoId, u.getId())
                .orElseGet(() -> nueva(p, u));
        if ("ENVIADA".equals(e.getEstado()))
            throw Http.conflicto("La evaluación ya fue enviada; reábrela para editarla");

        List<CriterioDto.Response> crit = criteriosDe(p);
        e = guardarNotas(e, r, crit);
        return aDto(p, u, e, crit);
    }

    @Transactional
    public EvaluacionDto.Response enviar(Long proyectoId, EvaluacionDto.Guardar r, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        exigirInvitado(u, p);
        exigirEditable(p);

        Evaluacion e = evaluaciones.findByProyectoIdAndEvaluadorId(proyectoId, u.getId())
                .orElseGet(() -> nueva(p, u));
        if ("ENVIADA".equals(e.getEstado()))
            throw Http.conflicto("La evaluación ya fue enviada");

        List<CriterioDto.Response> crit = criteriosDe(p);
        e = guardarNotas(e, r, crit);

        List<EvaluacionDetalle> det = detalles.findByEvaluacionId(e.getId());
        Set<Long> llenos = det.stream().map(d -> d.getCriterio().getId()).collect(Collectors.toSet());
        List<String> faltan = crit.stream().filter(c -> !llenos.contains(c.id()))
                .map(CriterioDto.Response::nombre).toList();
        if (!faltan.isEmpty())
            throw Http.invalido("Faltan notas en: " + String.join(", ", faltan));

        e.setCalificacionFinal(calculo.notaEvaluador(det));
        e.setEstado("ENVIADA");
        e = evaluaciones.save(e);
        return aDto(p, u, e, crit);
    }

    @Transactional
    public EvaluacionDto.Response reabrir(Long evaluacionId, String correo) {
        Usuario u = acceso.actor(correo);
        Evaluacion e = evaluaciones.findById(evaluacionId)
                .orElseThrow(() -> Http.noEncontrado("Evaluación no encontrada"));
        boolean admin = "admin".equals(acceso.rol(u));
        if (!admin && !e.getEvaluador().getId().equals(u.getId()))
            throw Http.prohibido("Solo quien envió la evaluación o un administrador puede reabrirla");
        if (!"ENVIADA".equals(e.getEstado()))
            throw Http.conflicto("La evaluación no está enviada");
        if (Boolean.TRUE.equals(e.getProyecto().getResultadosPublicados()))
            throw Http.conflicto("Los resultados ya fueron publicados; despublícalos antes de reabrir");

        e.setEstado("BORRADOR");
        e.setCalificacionFinal(BigDecimal.ZERO);
        e = evaluaciones.save(e);
        return aDto(e.getProyecto(), e.getEvaluador(), e, criteriosDe(e.getProyecto()));
    }

    // ---------- auxiliares ----------
    private Evaluacion guardarNotas(Evaluacion e, EvaluacionDto.Guardar r, List<CriterioDto.Response> crit) {
        if (r != null && r.comentarios() != null) e.setComentarios(r.comentarios());
        Evaluacion g = evaluaciones.save(e);
        if (r == null || r.notas() == null) return g;

        Set<Long> validos = crit.stream().map(CriterioDto.Response::id).collect(Collectors.toSet());
        Map<Long, EvaluacionDetalle> actuales = new HashMap<>();
        for (EvaluacionDetalle d : detalles.findByEvaluacionId(g.getId()))
            actuales.put(d.getCriterio().getId(), d);

        for (EvaluacionDto.NotaItem n : r.notas()) {
            if (!validos.contains(n.criterioId()))
                throw Http.invalido("El criterio " + n.criterioId() + " no pertenece a este proyecto");
            validarRango(n.nota());
            EvaluacionDetalle d = actuales.get(n.criterioId());
            if (d == null) {
                d = new EvaluacionDetalle();
                d.setEvaluacion(g);
                d.setCriterio(criterios.getReferenceById(n.criterioId()));
            }
            d.setNotaOtorgada(n.nota());
            actuales.put(n.criterioId(), detalles.save(d));
        }
        return g;
    }

    private void validarRango(BigDecimal nota) {
        if (nota == null || nota.compareTo(MIN) < 0 || nota.compareTo(MAX) > 0)
            throw Http.invalido("La nota debe estar entre 0 y 10");
    }

    private Evaluacion nueva(Proyecto p, Usuario u) {
        Evaluacion e = new Evaluacion();
        e.setProyecto(p);
        e.setEvaluador(u);
        return e;
    }

    private List<CriterioDto.Response> criteriosDe(Proyecto p) {
        CriterioDto.Efectivos ef = criterioService.efectivos(p.getId(), null, null, null);
        if (ef.criterios().isEmpty())
            throw Http.conflicto("El proyecto no tiene criterios definidos que sumen 100%");
        return ef.criterios();
    }

    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private void exigirInvitado(Usuario u, Proyecto p) {
        if (!acceso.estaInvitado(u, p))
            throw Http.prohibido("No estás asignado a este proyecto");
    }

    private void exigirEditable(Proyecto p) {
        if (Boolean.TRUE.equals(p.getResultadosPublicados()))
            throw Http.conflicto("Los resultados ya fueron publicados; no se puede modificar la evaluación");
    }

    private EvaluacionDto.Response aDto(Proyecto p, Usuario u, Evaluacion e, List<CriterioDto.Response> crit) {
        Map<Long, BigDecimal> notas = new HashMap<>();
        if (e != null)
            for (EvaluacionDetalle d : detalles.findByEvaluacionId(e.getId()))
                notas.put(d.getCriterio().getId(), d.getNotaOtorgada());

        List<EvaluacionDto.Detalle> det = crit.stream()
                .map(c -> new EvaluacionDto.Detalle(c.id(), c.nombre(), c.ponderacion(), notas.get(c.id())))
                .toList();
        boolean enviada = e != null && "ENVIADA".equals(e.getEstado());
        return new EvaluacionDto.Response(
                e == null ? null : e.getId(), p.getId(), p.getTitulo(),
                u.getId(), u.getNombre() + " " + u.getApellido(),
                e == null ? "SIN_INICIAR" : e.getEstado(),
                enviada ? e.getCalificacionFinal() : null,
                e == null ? null : e.getComentarios(),
                e == null ? null : e.getFecha(), det);
    }
}