package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ProyectoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Empresa;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EmpresaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProyectoService {
    private final ProyectoRepository proyectos;
    private final GrupoRepository grupos;
    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final EvaluadorInvitadoRepository invitados;
    private final CriterioRepository criterios;

    @Transactional(readOnly = true)
    public List<ProyectoDto.Response> listar(Long grupoId, Long empresaId) {
        List<Proyecto> lista = grupoId != null ? proyectos.findByGrupoId(grupoId)
                : empresaId != null ? proyectos.findByEmpresaId(empresaId)
                : proyectos.findAll();
        return lista.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDto.Response obtener(Long id) { return aDto(buscar(id)); }

    @Transactional
    public ProyectoDto.Response crear(ProyectoDto.Request r) {
        return aDto(proyectos.save(aplicar(new Proyecto(), r)));
    }

    @Transactional
    public ProyectoDto.Response actualizar(Long id, ProyectoDto.Request r) {
        return aDto(proyectos.save(aplicar(buscar(id), r)));
    }

    @Transactional
    public void eliminar(Long id) {
        Proyecto p = buscar(id);
        invitados.deleteAll(invitados.findByProyectoId(id));
        criterios.deleteAll(criterios.findByProyectoId(id));
        proyectos.delete(p);   // si ya tiene evaluaciones, la BD lo rechaza y responde 409
    }

    @Transactional(readOnly = true)
    public List<ProyectoDto.Evaluador> evaluadores(Long proyectoId) {
        buscar(proyectoId);
        return invitados.findByProyectoId(proyectoId).stream().map(this::aDto).toList();
    }

    @Transactional
    public ProyectoDto.Evaluador invitar(Long proyectoId, Long evaluadorId, String correoQuienInvita) {
        Proyecto p = buscar(proyectoId);
        Usuario u = usuarios.findById(evaluadorId)
                .orElseThrow(() -> Http.noEncontrado("Evaluador no encontrado"));
        String rol = u.getRol().getNombre().toLowerCase();
        if (!Set.of("jurado", "docente").contains(rol))
            throw Http.invalido("Solo se puede invitar a usuarios con rol jurado o docente");
        if (invitados.existsByProyectoIdAndEvaluadorId(proyectoId, evaluadorId))
            throw Http.conflicto("El evaluador ya fue invitado a este proyecto");
        EvaluadorInvitado ei = new EvaluadorInvitado();
        ei.setProyecto(p);
        ei.setEvaluador(u);
        ei.setInvitadoPor(usuarios.findByCorreo(correoQuienInvita).orElse(null));
        return aDto(invitados.save(ei));
    }

    @Transactional
    public void quitarEvaluador(Long proyectoId, Long evaluadorId) {
        invitados.delete(invitados.findByProyectoIdAndEvaluadorId(proyectoId, evaluadorId)
                .orElseThrow(() -> Http.noEncontrado("El evaluador no está invitado a este proyecto")));
    }

    private Proyecto buscar(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private Proyecto aplicar(Proyecto p, ProyectoDto.Request r) {
        Grupo g = grupos.findById(r.grupoId()).orElseThrow(() -> Http.noEncontrado("Grupo no encontrado"));
        Empresa e = r.empresaId() == null ? null
                : empresas.findById(r.empresaId()).orElseThrow(() -> Http.noEncontrado("Empresa no encontrada"));
        p.setTitulo(r.titulo().trim());
        p.setDescripcion(r.descripcion());
        p.setGrupo(g);
        p.setEmpresa(e);
        if (r.estado() != null) p.setEstado(r.estado());
        return p;
    }

    private ProyectoDto.Response aDto(Proyecto p) {
        Grupo g = p.getGrupo();
        Seccion s = g.getSeccion();
        Empresa e = p.getEmpresa();
        return new ProyectoDto.Response(p.getId(), p.getTitulo(), p.getDescripcion(),
                g.getId(), g.getNombre(), s.getId(), s.getMateria().getId(),
                e == null ? null : e.getId(), e == null ? null : e.getNombre(), p.getEstado());
    }

    private ProyectoDto.Evaluador aDto(EvaluadorInvitado i) {
        Usuario u = i.getEvaluador();
        return new ProyectoDto.Evaluador(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getRol().getNombre(), i.getFechaInvitacion());
    }
}