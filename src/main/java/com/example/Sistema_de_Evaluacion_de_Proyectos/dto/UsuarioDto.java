package com.example.Sistema_de_Evaluacion_de_Proyectos.dto;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDto {
    private UsuarioDto() {}

    public record Crear(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String apellido,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido") @Size(max = 150) String correo,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @NotNull(message = "El rol es obligatorio") Long rolId) {}

    // password es opcional: si no se envía, se conserva la actual
    public record Actualizar(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String apellido,
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido") @Size(max = 150) String correo,
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @NotNull(message = "El rol es obligatorio") Long rolId,
            @NotNull(message = "El estado (activo) es obligatorio") Boolean activo) {}

    public record Respuesta(Long id, String nombre, String apellido, String correo, String rol, boolean activo) {
        public static Respuesta de(Usuario u) {
            return new Respuesta(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(),
                    u.getRol().getNombre(), u.isActivo());
        }
    }
}