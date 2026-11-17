package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.NotaFinalDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final ProyectoRepository proyectos;
    private final CalculoNotaService calculo;
    private final NotaFinalService notasFinales;
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
        boolean jurado = "jurado".equalsIgnoreCase(acceso.rol(acceso.actor(correo)));
        for (ReporteDto.Fila x : listar(f, correo)) {
            List<NotaFinalDto.NotaAlumno> alumnos = jurado
                    ? List.of() : notasFinales.notasProyecto(x.proyectoId()).alumnos();
            if (alumnos.isEmpty()) {
                filas.add(filaListado(x, null));
            } else {
                for (NotaFinalDto.NotaAlumno alumno : alumnos) filas.add(filaListado(x, alumno));
            }
        }
        return new ReporteDto.Tabla("Reporte de proyectos",
                List.of("Proyecto", "Grupo", "Seccion", "Ciclo", "Materia", "Empresa", "Estado",
                        "Evaluadores", "Enviadas", "Nota docentes/jurado", "Evaluacion completa", "Publicado",
                        "Alumno", "Nota final alumno", "Estado nota alumno"),
                filas);
    }

    private List<Object> filaListado(ReporteDto.Fila x, NotaFinalDto.NotaAlumno alumno) {
        return Arrays.asList(
                x.titulo(), x.grupo(), x.seccion(), x.ciclo(), x.materia(),
                x.empresa() == null ? "" : x.empresa(), x.estado(),
                x.invitados(), x.enviadas(),
                x.notaFinal() == null ? "" : x.notaFinal(),
                x.definitiva() ? "Si" : "No", x.publicado() ? "Si" : "No",
                alumno == null ? "" : alumno.nombre(),
                alumno == null || alumno.notaFinal() == null ? "" : alumno.notaFinal(),
                alumno == null ? "" : alumno.definitiva() ? "Definitiva" : "Provisional");
    }

    @Transactional(readOnly = true)
    public ReporteDto.Tabla tablaDetalle(Long proyectoId, String correo) {
        Usuario u = acceso.actor(correo);
        boolean jurado = "jurado".equals(acceso.rol(u));
        ResultadoDto.Resumen r = detalle(proyectoId, correo);   // valida el acceso

        List<List<Object>> filas = new ArrayList<>();
        for (ResultadoDto.PorEvaluador e : r.evaluadores()) {
            filas.add(Arrays.asList("Evaluador: " + e.nombre(), e.rol(), e.estado(),
                    e.nota() == null ? "" : e.nota()));
        }
        for (ResultadoDto.PorCriterio c : r.criterios()) {
            filas.add(Arrays.asList("Criterio: " + c.nombre(), "Ponderación " + c.ponderacion() + "%", "",
                    c.promedio()));
        }
        // Alumnos del grupo con su nota final (la misma de Notas finales). El jurado no ve notas de alumnos.
        if (!jurado) {
            NotaFinalDto.NotasProyecto nf = notasFinales.notasProyecto(proyectoId);
            for (NotaFinalDto.NotaAlumno a : nf.alumnos()) {
                filas.add(Arrays.asList("Alumno: " + a.nombre(),
                        "Docentes/jurado " + fmt(a.notaDocenteJurado())
                                + " | Autoevaluación " + fmt(a.notaAutoevaluacion())
                                + " | Público " + fmt(a.notaPublico()),
                        a.definitiva() ? "Definitiva" : "Provisional",
                        a.notaFinal() == null ? "" : a.notaFinal()));
            }
        }

        String titulo = "Proyecto: " + r.titulo()
                + " | Materia: " + r.materia()
                + " | Sección: " + r.seccion()
                + " | Grupo: " + r.grupo()
                + " | Ciclo: " + r.ciclo()
                + " | Empresa: " + (r.empresa() == null ? "Sin empresa" : r.empresa())
                + " | Estado: " + r.estadoProyecto()
                + " | Publicación: " + (r.publicado() ? "Publicados" : "Pendiente");
        if (!jurado) {
            titulo += " | Nota docentes/jurado: "
                    + (r.notaFinal() == null ? "sin nota aún" : r.notaFinal().toPlainString());
        }
        return new ReporteDto.Tabla(titulo, List.of("Concepto", "Detalle", "Estado", "Nota"), filas);
    }

    // ---------- auxiliares ----------
    private String fmt(BigDecimal n) {
        return n == null ? "-" : n.toPlainString();
    }

    private boolean coincide(Proyecto p, ReporteDto.Filtros f) {
        Grupo g = p.getGrupo();
        Asignacion a = g.getAsignacion();
        Seccion s = a.getSubseccion().getSeccion();
        return (f.materiaId() == null || a.getMateria().getId().equals(f.materiaId()))
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
