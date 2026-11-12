package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.ResultadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResultadoController {
    private final ResultadoService service;

    // Alumno
    @GetMapping("/api/resultados/mi-grupo")
    public List<ResultadoDto.MiResultado> miGrupo(Authentication auth) {
        return service.miGrupo(auth.getName());
    }

    // Admin o docente de la materia
    @PostMapping("/api/proyectos/{id}/publicar")
    public ResultadoDto.Publicacion publicar(@PathVariable Long id, Authentication auth) {
        return service.publicar(id, true, auth.getName());
    }

    @PostMapping("/api/proyectos/{id}/despublicar")
    public ResultadoDto.Publicacion despublicar(@PathVariable Long id, Authentication auth) {
        return service.publicar(id, false, auth.getName());
    }
}