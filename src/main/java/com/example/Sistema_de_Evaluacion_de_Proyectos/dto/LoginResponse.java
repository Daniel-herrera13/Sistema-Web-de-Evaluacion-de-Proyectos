package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

public record LoginResponse(
        String token,
        Long id,
        String nombre,
        String email,
        String rol) {}