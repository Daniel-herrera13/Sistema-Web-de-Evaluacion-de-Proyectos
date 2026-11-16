package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.AsignacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.AsignacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {
    private final AsignacionService service;

    @GetMapping
    public List<AsignacionDto.Response> listar() {
        return service.listar();
    }

    // GET /api/asignaciones/detalle?seccionId=1
    @GetMapping("/detalle")
    public List<AsignacionDto.Detalle> detalle(@RequestParam Long seccionId) {
        return service.detalle(seccionId);
    }

    @PostMapping
    public ResponseEntity<AsignacionDto.Resultado> crear(@Valid @RequestBody AsignacionDto.Guardar r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}