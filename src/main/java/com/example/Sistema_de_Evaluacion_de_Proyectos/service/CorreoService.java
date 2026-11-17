package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.util.HtmlUtils;
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
        String nombreSeguro = HtmlUtils.htmlEscape(nombre == null ? "" : nombre);
        String codigoSeguro = HtmlUtils.htmlEscape(codigo);
        String texto = "Hola " + (nombre == null ? "" : nombre) + ",\n\n"
                + "Tu código para restablecer la contraseña es: " + codigo
                + "\n\nEste código vence en 10 minutos. Si no solicitaste este cambio, puedes ignorar este correo.";
        String html = """
                <!doctype html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#f4f1ed;font-family:Arial,Helvetica,sans-serif;color:#292522;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;">Tu código de recuperación vence en 10 minutos.</div>
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f4f1ed;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:560px;background-color:#ffffff;border:1px solid #e8e1db;border-radius:14px;overflow:hidden;">
                        <tr><td style="background-color:#b31b1b;padding:25px 32px;border-bottom:4px solid #e2b33c;">
                          <div style="font-size:12px;font-weight:bold;letter-spacing:2px;color:#ffffff;">ITCA - FEPADE</div>
                          <div style="font-size:12px;color:#f8dfdf;margin-top:7px;">Sistema Web de Evaluación</div>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <div style="font-size:11px;font-weight:bold;letter-spacing:1.8px;color:#b31b1b;text-transform:uppercase;">Seguridad de la cuenta</div>
                          <h1 style="font-size:24px;line-height:1.3;margin:10px 0 14px;color:#292522;">Restablece tu contraseña</h1>
                          <p style="font-size:15px;line-height:1.7;margin:0 0 20px;color:#625b55;">Hola {{nombre}}, recibimos una solicitud para cambiar la contraseña de tu cuenta. Usa este código para continuar:</p>
                          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0 0 22px;">
                            <tr><td align="center" style="background-color:#fbf5f0;border:1px solid #eadbd0;border-radius:10px;padding:22px 12px;">
                              <div style="font-size:11px;font-weight:bold;letter-spacing:1.5px;color:#81766e;text-transform:uppercase;margin-bottom:10px;">Código de verificación</div>
                              <div style="font-family:Arial,Helvetica,sans-serif;font-size:32px;line-height:1.2;font-weight:bold;letter-spacing:9px;color:#b31b1b;">{{codigo}}</div>
                            </td></tr>
                          </table>
                          <p style="font-size:13px;line-height:1.7;margin:0 0 8px;color:#625b55;"><strong>Este código vence en 10 minutos.</strong></p>
                          <p style="font-size:13px;line-height:1.7;margin:0;color:#81766e;">Si no solicitaste restablecer tu contraseña, ignora este correo. Tu contraseña actual seguirá sin cambios.</p>
                        </td></tr>
                        <tr><td style="padding:18px 32px;background-color:#faf9f7;border-top:1px solid #eee8e2;font-size:11px;line-height:1.6;color:#8a817a;">
                          Mensaje automático del Sistema Web de Evaluación.<br>Por seguridad, no compartas este código con nadie.
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.replace("{{nombre}}", nombreSeguro).replace("{{codigo}}", codigoSeguro);
        // En segundo plano: no bloquea la respuesta ni deja adivinar si el correo existe por el tiempo
        CompletableFuture.runAsync(() -> {
            try {
                var mensaje = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setFrom(remitente, "No responder | Sistema Web de Evaluación");
                helper.setTo(para);
                helper.setSubject("Código para restablecer tu contraseña");
                helper.setText(texto, html);
                sender.send(mensaje);
                log.info("[RECUPERACION] Correo enviado a {}", para);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
            }
        });
    }
}
