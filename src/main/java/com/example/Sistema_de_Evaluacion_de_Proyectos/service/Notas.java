package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;

import java.math.BigDecimal;

public final class Notas {
    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("10");

    private Notas() {}

    public static void validar(BigDecimal nota) {
        if (nota == null || nota.compareTo(MIN) < 0 || nota.compareTo(MAX) > 0)
            throw Http.invalido("La nota debe estar entre 0 y 10");
    }
}