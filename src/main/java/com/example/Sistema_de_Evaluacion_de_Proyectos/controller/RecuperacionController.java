package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.RecuperacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.RecuperacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RecuperacionController {
    private final RecuperacionService service;

    @PostMapping("/recuperar")
    public Map<String, Object> recuperar(@Valid @RequestBody RecuperacionDto.Solicitar r) {
        boolean permitido = service.solicitar(r.email());
        String mensaje = permitido
                ? "El correo está habilitado. Ingresa el código que recibirás."
                : "No encontramos una cuenta activa con ese correo.";
        return Map.of("permitido", permitido, "mensaje", mensaje);
    }

    @PostMapping("/restablecer")
    public Map<String, String> restablecer(@Valid @RequestBody RecuperacionDto.Restablecer r) {
        service.restablecer(r.email(), r.codigo(), r.password());
        return Map.of("mensaje", "Contraseña actualizada. Ya puedes iniciar sesión.");
    }
}
