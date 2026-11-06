package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.UsuarioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Errores;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Rol;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.RolRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto.Respuesta> listar() {
        return usuarios.findAll().stream().map(UsuarioDto.Respuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto.Respuesta obtener(Long id) {
        return UsuarioDto.Respuesta.de(buscar(id));
    }

    @Transactional
    public UsuarioDto.Respuesta crear(UsuarioDto.Crear r) {
        String correo = r.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCase(correo)) {
            throw Errores.conflicto("Ya existe un usuario con el correo " + correo);
        }
        Usuario u = new Usuario();
        u.setNombre(r.nombre().trim());
        u.setApellido(r.apellido().trim());
        u.setCorreo(correo);
        u.setPassword(encoder.encode(r.password()));
        u.setRol(buscarRol(r.rolId()));
        u.setActivo(true);
        return UsuarioDto.Respuesta.de(usuarios.save(u));
    }

    @Transactional
    public UsuarioDto.Respuesta actualizar(Long id, UsuarioDto.Actualizar r) {
        Usuario u = buscar(id);
        String correo = r.correo().trim().toLowerCase();
        if (usuarios.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw Errores.conflicto("Ya existe otro usuario con el correo " + correo);
        }
        u.setNombre(r.nombre().trim());
        u.setApellido(r.apellido().trim());
        u.setCorreo(correo);
        if (r.password() != null) {
            u.setPassword(encoder.encode(r.password()));
        }
        u.setRol(buscarRol(r.rolId()));
        u.setActivo(r.activo());
        return UsuarioDto.Respuesta.de(usuarios.save(u));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        usuarios.deleteById(id);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> Errores.noEncontrado("Usuario no encontrado"));
    }

    private Rol buscarRol(Long rolId) {
        return roles.findById(rolId)
                .orElseThrow(() -> Errores.invalido("El rol indicado no existe"));
    }
}