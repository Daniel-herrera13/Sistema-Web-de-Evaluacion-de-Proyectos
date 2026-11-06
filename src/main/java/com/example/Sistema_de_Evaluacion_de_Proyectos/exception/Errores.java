package com.example.Sistema_de_Evaluacion_de_Proyectos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Errores {
    private Errores() {}

    public static ResponseStatusException noEncontrado(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
    public static ResponseStatusException conflicto(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }
    public static ResponseStatusException invalido(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}