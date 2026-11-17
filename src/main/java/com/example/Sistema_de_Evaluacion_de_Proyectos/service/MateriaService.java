package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.MateriaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Errores;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MateriaService {

    private final MateriaRepository materias;
    private final UsuarioRepository usuarios;

    public MateriaService(MateriaRepository materias, UsuarioRepository usuarios) {
        this.materias = materias;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<MateriaDto.Respuesta> listar(String correo) {
        Usuario actor = usuarios.findByCorreo(correo)
                .orElseThrow(() -> Errores.noEncontrado("Usuario no encontrado"));
        List<Materia> visibles = "docente".equalsIgnoreCase(actor.getRol().getNombre())
                ? materias.findImpartidasPorDocente(actor.getId())
                : materias.findAll();
        return visibles.stream().map(MateriaDto.Respuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public MateriaDto.Respuesta obtener(Long id) {
        return MateriaDto.Respuesta.de(buscar(id));
    }

    @Transactional
    public MateriaDto.Respuesta crear(MateriaDto.Guardar r) {
        String nombre = r.nombre().trim();
        if (materias.existsByNombreIgnoreCase(nombre)) {
            throw Errores.conflicto("Ya existe una materia llamada \"" + nombre + "\"");
        }
        return MateriaDto.Respuesta.de(materias.save(aplicar(new Materia(), r, nombre)));
    }

    @Transactional
    public MateriaDto.Respuesta actualizar(Long id, MateriaDto.Guardar r) {
        Materia m = buscar(id);
        String nombre = r.nombre().trim();
        if (materias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw Errores.conflicto("Ya existe otra materia llamada \"" + nombre + "\"");
        }
        return MateriaDto.Respuesta.de(materias.save(aplicar(m, r, nombre)));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        materias.deleteById(id);
    }

    private Materia aplicar(Materia m, MateriaDto.Guardar r, String nombre) {
        Usuario docente = usuarios.findById(r.docentePrincipalId())
                .orElseThrow(() -> Errores.invalido("El docente indicado no existe"));
        if (!"docente".equalsIgnoreCase(docente.getRol().getNombre())) {
            throw Errores.invalido("El usuario indicado no tiene el rol de docente");
        }
        m.setNombre(nombre);
        m.setDescripcion(r.descripcion());
        m.setDocentePrincipal(docente);
        return m;
    }

    private Materia buscar(Long id) {
        return materias.findById(id)
                .orElseThrow(() -> Errores.noEncontrado("Materia no encontrada"));
    }
}
