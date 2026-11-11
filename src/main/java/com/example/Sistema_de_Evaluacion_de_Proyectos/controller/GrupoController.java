package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.GrupoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.GrupoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
public class GrupoController {
    private final GrupoService service;

    @GetMapping
    public List<GrupoDto.Response> listar(@RequestParam(required = false) Long seccionId) {
        return service.listar(seccionId);
    }

    @GetMapping("/{id}")
    public GrupoDto.Response obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public GrupoDto.Response crear(@Valid @RequestBody GrupoDto.Request r) { return service.crear(r); }

    @PutMapping("/{id}")
    public GrupoDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody GrupoDto.Request r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }

    @GetMapping("/{id}/alumnos")
    public List<GrupoDto.Alumno> alumnos(@PathVariable Long id) { return service.alumnos(id); }

    @PostMapping("/{id}/alumnos") @ResponseStatus(HttpStatus.CREATED)
    public GrupoDto.Alumno agregar(@PathVariable Long id, @Valid @RequestBody GrupoDto.AgregarAlumno r) {
        return service.agregarAlumno(id, r.alumnoId());
    }

    @DeleteMapping("/{id}/alumnos/{alumnoId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long id, @PathVariable Long alumnoId) {
        service.quitarAlumno(id, alumnoId);
    }
}