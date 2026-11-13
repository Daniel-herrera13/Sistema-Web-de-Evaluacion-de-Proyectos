package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.MatriculaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MatriculaController {
    private final MatriculaService service;

    @GetMapping("/api/subsecciones")
    public List<MatriculaDto.SubseccionResponse> subsecciones(@RequestParam(required = false) Long seccionId) {
        return service.listarSubsecciones(seccionId);
    }

    @GetMapping("/api/matriculas")
    public List<MatriculaDto.Response> listar(@RequestParam Long subseccionId) {
        return service.listar(subseccionId);
    }

    @GetMapping("/api/matriculas/sin-matricula")
    public List<GrupoDto.Alumno> sinMatricula() { return service.sinMatricula(); }

    @PostMapping("/api/matriculas") @ResponseStatus(HttpStatus.CREATED)
    public List<MatriculaDto.Response> matricular(@Valid @RequestBody MatriculaDto.Matricular r) {
        return service.matricular(r);
    }

    @DeleteMapping("/api/matriculas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desmatricular(@PathVariable Long id) { service.desmatricular(id); }
}
