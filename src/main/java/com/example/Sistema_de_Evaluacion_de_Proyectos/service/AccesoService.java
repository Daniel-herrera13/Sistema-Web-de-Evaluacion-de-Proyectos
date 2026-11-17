package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class AccesoService {
    private final UsuarioRepository usuarios;
    private final EvaluadorInvitadoRepository invitados;
    private final GrupoAlumnoRepository miembros;
    private final ProyectoRepository proyectos;

    public Usuario actor(String correo) {
        return usuarios.findByCorreo(correo)
                .orElseThrow(() -> Http.noEncontrado("Usuario no encontrado"));
    }

    public String rol(Usuario u) {
        return u.getRol().getNombre().toLowerCase();
    }

    /** Docente titular de la materia en la subsección del proyecto (Asignacion.docente). */
    public boolean esDocentePrincipal(Usuario u, Proyecto p) {
        Usuario d = p.getGrupo().getAsignacion().getDocente();
        return d != null && d.getId().equals(u.getId());
    }

    public boolean estaInvitado(Usuario u, Proyecto p) {
        return invitados.existsByProyectoIdAndEvaluadorId(p.getId(), u.getId());
    }

    public boolean esDocenteResponsable(Usuario u, Proyecto p) {
        if (esDocentePrincipal(u, p)) return true;
        Usuario principalMateria = p.getGrupo().getAsignacion().getMateria().getDocentePrincipal();
        return principalMateria != null && principalMateria.getId().equals(u.getId());
    }

    public boolean puedeVer(Usuario u, Proyecto p) {
        return switch (rol(u)) {
            case "admin" -> true;
            case "docente" -> esDocentePrincipal(u, p) || estaInvitado(u, p);
            case "jurado" -> estaInvitado(u, p);
            case "alumno" -> miembros.existsByGrupoIdAndAlumnoId(p.getGrupo().getId(), u.getId());
            default -> false;
        };
    }

    public void exigirAcceso(Usuario u, Proyecto p) {
        if (!puedeVer(u, p)) throw Http.prohibido("No tienes acceso a este proyecto");
    }

    public Collection<Long> proyectoIdsVisibles(Authentication auth) {
        Usuario u = actor(auth.getName());
        return proyectos.findAll().stream()
                .filter(p -> puedeVer(u, p))
                .map(Proyecto::getId)
                .toList();
    }

    public boolean puedeGestionar(Usuario u, Proyecto p) {
        String rol = rol(u);
        return "admin".equals(rol) || ("docente".equals(rol) && esDocentePrincipal(u, p));
    }

    public void exigirGestion(Usuario u, Proyecto p) {
        if (!puedeGestionar(u, p))
            throw Http.prohibido("Solo el administrador o el docente de la materia puede hacer esto");
    }

    public void exigirGestionPublica(Usuario u, Proyecto p) {
        if (!puedeGestionarPublica(u, p))
            throw Http.prohibido("Solo el administrador o el docente responsable de la materia puede gestionar el código público");
    }

    public boolean puedeGestionarPublica(Usuario u, Proyecto p) {
        String r = rol(u);
        return "admin".equals(r) || ("docente".equals(r) && esDocenteResponsable(u, p));
    }
}
