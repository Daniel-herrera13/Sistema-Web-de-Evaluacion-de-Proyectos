package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Rol;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initUsuarios(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.count() == 0) {
                repo.save(new Usuario("Administrador", "admin@itca.edu.sv", encoder.encode("Admin123"), Rol.ADMIN));
                repo.save(new Usuario("Docente Principal", "docente@itca.edu.sv", encoder.encode("Docente123"), Rol.DOCENTE));
                repo.save(new Usuario("Jurado Invitado", "jurado@itca.edu.sv", encoder.encode("Jurado123"), Rol.JURADO));
            }
        };
    }
}