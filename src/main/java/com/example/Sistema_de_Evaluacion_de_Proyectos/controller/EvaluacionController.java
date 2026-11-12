package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EvaluacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.EvaluacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionController {
    private final EvaluacionService service;

    @GetMapping("/proyecto/{proyectoId}/mia")
    public EvaluacionDto.Response mia(@PathVariable Long proyectoId, Authentication auth) {
        return service.miEvaluacion(proyectoId, auth.getName());
    }

    @PutMapping("/proyecto/{proyectoId}/borrador")
    public EvaluacionDto.Response borrador(@PathVariable Long proyectoId,
                                           @Valid @RequestBody EvaluacionDto.Guardar r,
                                           Authentication auth) {
        return service.guardarBorrador(proyectoId, r, auth.getName());
    }

    @PostMapping("/proyecto/{proyectoId}/enviar")
    public EvaluacionDto.Response enviar(@PathVariable Long proyectoId,
                                         @Valid @RequestBody(required = false) EvaluacionDto.Guardar r,
                                         Authentication auth) {
        return service.enviar(proyectoId, r, auth.getName());
    }

    @PostMapping("/{id}/reabrir")
    public EvaluacionDto.Response reabrir(@PathVariable Long id, Authentication auth) {
        return service.reabrir(id, auth.getName());
    }
}