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
                                             @RequestParam(required = false) Long empresaId,
                                             Authentication auth) {
        return service.listar(grupoId, empresaId, auth.getName());
    }

    @GetMapping("/{id}")
    public ProyectoDto.Response obtener(@PathVariable Long id, Authentication auth) {
        return service.obtener(id, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProyectoDto.Response crear(@Valid @RequestBody ProyectoDto.Request r) {
        return service.crear(r);
    }

    @PutMapping("/{id}")
    public ProyectoDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody ProyectoDto.Request r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}