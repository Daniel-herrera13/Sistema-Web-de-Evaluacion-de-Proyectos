package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.PublicoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.PublicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publico/proyectos")
@RequiredArgsConstructor
public class PublicoController {
    private final PublicoService service;

    @GetMapping("/{codigo}")
    public PublicoDto.Formulario formulario(@PathVariable String codigo) {
        return service.formulario(codigo);
    }

    @PostMapping("/{codigo}/evaluar")
    @ResponseStatus(HttpStatus.CREATED)
    public void evaluar(@PathVariable String codigo, @Valid @RequestBody PublicoDto.Evaluar r) {
        service.evaluar(codigo, r);
    }
}