package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CorreoService {
    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final ObjectProvider<JavaMailSender> mail;

    @Value("${app.mail.enabled:false}")
    private boolean habilitado;

    @Value("${app.mail.from:no-reply@itca.edu.sv}")
    private String remitente;

    public CorreoService(ObjectProvider<JavaMailSender> mail) {
        this.mail = mail;
    }

    public void enviarCodigo(String para, String nombre, String codigo) {
        // La consola del servidor permite al equipo administrador consultar el código generado.
        // El código sigue almacenándose únicamente como hash en la base de datos.
        log.info("[RECUPERACION] Código generado para {}: {}", para, codigo);

        JavaMailSender sender = habilitado ? mail.getIfAvailable() : null;
        if (sender == null) {
            if (habilitado) {
                log.error("SMTP está habilitado, pero JavaMailSender no está disponible. No se pudo enviar a {}", para);
            } else {
                log.warn("SMTP deshabilitado: no se envió el código por correo a {}", para);
            }
            return;
        }
        String texto = "Hola " + nombre + ",\n\nTu código para restablecer la contraseña es: " + codigo
                + "\nVence en 10 minutos. Si no lo solicitaste, ignora este mensaje.";
        // En segundo plano: no bloquea la respuesta ni deja adivinar si el correo existe por el tiempo
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(remitente);
                m.setTo(para);
                m.setSubject("Código para restablecer tu contraseña");
                m.setText(texto);
                sender.send(m);
                log.info("[RECUPERACION] Correo enviado a {}", para);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
            }
        });
    }
}
