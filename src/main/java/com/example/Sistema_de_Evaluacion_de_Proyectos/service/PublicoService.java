package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EvaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.PublicoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema.Modalidad;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionPublica;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionPublicaDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionPublicaDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionPublicaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicoService {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom azar = new SecureRandom();

    private final ProyectoRepository proyectos;
    private final EvaluacionPublicaRepository evaluaciones;
    private final EvaluacionPublicaDetalleRepository detalles;
    private final EsquemaService esquema;
    private final AccesoService acceso;

    @Transactional(readOnly = true)
    public List<EsquemaDto.ProyectoPublico> proyectosGestionables(String correo) {
        var actor = acceso.actor(correo);
        return proyectos.findAll().stream()
                .filter(p -> acceso.puedeGestionarPublica(actor, p))
                .map(p -> new EsquemaDto.ProyectoPublico(p.getId(), p.getTitulo(), p.getGrupo().getNombre()))
                .toList();
    }

    // ---------- gestión (admin o docente de la materia) ----------
    @Transactional
    public EsquemaDto.PublicoEstado abrir(Long proyectoId, String correo) {
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestionPublica(acceso.actor(correo), p);
        if (p.getCodigoValidacionPublica() == null || p.getCodigoValidacionPublica().isBlank())
            throw Http.conflicto("Define y guarda un codigo de validacion antes de abrir la evaluacion");
        if (esquema.deProyecto(p).getPesoPublico().signum() <= 0)
            throw Http.conflicto("El esquema de la materia no usa evaluación pública");
        if (criterios(p).isEmpty())
            throw Http.conflicto("El docente aún no definió los criterios de la evaluación pública");
        if (Boolean.TRUE.equals(p.getResultadosPublicados()))
            throw Http.conflicto("Los resultados ya fueron publicados");

        if (p.getCodigoPublico() == null) p.setCodigoPublico(generarCodigoProyecto());
        p.setPublicoAbierto(true);
        return estado(proyectos.save(p));
    }

    @Transactional
    public EsquemaDto.PublicoEstado cambiarCodigoValidacion(Long proyectoId, String codigo, String correo) {
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestionPublica(acceso.actor(correo), p);
        String normalizado = codigo == null ? "" : codigo.trim().toUpperCase();
        if (!normalizado.matches("[A-Z0-9-]{4,20}"))
            throw Http.invalido("El código debe tener entre 4 y 20 caracteres: letras, números o guiones");
        p.setCodigoValidacionPublica(normalizado);
        return estado(proyectos.save(p));
    }

    @Transactional
    public EsquemaDto.PublicoEstado cerrar(Long proyectoId, String correo) {
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestionPublica(acceso.actor(correo), p);
        p.setPublicoAbierto(false);
        return estado(proyectos.save(p));
    }

    @Transactional(readOnly = true)
    public EsquemaDto.PublicoEstado estado(Long proyectoId, String correo) {
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestionPublica(acceso.actor(correo), p);
        return estado(p);
    }

    // ---------- público anónimo ----------
    @Transactional(readOnly = true)
    public PublicoDto.Formulario formulario(String codigo, String codigoValidacion) {
        Proyecto p = abierto(codigo);
        exigirCodigoValidacion(p, codigoValidacion);
        return new PublicoDto.Formulario(p.getTitulo(), p.getGrupo().getNombre(),
                p.getEmpresa() == null ? null : p.getEmpresa().getNombre(),
                criterios(p).stream()
                        .map(c -> new EsquemaDto.Criterio(c.getId(), c.getNombre(), c.getDescripcion(),
                                c.getPonderacion()))
                        .toList());
    }

    @Transactional
    public void evaluar(String codigo, PublicoDto.Evaluar r) {
        Proyecto p = abierto(codigo);
        exigirCodigoValidacion(p, r.codigoValidacion());
        List<CriterioEsquema> crit = criterios(p);
        Map<Long, CriterioEsquema> porId = crit.stream()
                .collect(Collectors.toMap(CriterioEsquema::getId, c -> c));

        Set<Long> enviados = new HashSet<>();
        for (EvaluacionDto.NotaItem n : r.notas()) {
            if (!porId.containsKey(n.criterioId()))
                throw Http.invalido("El criterio " + n.criterioId() + " no pertenece a esta evaluación");
            Notas.validar(n.nota());
            if (!enviados.add(n.criterioId()))
                throw Http.invalido("El criterio " + n.criterioId() + " está repetido");
        }
        if (enviados.size() != crit.size())
            throw Http.invalido("Debes calificar todos los criterios");

        EvaluacionPublica e = new EvaluacionPublica();
        e.setProyecto(p);
        e = evaluaciones.save(e);
        for (EvaluacionDto.NotaItem n : r.notas()) {
            EvaluacionPublicaDetalle d = new EvaluacionPublicaDetalle();
            d.setEvaluacion(e);
            d.setCriterio(porId.get(n.criterioId()));
            d.setNota(n.nota());
            detalles.save(d);
        }
    }

    // ---------- auxiliares ----------
    private Proyecto abierto(String codigo) {
        Proyecto p = proyectos.findByCodigoPublico(codigo == null ? "" : codigo.trim().toUpperCase())
                .orElseThrow(() -> Http.noEncontrado("Código no válido"));
        if (!Boolean.TRUE.equals(p.getPublicoAbierto()))
            throw Http.conflicto("La evaluación pública de este proyecto está cerrada");
        return p;
    }

    private void exigirCodigoValidacion(Proyecto p, String codigo) {
        String recibido = codigo == null ? "" : codigo.trim();
        String esperado = p.getCodigoValidacionPublica();
        if (esperado == null || !esperado.equalsIgnoreCase(recibido))
            throw Http.prohibido("El codigo de validacion no es correcto");
    }

    private List<CriterioEsquema> criterios(Proyecto p) {
        List<CriterioEsquema> crit = esquema.criterios(p, Modalidad.PUBLICO);
        if (crit.isEmpty()) throw Http.conflicto("La evaluación pública no tiene criterios definidos");
        return crit;
    }

    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private EsquemaDto.PublicoEstado estado(Proyecto p) {
        return new EsquemaDto.PublicoEstado(p.getCodigoPublico(), p.getCodigoValidacionPublica(),
                Boolean.TRUE.equals(p.getPublicoAbierto()), evaluaciones.countByProyectoId(p.getId()));
    }

    private String generarCodigoProyecto() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
            codigo = sb.toString();
        } while (proyectos.existsByCodigoPublico(codigo));
        return codigo;
    }

}
