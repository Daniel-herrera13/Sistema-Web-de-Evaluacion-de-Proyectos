package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoAlumnoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/alumnos")
@RequiredArgsConstructor
public class AlumnoController {
    private final UsuarioRepository usuarios;
    private final GrupoAlumnoRepository miembros;

    @GetMapping
    @Transactional(readOnly = true)
    public List<GrupoDto.Alumno> listar(@RequestParam(required = false) Long seccionId,
                                        @RequestParam(required = false) Boolean sinGrupo) {
        List<Usuario> lista = usuarios.findByRolNombre("alumno");
        if (seccionId != null && Boolean.TRUE.equals(sinGrupo)) {
            Set<Long> ocupados = new HashSet<>();
            miembros.findByAsignacionSubseccionSeccionId(seccionId)
                    .forEach(m -> ocupados.add(m.getAlumno().getId()));
            lista = lista.stream().filter(a -> !ocupados.contains(a.getId())).toList();
        }
        return lista.stream()
                .map(a -> new GrupoDto.Alumno(a.getId(), a.getNombre(), a.getApellido(), a.getCorreo()))
                .toList();
    }
}