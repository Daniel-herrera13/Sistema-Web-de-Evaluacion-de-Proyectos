package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.InvitacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.InvitacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitaciones")
@RequiredArgsConstructor
public class InvitacionController {
    private final InvitacionService service;

    @GetMapping
    public List<InvitacionDto.Response> listar(@RequestParam Long proyectoId, Authentication auth) {
        return service.listar(proyectoId, auth.getName());
    }

    @GetMapping("/mias")
    public List<InvitacionDto.Asignado> mias(Authentication auth) {
        return service.misProyectos(auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitacionDto.Response invitar(@Valid @RequestBody InvitacionDto.Crear r, Authentication auth) {
        return service.invitar(r, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable Long id, Authentication auth) {
        service.quitar(id, auth.getName());
    }
}