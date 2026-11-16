package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import com.example.Sistema_de_Evaluacion_de_Proyectos.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.origin}")
    private String corsOrigin;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/publico/**").permitAll()
                        .requestMatchers("/api/esquemas/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/autoevaluacion/**").hasRole("ALUMNO")
                        .requestMatchers("/api/notas/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/auth/login", "/api/auth/recuperar", "/api/auth/restablecer", "/error").permitAll()                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("ADMIN", "DOCENTE", "JURADO")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/materias/**", "/api/secciones/**", "/api/asignaciones/**", "/api/grupos/**",
                                "/api/empresas/**", "/api/criterios/**", "/api/roles/**").authenticated()
                        .requestMatchers("/api/matriculas/**", "/api/subsecciones/**", "/api/asignaciones/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/alumnos/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/materias/**", "/api/secciones/**", "/api/grupos/**",
                                "/api/empresas/**", "/api/criterios/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(HttpMethod.POST, "/api/proyectos").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/proyectos/*").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/proyectos/*").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/invitaciones/mias").hasAnyRole("DOCENTE", "JURADO")
                        .requestMatchers("/api/invitaciones/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/evaluaciones/**").hasAnyRole("ADMIN", "DOCENTE", "JURADO")
                        .requestMatchers("/api/reportes/estadisticas").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers("/api/reportes/**").hasAnyRole("ADMIN", "DOCENTE", "JURADO")
                        .requestMatchers("/api/resultados/**").hasRole("ALUMNO")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(corsOrigin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}