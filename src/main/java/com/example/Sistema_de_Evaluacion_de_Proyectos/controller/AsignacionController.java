package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.AsignacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class AsignacionController {
    private final AsignacionRepository asignaciones;

    @GetMapping
    @Transactional(readOnly = true)
    public List<AsignacionDto.Response> listar() {
        return asignaciones.findAll().stream()
                .map(a -> new AsignacionDto.Response(
                        a.getId(),
                        a.getMateria().getNombre(),
                        a.getSubseccion().getSeccion().getNombre(),
                        a.getSubseccion().getSeccion().getCiclo(),
                        a.getSubseccion().getLetra(),
                        a.getDocente().getNombre() + " " + a.getDocente().getApellido()))
                .toList();
    }
}
