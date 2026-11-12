package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final ProyectoRepository proyectos;
    private final CalculoNotaService calculo;
    private final AccesoService acceso;

    /** Listado filtrado. Cada rol ve solo los proyectos a los que tiene acceso. */
    @Transactional(readOnly = true)
    public List<ReporteDto.Fila> listar(ReporteDto.Filtros f, String correo) {
        Usuario u = acceso.actor(correo);
        boolean ocultaNota = "jurado".equals(acceso.rol(u));
        return proyectos.findAll().stream()
                .filter(p -> coincide(p, f))
                .filter(p -> acceso.puedeVer(u, p))
                .map(p -> fila(p, calculo.resumen(p.getId()), ocultaNota))
                .toList();
    }

    /** Detalle de un proyecto. El jurado solo ve su propia evaluación. */
    @Transactional(readOnly = true)
    public ResultadoDto.Resumen detalle(Long proyectoId, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyectos.findById(proyectoId)
                .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
        acceso.exigirAcceso(u, p);
        ResultadoDto.Resumen r = calculo.resumen(proyectoId);
        return "jurado".equals(acceso.rol(u)) ? r.paraJurado(u.getId()) : r;
    }

    // ---------- tablas para exportar ----------
    @Transactional(readOnly = true)
    public ReporteDto.Tabla tablaListado(ReporteDto.Filtros f, String correo) {
        List<List<Object>> filas = new ArrayList<>();
        for (ReporteDto.Fila x : listar(f, correo)) {
            filas.add(List.of(
                    x.titulo(), x.grupo(), x.seccion(), x.ciclo(), x.materia(),
                    x.empresa() == null ? "" : x.empresa(), x.estado(),
                    x.invitados(), x.enviadas(),
                    x.notaFinal() == null ? "" : x.notaFinal(),
                    x.definitiva(), x.publicado()));
        }
        return new ReporteDto.Tabla("Reporte de proyectos",
                List.of("Proyecto", "Grupo", "Sección", "Ciclo", "Materia", "Empresa", "Estado",
                        "Evaluadores", "Enviadas", "Nota final", "Definitiva", "Publicado"),
                filas);
    }

    @Transactional(readOnly = true)
    public ReporteDto.Tabla tablaDetalle(Long proyectoId, String correo) {
        ResultadoDto.Resumen r = detalle(proyectoId, correo);
        List<List<Object>> filas = new ArrayList<>();
        for (ResultadoDto.PorEvaluador e : r.evaluadores()) {
            filas.add(List.of("Evaluador: " + e.nombre(), e.rol(), e.estado(),
                    e.nota() == null ? "" : e.nota()));
        }
        for (ResultadoDto.PorCriterio c : r.criterios()) {
            filas.add(List.of("Criterio: " + c.nombre(), "Ponderación " + c.ponderacion() + "%", "",
                    c.promedio()));
        }
        String nota = r.notaFinal() == null ? "sin nota aún" : r.notaFinal().toPlainString();
        return new ReporteDto.Tabla("Proyecto: " + r.titulo() + " | Grupo " + r.grupo() + " | Nota final: " + nota,
                List.of("Concepto", "Detalle", "Estado", "Nota"), filas);
    }

    // ---------- auxiliares ----------
    private boolean coincide(Proyecto p, ReporteDto.Filtros f) {
        Grupo g = p.getGrupo();
        Seccion s = g.getSeccion();
        return (f.materiaId() == null || s.getMateria().getId().equals(f.materiaId()))
                && (f.seccionId() == null || s.getId().equals(f.seccionId()))
                && (f.grupoId() == null || g.getId().equals(f.grupoId()))
                && (f.empresaId() == null || (p.getEmpresa() != null && p.getEmpresa().getId().equals(f.empresaId())))
                && (f.estado() == null || f.estado().isBlank() || f.estado().equalsIgnoreCase(p.getEstado()))
                && (f.ciclo() == null || f.ciclo().isBlank() || f.ciclo().equalsIgnoreCase(s.getCiclo()))
                && (f.publicado() == null || f.publicado().equals(Boolean.TRUE.equals(p.getResultadosPublicados())));
    }

    private ReporteDto.Fila fila(Proyecto p, ResultadoDto.Resumen r, boolean ocultaNota) {
        return new ReporteDto.Fila(p.getId(), p.getTitulo(), r.grupo(), r.seccion(), r.ciclo(),
                r.materia(), r.empresa(), p.getEstado(), r.invitados(), r.enviadas(),
                ocultaNota ? null : r.notaFinal(), r.definitiva(), r.publicado());
    }
}