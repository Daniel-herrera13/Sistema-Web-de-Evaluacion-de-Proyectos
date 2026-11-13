package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.CriterioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Criterio;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CriterioService {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private enum Nivel { MATERIA, ASIGNACION, GRUPO, PROYECTO }
    private record Ambito(Nivel nivel, Long id) {}

    private final CriterioRepository criterios;
    private final MateriaRepository materias;
    private final AsignacionRepository asignaciones;
    private final GrupoRepository grupos;
    private final ProyectoRepository proyectos;

    // ---------- CRUD ----------
    @Transactional(readOnly = true)
    public List<CriterioDto.Response> listar(Long m, Long a, Long g, Long p) {
        if (m == null && a == null && g == null && p == null)
            return criterios.findAll().stream().map(this::aDto).toList();
        return delAmbito(ambito(m, a, g, p)).stream().map(this::aDto).toList();
    }

    @Transactional
    public CriterioDto.Response crear(CriterioDto.Request r) {
        Ambito a = ambito(r.materiaId(), r.asignacionId(), r.grupoId(), r.proyectoId());
        BigDecimal actual = suma(delAmbito(a));
        if (actual.add(r.ponderacion()).compareTo(CIEN) > 0)
            throw Http.conflicto("La suma de ponderaciones del nivel pasaría de 100% (actual: " + actual + "%)");
        Criterio c = new Criterio();
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        c.setPonderacion(r.ponderacion());
        asignar(c, a);
        return aDto(criterios.save(c));
    }

    @Transactional
    public CriterioDto.Response actualizar(Long id, CriterioDto.Request r) {
        Criterio c = buscar(id);
        Ambito a = ambitoDe(c);   // el nivel no se puede cambiar; se ignoran los IDs del body
        BigDecimal otros = suma(delAmbito(a).stream().filter(x -> !x.getId().equals(id)).toList());
        if (otros.add(r.ponderacion()).compareTo(CIEN) > 0)
            throw Http.conflicto("La suma de ponderaciones del nivel pasaría de 100% (otros criterios: " + otros + "%)");
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        c.setPonderacion(r.ponderacion());
        return aDto(criterios.save(c));
    }

    @Transactional
    public void eliminar(Long id) { criterios.delete(buscar(id)); }

    // ---------- juego completo de un nivel (exige 100%) ----------
    @Transactional
    public List<CriterioDto.Response> reemplazarConjunto(CriterioDto.Conjunto r) {
        Ambito a = ambito(r.materiaId(), r.asignacionId(), r.grupoId(), r.proyectoId());
        BigDecimal total = r.criterios().stream().map(CriterioDto.Item::ponderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(CIEN) != 0)
            throw Http.invalido("Las ponderaciones deben sumar exactamente 100% (suma enviada: " + total + "%)");
        criterios.deleteAll(delAmbito(a));
        List<Criterio> nuevos = new ArrayList<>();
        for (CriterioDto.Item i : r.criterios()) {
            Criterio c = new Criterio();
            c.setNombre(i.nombre().trim());
            c.setDescripcion(i.descripcion());
            c.setPonderacion(i.ponderacion());
            asignar(c, a);
            nuevos.add(c);
        }
        return criterios.saveAll(nuevos).stream().map(this::aDto).toList();
    }

    // ---------- herencia: proyecto > grupo > asignación > materia ----------
    @Transactional(readOnly = true)
    public CriterioDto.Efectivos efectivos(Long proyectoId, Long grupoId, Long asignacionId, Long materiaId) {
        Long p = null, g = null, a = null, m = null;
        if (proyectoId != null) {
            Proyecto x = proyectos.findById(proyectoId)
                    .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
            p = x.getId();
            g = x.getGrupo().getId();
            a = x.getGrupo().getAsignacion().getId();
            m = x.getGrupo().getAsignacion().getMateria().getId();
        } else if (grupoId != null) {
            Grupo x = grupos.findById(grupoId)
                    .orElseThrow(() -> Http.noEncontrado("Grupo no encontrado"));
            g = x.getId();
            a = x.getAsignacion().getId();
            m = x.getAsignacion().getMateria().getId();
        } else if (asignacionId != null) {
            Asignacion x = asignaciones.findById(asignacionId)
                    .orElseThrow(() -> Http.noEncontrado("Asignación no encontrada"));
            a = x.getId();
            m = x.getMateria().getId();
        } else if (materiaId != null) {
            if (!materias.existsById(materiaId)) throw Http.noEncontrado("Materia no encontrada");
            m = materiaId;
        } else {
            throw Http.invalido("Indique proyectoId, grupoId, asignacionId o materiaId");
        }

        Optional<CriterioDto.Efectivos> r;
        if (p != null && (r = completo("PROYECTO", criterios.findByProyectoId(p))).isPresent()) return r.get();
        if (g != null && (r = completo("GRUPO", criterios.findByGrupoId(g))).isPresent()) return r.get();
        if (a != null && (r = completo("ASIGNACION", criterios.findByAsignacionId(a))).isPresent()) return r.get();
        if (m != null && (r = completo("MATERIA", criterios.findByMateriaId(m))).isPresent()) return r.get();
        return new CriterioDto.Efectivos("NINGUNO", BigDecimal.ZERO, List.of());
    }

    private Optional<CriterioDto.Efectivos> completo(String origen, List<Criterio> lista) {
        BigDecimal total = suma(lista);
        if (lista.isEmpty() || total.compareTo(CIEN) != 0) return Optional.empty();
        return Optional.of(new CriterioDto.Efectivos(origen, total, lista.stream().map(this::aDto).toList()));
    }

    // ---------- auxiliares ----------
    private Ambito ambito(Long m, Long a, Long g, Long p) {
        long n = Stream.of(m, a, g, p).filter(Objects::nonNull).count();
        if (n != 1) throw Http.invalido("Indique exactamente uno: materiaId, asignacionId, grupoId o proyectoId");
        if (m != null) {
            if (!materias.existsById(m)) throw Http.noEncontrado("Materia no encontrada");
            return new Ambito(Nivel.MATERIA, m);
        }
        if (a != null) {
            if (!asignaciones.existsById(a)) throw Http.noEncontrado("Asignación no encontrada");
            return new Ambito(Nivel.ASIGNACION, a);
        }
        if (g != null) {
            if (!grupos.existsById(g)) throw Http.noEncontrado("Grupo no encontrado");
            return new Ambito(Nivel.GRUPO, g);
        }
        if (!proyectos.existsById(p)) throw Http.noEncontrado("Proyecto no encontrado");
        return new Ambito(Nivel.PROYECTO, p);
    }

    private Ambito ambitoDe(Criterio c) {
        if (c.getProyecto() != null)   return new Ambito(Nivel.PROYECTO, c.getProyecto().getId());
        if (c.getGrupo() != null)      return new Ambito(Nivel.GRUPO, c.getGrupo().getId());
        if (c.getAsignacion() != null) return new Ambito(Nivel.ASIGNACION, c.getAsignacion().getId());
        return new Ambito(Nivel.MATERIA, c.getMateria().getId());
    }

    private List<Criterio> delAmbito(Ambito a) {
        return switch (a.nivel()) {
            case MATERIA    -> criterios.findByMateriaId(a.id());
            case ASIGNACION -> criterios.findByAsignacionId(a.id());
            case GRUPO      -> criterios.findByGrupoId(a.id());
            case PROYECTO   -> criterios.findByProyectoId(a.id());
        };
    }

    private void asignar(Criterio c, Ambito a) {
        switch (a.nivel()) {
            case MATERIA    -> c.setMateria(materias.getReferenceById(a.id()));
            case ASIGNACION -> c.setAsignacion(asignaciones.getReferenceById(a.id()));
            case GRUPO      -> c.setGrupo(grupos.getReferenceById(a.id()));
            case PROYECTO   -> c.setProyecto(proyectos.getReferenceById(a.id()));
        }
    }

    private BigDecimal suma(List<Criterio> lista) {
        return lista.stream().map(Criterio::getPonderacion).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Criterio buscar(Long id) {
        return criterios.findById(id).orElseThrow(() -> Http.noEncontrado("Criterio no encontrado"));
    }

    private CriterioDto.Response aDto(Criterio c) {
        Ambito a = ambitoDe(c);
        return new CriterioDto.Response(c.getId(), c.getNombre(), c.getDescripcion(),
                c.getPonderacion(), a.nivel().name(), a.id());
    }
}