package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.InvitacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Evaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvitacionService {

    private final EvaluadorInvitadoRepository invitados;
    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;
    private final EvaluacionRepository evaluaciones;
    private final AccesoService acceso;

    // ---------- proyectos del usuario autenticado ----------
    @Transactional
    public List<InvitacionDto.Asignado> misProyectos(String correo) {
        Usuario u = acceso.actor(correo);
        Map<Long, Proyecto> visibles = new LinkedHashMap<>();

        if ("admin".equals(acceso.rol(u))) {
            proyectos.findAll().forEach(p -> visibles.put(p.getId(), p));
        } else {
            if ("docente".equals(acceso.rol(u))) {
                // el docente de la materia siempre es evaluador de los proyectos de sus grupos
                for (Proyecto p : proyectos.findByGrupoAsignacionDocenteId(u.getId())) {
                    asegurarDocente(p, u);
                }
            }
            invitados.findByEvaluadorId(u.getId())
                    .forEach(i -> visibles.put(i.getProyecto().getId(), i.getProyecto()));
        }
        return visibles.values().stream().map(p -> aAsignado(p, u)).toList();
    }

    /** Crea la invitación del docente principal si no existe (idempotente). */
    @Transactional
    public void asegurarDocente(Proyecto p, Usuario docente) {
        if (invitados.existsByProyectoIdAndEvaluadorId(p.getId(), docente.getId())) return;
        EvaluadorInvitado i = new EvaluadorInvitado();
        i.setProyecto(p);
        i.setEvaluador(docente);
        i.setInvitadoPor(docente);
        invitados.save(i);
    }

    // ---------- invitaciones de un proyecto ----------
    @Transactional(readOnly = true)
    public List<InvitacionDto.Response> listar(Long proyectoId, String correo) {
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestion(acceso.actor(correo), p);
        return invitados.findByProyectoId(proyectoId).stream().map(this::aResponse).toList();
    }

    @Transactional
    public InvitacionDto.Response invitar(InvitacionDto.Crear r, String correo) {
        Usuario quien = acceso.actor(correo);
        Proyecto p = proyecto(r.proyectoId());
        acceso.exigirGestion(quien, p);

        Usuario evaluador = usuarios.findById(r.evaluadorId())
                .orElseThrow(() -> Http.noEncontrado("El evaluador no existe"));
        String rol = acceso.rol(evaluador);
        if (!evaluador.isActivo()) throw Http.invalido("El evaluador está inactivo");
        if (!"jurado".equals(rol) && !"docente".equals(rol))
            throw Http.invalido("Solo se puede invitar a jurados o docentes");
        if (invitados.existsByProyectoIdAndEvaluadorId(p.getId(), evaluador.getId()))
            throw Http.conflicto("Ese evaluador ya fue invitado a este proyecto");

        EvaluadorInvitado i = new EvaluadorInvitado();
        i.setProyecto(p);
        i.setEvaluador(evaluador);
        i.setInvitadoPor(quien);
        return aResponse(invitados.save(i));
    }

    @Transactional
    public void quitar(Long id, String correo) {
        EvaluadorInvitado i = invitados.findById(id)
                .orElseThrow(() -> Http.noEncontrado("La invitación no existe"));
        acceso.exigirGestion(acceso.actor(correo), i.getProyecto());
        if (acceso.esDocentePrincipal(i.getEvaluador(), i.getProyecto()))
            throw Http.conflicto("El docente de la materia siempre evalúa sus proyectos");
        invitados.delete(i);
    }

    // ---------- auxiliares ----------
    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private InvitacionDto.Asignado aAsignado(Proyecto p, Usuario u) {
        Evaluacion e = evaluaciones.findByProyectoIdAndEvaluadorId(p.getId(), u.getId()).orElse(null);
        return new InvitacionDto.Asignado(
                p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                p.getEmpresa() != null ? p.getEmpresa().getNombre() : null,
                p.getEstado(),
                e == null ? "SIN_INICIAR" : e.getEstado(),
                e == null ? null : e.getId());
    }

    private InvitacionDto.Response aResponse(EvaluadorInvitado i) {
        Usuario e = i.getEvaluador();
        Usuario por = i.getInvitadoPor();
        return new InvitacionDto.Response(
                i.getId(), i.getProyecto().getId(), i.getProyecto().getTitulo(),
                e.getId(), e.getNombre(), e.getApellido(), e.getCorreo(),
                e.getRol().getNombre(),
                por == null ? null : por.getNombre() + " " + por.getApellido(),
                i.getFechaInvitacion());
    }
}