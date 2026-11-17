package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmpresaDto {
    public record Request(@NotBlank @Size(max = 150) String nombre,
                          @Size(max = 100) String contacto,
                          @Email @Size(max = 150) String correo,
                          @Pattern(regexp = "(?:\\d{8}|\\d{4}-\\d{4})",
                                  message = "El teléfono debe tener 8 dígitos, por ejemplo 6620-5555")
                          String telefono) {}
    public record Response(Long id, String nombre, String contacto, String correo, String telefono) {}
}
