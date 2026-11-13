package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.EsquemaService;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.PublicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/esquemas")
@RequiredArgsConstructor
public class EsquemaController {
    private final EsquemaService esquemas;
    private final PublicoService publico;

    @GetMapping("/materia/{materiaId}")
    public EsquemaDto.Response obtener(@PathVariable Long materiaId, Authentication auth) {
        return esquemas.obtener(materiaId, auth.getName());
    }

    @PutMapping("/materia/{materiaId}")
    public EsquemaDto.Response guardar(@PathVariable Long materiaId,
                                       @Valid @RequestBody EsquemaDto.Guardar r, Authentication auth) {
        return esquemas.guardar(materiaId, r, auth.getName());
    }

    @GetMapping("/proyecto/{proyectoId}/publico")
    public EsquemaDto.PublicoEstado estadoPublico(@PathVariable Long proyectoId, Authentication auth) {
        return publico.estado(proyectoId, auth.getName());
    }

    @PostMapping("/proyecto/{proyectoId}/publico/abrir")
    public EsquemaDto.PublicoEstado abrir(@PathVariable Long proyectoId, Authentication auth) {
        return publico.abrir(proyectoId, auth.getName());
    }

    @PostMapping("/proyecto/{proyectoId}/publico/cerrar")
    public EsquemaDto.PublicoEstado cerrar(@PathVariable Long proyectoId, Authentication auth) {
        return publico.cerrar(proyectoId, auth.getName());
    }
}