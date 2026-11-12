package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Rol;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.RolRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        for (String n : List.of("alumno", "jurado", "docente", "admin")) {
            if (roles.findByNombre(n).isEmpty()) {
                Rol r = new Rol();
                r.setNombre(n);
                roles.save(r);
            }
        }
        crearUsuario("Admin", "admin@itca.edu.sv", "Admin123", "admin");
        crearUsuario("Docente", "docente@itca.edu.sv", "Docente123", "docente");
        crearUsuario("Jurado", "jurado@itca.edu.sv", "Jurado123", "jurado");
        crearUsuario("Alumno", "alumno@itca.edu.sv", "Alumno123", "alumno");
    }

    private void crearUsuario(String nombre, String correo, String pass, String rol) {
        if (usuarios.findByCorreo(correo).isPresent()) return;
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido("Prueba");
        u.setCorreo(correo);
        u.setPassword(encoder.encode(pass));
        u.setRol(roles.findByNombre(rol).orElseThrow());
        usuarios.save(u);
    }
}