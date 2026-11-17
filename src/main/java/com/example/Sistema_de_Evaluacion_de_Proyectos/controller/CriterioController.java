package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.CriterioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.CriterioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/criterios")
@RequiredArgsConstructor
public class CriterioController {
    private final CriterioService service;

    /** Sin ciclo: conjunto general del nivel. Con ciclo (NN-AAAA): el de ese ciclo. */
    @GetMapping
    public List<CriterioDto.Response> listar(@RequestParam(required = false) Long materiaId,
                                             @RequestParam(required = false) Long asignacionId,
                                             @RequestParam(required = false) Long grupoId,
                                             @RequestParam(required = false) Long proyectoId,
                                             @RequestParam(required = false) String ciclo) {
        return service.listar(materiaId, asignacionId, grupoId, proyectoId, ciclo);
    }

    /** Conjuntos que existen en un nivel (uno por ciclo). */
    @GetMapping("/conjuntos")
    public List<CriterioDto.ConjuntoResumen> conjuntos(@RequestParam(required = false) Long materiaId,
                                                       @RequestParam(required = false) Long asignacionId,
                                                       @RequestParam(required = false) Long grupoId,
                                                       @RequestParam(required = false) Long proyectoId) {
        return service.conjuntos(materiaId, asignacionId, grupoId, proyectoId);
    }

    @GetMapping("/efectivos")
    public CriterioDto.Efectivos efectivos(@RequestParam(required = false) Long proyectoId,
                                           @RequestParam(required = false) Long grupoId,
                                           @RequestParam(required = false) Long asignacionId,
                                           @RequestParam(required = false) Long materiaId,
                                           @RequestParam(required = false) String ciclo) {
        return service.efectivos(proyectoId, grupoId, asignacionId, materiaId, ciclo);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CriterioDto.Response crear(@Valid @RequestBody CriterioDto.Request r, Authentication auth) {
        return service.crear(r, auth.getName());
    }

    @PutMapping("/conjunto")
    public List<CriterioDto.Response> conjunto(@Valid @RequestBody CriterioDto.Conjunto r, Authentication auth) {
        return service.reemplazarConjunto(r, auth.getName());
    }

    @PostMapping("/duplicar") @ResponseStatus(HttpStatus.CREATED)
    public List<CriterioDto.Response> duplicar(@Valid @RequestBody CriterioDto.Duplicar r, Authentication auth) {
        return service.duplicar(r, auth.getName());
    }

    @PutMapping("/{id}")
    public CriterioDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody CriterioDto.Request r,
                                           Authentication auth) {
        return service.actualizar(id, r, auth.getName());
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, Authentication auth) { service.eliminar(id, auth.getName()); }
}