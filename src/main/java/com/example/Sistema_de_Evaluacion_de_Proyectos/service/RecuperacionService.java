package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CodigoRecuperacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CodigoRecuperacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecuperacionService {
    private static final int MINUTOS_VIGENCIA = 10;
    private static final int MAX_INTENTOS = 5;
    private static final int SEGUNDOS_ENTRE_ENVIOS = 60;

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder;
    private final CorreoService correo;
    private final SecureRandom azar = new SecureRandom();

    @Value("${app.recuperacion.correos-permitidos:}")
    private String correosPermitidos;

    /** Siempre termina igual exista o no el correo, para no revelar qué cuentas existen. */
    @Transactional
    public boolean solicitar(String email) {
        String correoNormalizado = normalizar(email);
        if (!listaPermitida().contains(correoNormalizado)) return false;
        Usuario u = usuarios.findByCorreoIgnoreCase(correoNormalizado)
                .filter(Usuario::isActivo).orElse(null);
        if (u == null) return false;

        var previo = codigos.findFirstByUsuarioIdAndUsadoFalseOrderByIdDesc(u.getId());
        if (previo.isPresent() && previo.get().getCreadoEn()
                .isAfter(LocalDateTime.now().minusSeconds(SEGUNDOS_ENTRE_ENVIOS))) return true;

        codigos.deleteByUsuarioId(u.getId());
        String codigo = String.format("%06d", azar.nextInt(1_000_000));

        CodigoRecuperacion c = new CodigoRecuperacion();
        c.setUsuario(u);
        c.setCodigoHash(encoder.encode(codigo));
        c.setExpiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA));
        codigos.save(c);

        correo.enviarCodigo(u.getCorreo(), u.getNombre(), codigo);
        return true;
    }

    // noRollbackFor: si el código es incorrecto, el contador de intentos debe guardarse igual
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void restablecer(String email, String codigo, String nuevaPassword) {
        String correoNormalizado = normalizar(email);
        Usuario u = usuarios.findByCorreoIgnoreCase(correoNormalizado)
                .filter(Usuario::isActivo)
                .orElseThrow(RecuperacionService::codigoInvalido);
        CodigoRecuperacion c = codigos.findFirstByUsuarioIdAndUsadoFalseOrderByIdDesc(u.getId())
                .orElseThrow(RecuperacionService::codigoInvalido);

        if (c.getExpiraEn().isBefore(LocalDateTime.now()) || c.getIntentos() >= MAX_INTENTOS)
            throw codigoInvalido();

        if (!encoder.matches(codigo, c.getCodigoHash())) {
            c.setIntentos(c.getIntentos() + 1);
            throw codigoInvalido();
        }

        u.setPassword(encoder.encode(nuevaPassword));
        c.setUsado(true);
    }

    private static ResponseStatusException codigoInvalido() {
        return Http.invalido("Código incorrecto o vencido");
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> listaPermitida() {
        return java.util.Arrays.stream(correosPermitidos.split(","))
                .map(this::normalizar)
                .filter(c -> !c.isBlank())
                .collect(Collectors.toSet());
    }
}
