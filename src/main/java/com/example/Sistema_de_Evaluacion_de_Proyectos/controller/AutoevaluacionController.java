package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.AutoevaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.AutoevaluacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autoevaluacion")
@RequiredArgsConstructor
public class AutoevaluacionController {
    private final AutoevaluacionService service;

    @GetMapping("/proyecto/{proyectoId}")
    public AutoevaluacionDto.Formulario formulario(@PathVariable Long proyectoId, Authentication auth) {
        return service.formulario(proyectoId, auth.getName());
    }

    @PutMapping("/proyecto/{proyectoId}")
    public AutoevaluacionDto.Formulario guardar(@PathVariable Long proyectoId,
                                                @Valid @RequestBody AutoevaluacionDto.Guardar r,
                                                Authentication auth) {
        return service.guardar(proyectoId, r, auth.getName());
    }
}