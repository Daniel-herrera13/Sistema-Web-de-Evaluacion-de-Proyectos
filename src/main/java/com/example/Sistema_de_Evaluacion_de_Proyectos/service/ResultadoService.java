package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.NotaFinalDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultadoService {
    private final ProyectoRepository proyectos;
    private final GrupoAlumnoRepository miembros;
    private final CalculoNotaService calculo;
    private final AccesoService acceso;
    private final NotaFinalService notasFinales;

    /** Resultados de los proyectos del grupo del alumno. Sin publicar, no se muestra ninguna nota. */
    @Transactional(readOnly = true)
    public List<ResultadoDto.MiResultado> miGrupo(String correo) {
        Usuario u = acceso.actor(correo);
        List<ResultadoDto.MiResultado> salida = new ArrayList<>();
        for (GrupoAlumno m : miembros.findByAlumnoId(u.getId())) {
            for (Proyecto p : proyectos.findByGrupoId(m.getGrupo().getId())) {
                salida.add(vista(p, u));
            }
        }
        return salida;
    }

    @Transactional
    public ResultadoDto.Publicacion publicar(Long proyectoId, boolean publicar, String correo) {
        Usuario u = acceso.actor(correo);
        Proyecto p = proyectos.findById(proyectoId)
                .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
        acceso.exigirGestion(u, p);

        ResultadoDto.Resumen r = calculo.resumen(proyectoId);
        // Se publica solo si TODAS las modalidades del esquema están completas
        // (docentes/jurado, autoevaluación y evaluación pública cerrada), igual que en Notas finales.
        if (publicar && !notasFinales.notasProyecto(proyectoId).definitiva())
            throw Http.conflicto("Aún faltan notas por completar: evaluaciones de docentes y jurado ("
                    + r.enviadas() + " de " + r.invitados() + "), autoevaluación de todos los alumnos "
                    + "o cierre de la evaluación pública");

        p.setResultadosPublicados(publicar);
        if (publicar) p.setPublicoAbierto(false);
        proyectos.save(p);
        return new ResultadoDto.Publicacion(proyectoId, publicar, r.notaFinal());
    }

    private ResultadoDto.MiResultado vista(Proyecto p, Usuario alumno) {
        if (!Boolean.TRUE.equals(p.getResultadosPublicados())) {
            return new ResultadoDto.MiResultado(p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                    false, null, List.of(), List.of());
        }
        ResultadoDto.Resumen r = calculo.resumen(p.getId());
        // La nota final es la misma que ve el docente en Notas finales (con todas las modalidades)
        NotaFinalDto.NotaAlumno mia = notasFinales.notasProyecto(p.getId()).alumnos().stream()
                .filter(a -> a.alumnoId().equals(alumno.getId())).findFirst().orElse(null);
        // Comentarios sin el nombre del evaluador, para que el resultado sea anónimo
        List<String> comentarios = r.evaluadores().stream()
                .map(ResultadoDto.PorEvaluador::comentario)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        return new ResultadoDto.MiResultado(p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                true, mia == null ? null : mia.notaFinal(), r.criterios(), comentarios);
    }
}
