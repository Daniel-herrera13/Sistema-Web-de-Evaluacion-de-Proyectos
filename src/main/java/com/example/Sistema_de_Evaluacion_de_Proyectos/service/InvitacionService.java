package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.InvitacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Evaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InvitacionService {
    private final EvaluadorInvitadoRepository invitados;
    private final ProyectoRepository proyectos;
    private final UsuarioRepository usuarios;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionDetalleRepository detalles;
    private final AccesoService acceso;

    @Transactional(readOnly = true)
    public List<InvitacionDto.Response> listar(Long proyectoId, String correo) {
        Usuario actor = acceso.actor(correo);
        Proyecto p = proyecto(proyectoId);
        acceso.exigirGestion(actor, p);
        return invitados.findByProyectoId(proyectoId).stream().map(this::aDto).toList();
    }

    @Transactional
    public InvitacionDto.Response invitar(InvitacionDto.Crear r, String correo) {
        Usuario actor = acceso.actor(correo);
        Proyecto p = proyecto(r.proyectoId());
        acceso.exigirGestion(actor, p);

        Usuario evaluador = usuarios.findById(r.evaluadorId())
                .orElseThrow(() -> Http.noEncontrado("Evaluador no encontrado"));
        if (!Set.of("jurado", "docente").contains(acceso.rol(evaluador)))
            throw Http.invalido("Solo se puede invitar a usuarios con rol jurado o docente");
        if (invitados.existsByProyectoIdAndEvaluadorId(p.getId(), evaluador.getId()))
            throw Http.conflicto("El evaluador ya fue invitado a este proyecto");

        EvaluadorInvitado i = new EvaluadorInvitado();
        i.setProyecto(p);
        i.setEvaluador(evaluador);
        i.setInvitadoPor(actor);
        return aDto(invitados.save(i));
    }

    @Transactional
    public void quitar(Long id, String correo) {
        Usuario actor = acceso.actor(correo);
        EvaluadorInvitado i = invitados.findById(id)
                .orElseThrow(() -> Http.noEncontrado("Invitación no encontrada"));
        acceso.exigirGestion(actor, i.getProyecto());

        Optional<Evaluacion> ev = evaluaciones.findByProyectoIdAndEvaluadorId(
                i.getProyecto().getId(), i.getEvaluador().getId());
        if (ev.isPresent()) {
            if ("ENVIADA".equals(ev.get().getEstado()))
                throw Http.conflicto("El evaluador ya envió su evaluación; debe reabrirla antes de quitarlo");
            detalles.deleteAll(detalles.findByEvaluacionId(ev.get().getId()));
            evaluaciones.delete(ev.get());
        }
        invitados.delete(i);
    }

    /** Proyectos asignados al usuario que inició sesión (jurado o docente). */
    @Transactional(readOnly = true)
    public List<InvitacionDto.Asignado> misProyectos(String correo) {
        Usuario u = acceso.actor(correo);
        return invitados.findByEvaluadorId(u.getId()).stream().map(i -> {
            Proyecto p = i.getProyecto();
            Optional<Evaluacion> ev = evaluaciones.findByProyectoIdAndEvaluadorId(p.getId(), u.getId());
            return new InvitacionDto.Asignado(
                    p.getId(), p.getTitulo(), p.getGrupo().getNombre(),
                    p.getEmpresa() == null ? null : p.getEmpresa().getNombre(),
                    p.getEstado(),
                    ev.map(Evaluacion::getEstado).orElse("SIN_INICIAR"),
                    ev.map(Evaluacion::getId).orElse(null));
        }).toList();
    }

    private Proyecto proyecto(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private InvitacionDto.Response aDto(EvaluadorInvitado i) {
        Usuario e = i.getEvaluador();
        Usuario por = i.getInvitadoPor();
        return new InvitacionDto.Response(i.getId(), i.getProyecto().getId(), i.getProyecto().getTitulo(),
                e.getId(), e.getNombre(), e.getApellido(), e.getCorreo(), e.getRol().getNombre(),
                por == null ? null : por.getNombre() + " " + por.getApellido(), i.getFechaInvitacion());
    }
}