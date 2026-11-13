package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.NotaFinalDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.NotaFinalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotaFinalController {
    private final NotaFinalService service;

    // Admin y docente
    @GetMapping("/api/notas/proyecto/{id}")
    public NotaFinalDto.NotasProyecto deProyecto(@PathVariable Long id, Authentication auth) {
        return service.deProyecto(id, auth.getName());
    }

    // Alumno
    @GetMapping("/api/resultados/mi-nota")
    public List<NotaFinalDto.MiNota> miNota(Authentication auth) {
        return service.miNota(auth.getName());
    }
}