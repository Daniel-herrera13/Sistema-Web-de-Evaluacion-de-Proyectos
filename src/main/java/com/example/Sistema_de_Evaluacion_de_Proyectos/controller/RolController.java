package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.RolDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {
    private final RolRepository roles;

    @GetMapping
    public List<RolDto> listar() {
        return roles.findAll().stream().map(rol -> new RolDto(rol.getId(), rol.getNombre())).toList();
    }
}
