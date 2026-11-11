package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EmpresaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.EmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {
    private final EmpresaService service;

    @GetMapping
    public List<EmpresaDto.Response> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public EmpresaDto.Response obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EmpresaDto.Response crear(@Valid @RequestBody EmpresaDto.Request r) { return service.crear(r); }

    @PutMapping("/{id}")
    public EmpresaDto.Response actualizar(@PathVariable Long id, @Valid @RequestBody EmpresaDto.Request r) {
        return service.actualizar(id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}