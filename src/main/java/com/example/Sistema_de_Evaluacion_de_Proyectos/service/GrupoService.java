package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Matricula;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MatriculaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GrupoService {
    private final GrupoRepository grupos;
    private final AsignacionRepository asignaciones;
    private final MatriculaRepository matriculas;
    private final UsuarioRepository usuarios;
    private final GrupoAlumnoRepository miembros;
    private final ProyectoRepository proyectos;

    @Transactional(readOnly = true)
    public List<GrupoDto.Response> listar(Long asignacionId) {
        var lista = asignacionId == null ? grupos.findAll() : grupos.findByAsignacionId(asignacionId);
        return lista.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public GrupoDto.Response obtener(Long id) { return aDto(buscar(id)); }

    @Transactional
    public GrupoDto.Response crear(GrupoDto.Request r) {
        Asignacion a = asignaciones.findById(r.asignacionId())
                .orElseThrow(() -> Http.noEncontrado("Asignación no encontrada"));
        String nombre = r.nombre().trim();
        if (grupos.existsByAsignacionIdAndNombreIgnoreCase(a.getId(), nombre))
            throw Http.conflicto("Ya existe un grupo con ese nombre en la asignación");
        Grupo g = new Grupo();
        g.setNombre(nombre);
        g.setAsignacion(a);
        return aDto(grupos.save(g));
    }

    @Transactional
    public GrupoDto.Response actualizar(Long id, GrupoDto.Request r) {
        Grupo g = buscar(id);
        if (!Objects.equals(g.getAsignacion().getId(), r.asignacionId()))
            throw Http.invalido("No se puede mover un grupo a otra asignación");
        String nombre = r.nombre().trim();
        if (grupos.existsByAsignacionIdAndNombreIgnoreCaseAndIdNot(g.getAsignacion().getId(), nombre, id))
            throw Http.conflicto("Ya existe un grupo con ese nombre en la asignación");
        g.setNombre(nombre);
        return aDto(grupos.save(g));
    }

    @Transactional
    public void eliminar(Long id) {
        Grupo g = buscar(id);
        if (proyectos.existsByGrupoId(id))
            throw Http.conflicto("El grupo tiene proyectos asociados");
        miembros.deleteAll(miembros.findByGrupoId(id));
        grupos.delete(g);
    }

    @Transactional(readOnly = true)
    public List<GrupoDto.Alumno> alumnos(Long grupoId) {
        buscar(grupoId);
        return miembros.findByGrupoId(grupoId).stream()
                .map(m -> new GrupoDto.Alumno(m.getAlumno().getId(), m.getAlumno().getNombre(),
                        m.getAlumno().getApellido(), m.getAlumno().getCorreo()))
                .toList();
    }
    /** Alumnos matriculados en la subsección de la asignación del grupo y sin grupo en esa asignación. */
    @Transactional(readOnly = true)
    public List<GrupoDto.Alumno> disponibles(Long grupoId) {
        Asignacion asignacion = buscar(grupoId).getAsignacion();
        return matriculas.findBySubseccionId(asignacion.getSubseccion().getId()).stream()
                .map(Matricula::getAlumno)
                .filter(Usuario::isActivo)
                .filter(a -> !miembros.existsByAsignacionIdAndAlumnoId(asignacion.getId(), a.getId()))
                .map(a -> new GrupoDto.Alumno(a.getId(), a.getNombre(), a.getApellido(), a.getCorreo()))
                .toList();
    }

    @Transactional
    public GrupoDto.Alumno agregarAlumno(Long grupoId, Long alumnoId) {
        Grupo g = buscar(grupoId);
        Usuario a = usuarios.findById(alumnoId)
                .orElseThrow(() -> Http.noEncontrado("Alumno no encontrado"));
        if (!"alumno".equalsIgnoreCase(a.getRol().getNombre()))
            throw Http.invalido("El usuario no tiene rol alumno");

        // El alumno debe estar matriculado en la subsección a la que pertenece la asignación del grupo
        Asignacion asignacion = g.getAsignacion();
        Matricula mat = matriculas.findByAlumnoId(alumnoId)
                .orElseThrow(() -> Http.invalido("El alumno no está matriculado en ninguna subsección"));
        if (!Objects.equals(mat.getSubseccion().getId(), asignacion.getSubseccion().getId()))
            throw Http.invalido("El alumno pertenece a otra subsección");

        // Un solo grupo por asignación (materia dentro de la subsección)
        if (miembros.existsByAsignacionIdAndAlumnoId(asignacion.getId(), alumnoId))
            throw Http.conflicto("El alumno ya pertenece a un grupo de esta materia");

        GrupoAlumno m = new GrupoAlumno();
        m.setGrupo(g);
        m.setAsignacion(asignacion);
        m.setAlumno(a);
        miembros.save(m);
        return new GrupoDto.Alumno(a.getId(), a.getNombre(), a.getApellido(), a.getCorreo());
    }

    @Transactional
    public void quitarAlumno(Long grupoId, Long alumnoId) {
        GrupoAlumno m = miembros.findByGrupoIdAndAlumnoId(grupoId, alumnoId)
                .orElseThrow(() -> Http.noEncontrado("El alumno no pertenece a este grupo"));
        miembros.delete(m);
    }

    private Grupo buscar(Long id) {
        return grupos.findById(id).orElseThrow(() -> Http.noEncontrado("Grupo no encontrado"));
    }

    private GrupoDto.Response aDto(Grupo g) {
        Asignacion a = g.getAsignacion();
        String nombreAsignacion = a.getSubseccion().getSeccion().getNombre() + a.getSubseccion().getLetra()
                + " · " + a.getMateria().getNombre();
        return new GrupoDto.Response(g.getId(), g.getNombre(), a.getId(),
                a.getSubseccion().getSeccion().getId(), nombreAsignacion,
                miembros.countByGrupoId(g.getId()));
    }
}