package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

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

    /** Resultados de los proyectos del grupo del alumno. Sin publicar, no se muestra ninguna nota. */
    @Transactional(readOnly = true)
    public List<ResultadoDto.MiResultado> miGrupo(String correo) {
        Usuario u = acceso.actor(correo);
        List<ResultadoDto.MiResultado> salida = new ArrayList<>();
        for (GrupoAlumno m : miembros.findByAlumnoId(u.getId())) {
            for (Proyecto p : proyectos.findByGrupoId(m.getGrupo().getId())) {
                salida.add(vista(p));
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
        if (publicar && !r.definitiva())
            throw Http.conflicto("Aún hay evaluadores sin enviar su evaluación (" + r.enviadas()
                    + " de " + r.invitados() + ")");

        p.setResultadosPublicados(publicar);
        proyectos.save(p);
        return new ResultadoDto.Publicacion(proyectoId, publicar, r.notaFinal());
    }

    private ResultadoDto.MiResultado vista(Proyecto p) {
        if (!Boolean.TRUE.equals(p.getResultadosPublicados())) {
            return new ResultadoDto.MiResultado(p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                    false, null, List.of(), List.of());
        }
        ResultadoDto.Resumen r = calculo.resumen(p.getId());
        // Comentarios sin el nombre del evaluador, para que el resultado sea anónimo
        List<String> comentarios = r.evaluadores().stream()
                .map(ResultadoDto.PorEvaluador::comentario)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        return new ResultadoDto.MiResultado(p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                true, r.notaFinal(), r.criterios(), comentarios);
    }
}