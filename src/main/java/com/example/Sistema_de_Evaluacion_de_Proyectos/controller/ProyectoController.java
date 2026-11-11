package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ProyectoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.ProyectoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
public class ProyectoController {
    private final ProyectoService service;

    @GetMapping
    public List<ProyectoDto.Response> listar(@RequestParam(required = false) Long grupoId,
                                             @RequestParam(required = false) Long empresaId) {
        return service.listar(grupoId, empresaId);
    }

    @GetMapping("/{id}")
    public ProyectoDto.Response obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto.Response crear(@Valid @RequestBody ProyectoDto.Request r) { return service.crear(r); }

    @PutMapping("/{id}")
    public ProyectoDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoDto.Request r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }

    @GetMapping("/{id}/evaluadores")
    public List<ProyectoDto.Evaluador> evaluadores(@PathVariable Long id) { return service.evaluadores(id); }

    @PostMapping("/{id}/evaluadores") @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto.Evaluador invitar(@PathVariable Long id,
                                         @Valid @RequestBody ProyectoDto.Invitar r,
                                         Authentication auth) {
        return service.invitar(id, r.evaluadorId(), auth.getName());
    }

    @DeleteMapping("/{id}/evaluadores/{evaluadorId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long id, @PathVariable Long evaluadorId) {
        service.quitarEvaluador(id, evaluadorId);
    }
}