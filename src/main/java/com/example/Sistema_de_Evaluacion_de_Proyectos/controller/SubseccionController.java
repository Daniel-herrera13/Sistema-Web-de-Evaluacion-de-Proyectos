package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.SubseccionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.SubseccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subsecciones")
public class SubseccionController {

    private final SubseccionService service;

    public SubseccionController(SubseccionService service) {
        this.service = service;
    }

    // GET /api/subsecciones                -> todas
    // GET /api/subsecciones/detalle?seccionId=1  -> las de una sección (con id, letra y nombre)
    @GetMapping("/detalle")
    public List<SubseccionDto.Respuesta> listar(@RequestParam(required = false) Long seccionId) {
        return service.listar(seccionId);
    }

    @PostMapping
    public ResponseEntity<SubseccionDto.Respuesta> crear(@Valid @RequestBody SubseccionDto.Guardar r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}