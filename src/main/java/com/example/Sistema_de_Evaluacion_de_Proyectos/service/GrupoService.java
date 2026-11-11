package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SeccionRepository;
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
    private final SeccionRepository secciones;
    private final UsuarioRepository usuarios;
    private final GrupoAlumnoRepository miembros;
    private final ProyectoRepository proyectos;

    @Transactional(readOnly = true)
    public List<GrupoDto.Response> listar(Long seccionId) {
        var lista = seccionId == null ? grupos.findAll() : grupos.findBySeccionId(seccionId);
        return lista.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public GrupoDto.Response obtener(Long id) { return aDto(buscar(id)); }

    @Transactional
    public GrupoDto.Response crear(GrupoDto.Request r) {
        Seccion s = secciones.findById(r.seccionId())
                .orElseThrow(() -> Http.noEncontrado("Sección no encontrada"));
        String nombre = r.nombre().trim();
        if (grupos.existsBySeccionIdAndNombreIgnoreCase(s.getId(), nombre))
            throw Http.conflicto("Ya existe un grupo con ese nombre en la sección");
        Grupo g = new Grupo();
        g.setNombre(nombre);
        g.setSeccion(s);
        return aDto(grupos.save(g));
    }

    @Transactional
    public GrupoDto.Response actualizar(Long id, GrupoDto.Request r) {
        Grupo g = buscar(id);
        if (!Objects.equals(g.getSeccion().getId(), r.seccionId()))
            throw Http.invalido("No se puede mover un grupo a otra sección");
        String nombre = r.nombre().trim();
        if (grupos.existsBySeccionIdAndNombreIgnoreCaseAndIdNot(g.getSeccion().getId(), nombre, id))
            throw Http.conflicto("Ya existe un grupo con ese nombre en la sección");
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

    @Transactional
    public GrupoDto.Alumno agregarAlumno(Long grupoId, Long alumnoId) {
        Grupo g = buscar(grupoId);
        Usuario a = usuarios.findById(alumnoId)
                .orElseThrow(() -> Http.noEncontrado("Alumno no encontrado"));
        if (!"alumno".equalsIgnoreCase(a.getRol().getNombre()))
            throw Http.invalido("El usuario no tiene rol alumno");
        Long seccionId = g.getSeccion().getId();
        if (miembros.existsBySeccionIdAndAlumnoId(seccionId, alumnoId))
            throw Http.conflicto("El alumno ya pertenece a un grupo de esta sección");
        GrupoAlumno m = new GrupoAlumno();
        m.setGrupo(g);
        m.setSeccion(g.getSeccion());
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
        return new GrupoDto.Response(g.getId(), g.getNombre(), g.getSeccion().getId(),
                g.getSeccion().getNombre(), miembros.countByGrupoId(g.getId()));
    }
}