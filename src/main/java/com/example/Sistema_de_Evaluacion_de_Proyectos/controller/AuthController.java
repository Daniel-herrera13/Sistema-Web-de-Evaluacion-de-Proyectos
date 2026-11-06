package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;


import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.LoginRequest;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.LoginResponse;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UsuarioRepository repo;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authManager,
                          UsuarioRepository repo,
                          JwtService jwtService) {
        this.authManager = authManager;
        this.repo = repo;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        Usuario u = repo.findByCorreo(req.email()).orElseThrow();
        String rol = u.getRol().getNombre().toUpperCase();
        String token = jwtService.generarToken(u.getCorreo(), rol);

        return ResponseEntity.ok(new LoginResponse(
                token, u.getId(), u.getNombre() + " " + u.getApellido(), u.getCorreo(), rol));
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(Authentication authentication) {
        Usuario u = repo.findByCorreo(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(new LoginResponse(
                null, u.getId(), u.getNombre() + " " + u.getApellido(), u.getCorreo(),
                u.getRol().getNombre().toUpperCase()));
    }
}