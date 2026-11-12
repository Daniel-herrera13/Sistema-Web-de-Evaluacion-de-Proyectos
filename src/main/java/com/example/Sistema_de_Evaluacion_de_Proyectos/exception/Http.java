package com.example.Sistema_de_Evaluacion_de_Proyectos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Http {
    private Http() {}

    public static ResponseStatusException noEncontrado(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }
    public static ResponseStatusException conflicto(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }
    public static ResponseStatusException invalido(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
    public static ResponseStatusException prohibido(String m) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, m);
    }
}