package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ProyectoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Empresa;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EmpresaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProyectoService {
    private final ProyectoRepository proyectos;
    private final GrupoRepository grupos;
    private final EmpresaRepository empresas;
    private final EvaluadorInvitadoRepository invitados;
    private final CriterioRepository criterios;
    private final AccesoService acceso;

    @Transactional(readOnly = true)
    public List<ProyectoDto.Response> listar(Long grupoId, Long empresaId, String correo) {
        Usuario u = acceso.actor(correo);
        List<Proyecto> lista = grupoId != null ? proyectos.findByGrupoId(grupoId)
                : empresaId != null ? proyectos.findByEmpresaId(empresaId)
                : proyectos.findAll();
        return lista.stream().filter(p -> acceso.puedeVer(u, p)).map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public ProyectoDto.Response obtener(Long id, String correo) {
        Proyecto p = buscar(id);
        acceso.exigirAcceso(acceso.actor(correo), p);
        return aDto(p);
    }

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

    private Proyecto buscar(Long id) {
        return proyectos.findById(id).orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
    }

    private Proyecto aplicar(Proyecto p, ProyectoDto.Request r) {
        Grupo g = grupos.findById(r.grupoId()).orElseThrow(() -> Http.noEncontrado("Grupo no encontrado"));
        // Un proyecto por grupo (y cada grupo es de una sola materia/asignación)
        boolean otroProyecto = proyectos.findByGrupoId(g.getId()).stream()
                .anyMatch(x -> !Objects.equals(x.getId(), p.getId()));
        if (otroProyecto)
            throw Http.conflicto("El grupo ya tiene un proyecto en esta materia");
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
        Asignacion a = g.getAsignacion();
        Empresa e = p.getEmpresa();
        // Se mantiene la forma del DTO: seccionId = sección (SOFT41) y materiaId = materia de la asignación
        return new ProyectoDto.Response(p.getId(), p.getTitulo(), p.getDescripcion(),
                g.getId(), g.getNombre(), a.getSubseccion().getSeccion().getId(), a.getMateria().getId(),
                e == null ? null : e.getId(), e == null ? null : e.getNombre(),
                p.getEstado(), p.getResultadosPublicados());
    }
}