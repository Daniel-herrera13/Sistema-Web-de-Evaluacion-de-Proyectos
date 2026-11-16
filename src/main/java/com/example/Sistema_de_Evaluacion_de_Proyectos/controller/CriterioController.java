package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.CriterioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.CriterioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/criterios")
@RequiredArgsConstructor
public class CriterioController {
    private final CriterioService service;

    @GetMapping
    public List<CriterioDto.Response> listar(@RequestParam(required = false) Long materiaId,
                                             @RequestParam(required = false) Long asignacionId,
                                             @RequestParam(required = false) Long grupoId,
                                             @RequestParam(required = false) Long proyectoId) {
        return service.listar(materiaId, asignacionId, grupoId, proyectoId);
    }

    @GetMapping("/efectivos")
    public CriterioDto.Efectivos efectivos(@RequestParam(required = false) Long proyectoId,
                                           @RequestParam(required = false) Long grupoId,
                                           @RequestParam(required = false) Long asignacionId,
                                           @RequestParam(required = false) Long materiaId) {
        return service.efectivos(proyectoId, grupoId, asignacionId, materiaId);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CriterioDto.Response crear(@Valid @RequestBody CriterioDto.Request r) { return service.crear(r); }

    @PutMapping("/conjunto")
    public List<CriterioDto.Response> conjunto(@Valid @RequestBody CriterioDto.Conjunto r) {
        return service.reemplazarConjunto(r);
    }

    @PutMapping("/{id}")
    public CriterioDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody CriterioDto.Request r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}