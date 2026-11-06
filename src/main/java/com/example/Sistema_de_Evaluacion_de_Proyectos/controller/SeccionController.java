package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.SeccionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.SeccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secciones")
public class SeccionController {

    private final SeccionService service;

    public SeccionController(SeccionService service) {
        this.service = service;
    }

    // GET /api/secciones            -> todas
    // GET /api/secciones?materiaId=1 -> las de una materia
    @GetMapping
    public List<SeccionDto.Respuesta> listar(@RequestParam(required = false) Long materiaId) {
        return service.listar(materiaId);
    }

    @GetMapping("/{id}")
    public SeccionDto.Respuesta obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<SeccionDto.Respuesta> crear(@Valid @RequestBody SeccionDto.Guardar r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }

    @PutMapping("/{id}")
    public SeccionDto.Respuesta actualizar(@PathVariable Long id, @Valid @RequestBody SeccionDto.Guardar r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}