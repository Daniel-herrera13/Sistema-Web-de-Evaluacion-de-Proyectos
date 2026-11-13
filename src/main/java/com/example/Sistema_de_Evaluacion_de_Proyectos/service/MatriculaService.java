package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.MatriculaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Matricula;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Subseccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MatriculaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SubseccionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MatriculaService {
    private final MatriculaRepository matriculas;
    private final SubseccionRepository subsecciones;
    private final UsuarioRepository usuarios;
    private final GrupoAlumnoRepository miembros;

    @Transactional(readOnly = true)
    public List<MatriculaDto.SubseccionResponse> listarSubsecciones(Long seccionId) {
        var lista = seccionId == null ? subsecciones.findAll() : subsecciones.findBySeccionId(seccionId);
        return lista.stream()
                .map(s -> new MatriculaDto.SubseccionResponse(s.getId(), s.getLetra(),
                        s.getSeccion().getId(), nombre(s)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MatriculaDto.Response> listar(Long subseccionId) {
        buscarSubseccion(subseccionId);
        return matriculas.findBySubseccionId(subseccionId).stream().map(this::aDto).toList();
    }

    /** Alumnos activos que todavía no tienen matrícula (candidatos a matricular). */
    @Transactional(readOnly = true)
    public List<GrupoDto.Alumno> sinMatricula() {
        return usuarios.findByRolNombreAndActivoTrue("alumno").stream()
                .filter(a -> !matriculas.existsByAlumnoId(a.getId()))
                .map(a -> new GrupoDto.Alumno(a.getId(), a.getNombre(), a.getApellido(), a.getCorreo()))
                .toList();
    }

    @Transactional
    public List<MatriculaDto.Response> matricular(MatriculaDto.Matricular r) {
        Subseccion sub = buscarSubseccion(r.subseccionId());
        List<MatriculaDto.Response> creadas = new ArrayList<>();
        for (Long alumnoId : new LinkedHashSet<>(r.alumnoIds())) {
            Usuario a = usuarios.findById(alumnoId)
                    .orElseThrow(() -> Http.noEncontrado("Alumno no encontrado (id " + alumnoId + ")"));
            if (!"alumno".equalsIgnoreCase(a.getRol().getNombre()))
                throw Http.invalido(a.getNombre() + " " + a.getApellido() + " no tiene rol alumno");
            var existente = matriculas.findByAlumnoId(alumnoId);
            if (existente.isPresent()) {
                String donde = Objects.equals(existente.get().getSubseccion().getId(), sub.getId())
                        ? "esta subsección" : nombre(existente.get().getSubseccion());
                throw Http.conflicto(a.getNombre() + " " + a.getApellido()
                        + " ya está matriculado en " + donde);
            }
            Matricula m = new Matricula();
            m.setAlumno(a);
            m.setSubseccion(sub);
            creadas.add(aDto(matriculas.save(m)));
        }
        return creadas;
    }

    @Transactional
    public void desmatricular(Long matriculaId) {
        Matricula m = matriculas.findById(matriculaId)
                .orElseThrow(() -> Http.noEncontrado("Matrícula no encontrada"));
        boolean enGrupo = miembros.findByAlumnoId(m.getAlumno().getId()).stream()
                .anyMatch(g -> Objects.equals(g.getAsignacion().getSubseccion().getId(), m.getSubseccion().getId()));
        if (enGrupo)
            throw Http.conflicto("El alumno pertenece a un grupo de esta subsección; quítelo del grupo primero");
        matriculas.delete(m);
    }

    private Subseccion buscarSubseccion(Long id) {
        return subsecciones.findById(id).orElseThrow(() -> Http.noEncontrado("Subsección no encontrada"));
    }

    private String nombre(Subseccion s) {
        return s.getSeccion().getNombre() + s.getLetra();
    }

    private MatriculaDto.Response aDto(Matricula m) {
        Usuario a = m.getAlumno();
        return new MatriculaDto.Response(m.getId(), a.getId(), a.getNombre(), a.getApellido(), a.getCorreo(),
                m.getSubseccion().getId(), nombre(m.getSubseccion()));
    }
}
