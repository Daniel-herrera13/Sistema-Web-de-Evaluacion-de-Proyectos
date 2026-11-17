package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CicloAcademico;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CicloAcademicoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ciclos")
@RequiredArgsConstructor
public class CicloAcademicoController implements CommandLineRunner {
    private static final Pattern FORMATO = Pattern.compile("\\d{2}-\\d{4}");
    private final CicloAcademicoRepository ciclos;
    private final SeccionRepository secciones;

    @GetMapping
    public List<CicloResponse> listar(@RequestParam(defaultValue = "false") boolean activos) {
        var lista = activos ? ciclos.findByActivoTrueOrderByCodigoDesc() : ciclos.findAllByOrderByCodigoDesc();
        return lista.stream().map(c -> new CicloResponse(c.getId(), c.getCodigo(), c.isActivo())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CicloResponse crear(@RequestBody CicloRequest request) {
        String codigo = normalizar(request.codigo());
        if (ciclos.existsByCodigoIgnoreCase(codigo))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese ciclo ya está registrado");
        CicloAcademico ciclo = new CicloAcademico();
        ciclo.setCodigo(codigo);
        ciclo.setActivo(request.activo() == null || request.activo());
        ciclo = ciclos.save(ciclo);
        return new CicloResponse(ciclo.getId(), ciclo.getCodigo(), ciclo.isActivo());
    }

    @PutMapping("/{id}")
    public CicloResponse actualizar(@PathVariable Long id, @RequestBody CicloRequest request) {
        CicloAcademico ciclo = ciclos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ciclo no encontrado"));
        String codigo = normalizar(request.codigo());
        ciclos.findByCodigoIgnoreCase(codigo).filter(otro -> !otro.getId().equals(id)).ifPresent(otro -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese ciclo ya está registrado");
        });
        ciclo.setCodigo(codigo);
        if (request.activo() != null) ciclo.setActivo(request.activo());
        ciclo = ciclos.save(ciclo);
        return new CicloResponse(ciclo.getId(), ciclo.getCodigo(), ciclo.isActivo());
    }

    private String normalizar(String valor) {
        String codigo = valor == null ? "" : valor.trim();
        if (!FORMATO.matcher(codigo).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ciclo debe tener el formato NN-AAAA, por ejemplo 01-2027");
        return codigo;
    }

    @Override
    public void run(String... args) {
        // Registra los ciclos ya usados en secciones para que puedan administrarse
        // sin perder compatibilidad con la información que existe en la base.
        secciones.findAll().stream().map(s -> s.getCiclo().trim()).filter(c -> FORMATO.matcher(c).matches())
                .distinct().filter(c -> !ciclos.existsByCodigoIgnoreCase(c)).forEach(c -> {
                    CicloAcademico ciclo = new CicloAcademico();
                    ciclo.setCodigo(c);
                    ciclo.setActivo(true);
                    ciclos.save(ciclo);
                });
    }

    public record CicloRequest(String codigo, Boolean activo) {}
    public record CicloResponse(Long id, String codigo, boolean activo) {}
}
