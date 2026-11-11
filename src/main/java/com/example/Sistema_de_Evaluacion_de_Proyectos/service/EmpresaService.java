package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EmpresaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Empresa;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EmpresaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;

    @Transactional(readOnly = true)
    public List<EmpresaDto.Response> listar() {
        return empresas.findAll().stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaDto.Response obtener(Long id) { return aDto(buscar(id)); }

    @Transactional
    public EmpresaDto.Response crear(EmpresaDto.Request r) {
        if (empresas.existsByNombreIgnoreCase(r.nombre().trim()))
            throw Http.conflicto("Ya existe una empresa con ese nombre");
        return aDto(empresas.save(aplicar(new Empresa(), r)));
    }

    @Transactional
    public EmpresaDto.Response actualizar(Long id, EmpresaDto.Request r) {
        Empresa e = buscar(id);
        if (empresas.existsByNombreIgnoreCaseAndIdNot(r.nombre().trim(), id))
            throw Http.conflicto("Ya existe una empresa con ese nombre");
        return aDto(empresas.save(aplicar(e, r)));
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa e = buscar(id);
        if (proyectos.existsByEmpresaId(id))
            throw Http.conflicto("La empresa tiene proyectos asociados");
        empresas.delete(e);
    }

    private Empresa buscar(Long id) {
        return empresas.findById(id).orElseThrow(() -> Http.noEncontrado("Empresa no encontrada"));
    }

    private Empresa aplicar(Empresa e, EmpresaDto.Request r) {
        e.setNombre(r.nombre().trim());
        e.setContacto(r.contacto());
        e.setCorreo(r.correo());
        e.setTelefono(r.telefono());
        return e;
    }

    private EmpresaDto.Response aDto(Empresa e) {
        return new EmpresaDto.Response(e.getId(), e.getNombre(), e.getContacto(), e.getCorreo(), e.getTelefono());
    }
}