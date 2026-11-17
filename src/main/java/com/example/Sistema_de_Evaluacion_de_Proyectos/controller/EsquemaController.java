package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.EsquemaService;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.PublicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/esquemas")
@RequiredArgsConstructor
public class EsquemaController {
    private final EsquemaService esquemas;
    private final PublicoService publico;

    @GetMapping("/materias")
    public List<EsquemaDto.MateriaOpcion> materias(Authentication auth) {
        return esquemas.materiasGestionables(auth.getName());
    }

    @GetMapping("/proyectos-publicos")
    public List<EsquemaDto.ProyectoPublico> proyectosPublicos(Authentication auth) {
        return publico.proyectosGestionables(auth.getName());
    }

    /** Sin ciclo: esquema general. Con ciclo (NN-AAAA): el de ese ciclo, o el general marcado como heredado. */
    @GetMapping("/materia/{materiaId}")
    public EsquemaDto.Response obtener(@PathVariable Long materiaId,
                                       @RequestParam(required = false) String ciclo,
                                       Authentication auth) {
        return esquemas.obtener(materiaId, ciclo, auth.getName());
    }

    /** Ciclos de la materia que ya tienen un esquema propio. */
    @GetMapping("/materia/{materiaId}/ciclos")
    public List<String> ciclos(@PathVariable Long materiaId, Authentication auth) {
        return esquemas.ciclosConEsquema(materiaId, auth.getName());
    }

    @PutMapping("/materia/{materiaId}")
    public EsquemaDto.Response guardar(@PathVariable Long materiaId,
                                       @RequestParam(required = false) String ciclo,
                                       @Valid @RequestBody EsquemaDto.Guardar r, Authentication auth) {
        return esquemas.guardar(materiaId, ciclo, r, auth.getName());
    }

    @GetMapping("/proyecto/{proyectoId}/publico")
    public EsquemaDto.PublicoEstado estadoPublico(@PathVariable Long proyectoId, Authentication auth) {
        return publico.estado(proyectoId, auth.getName());
    }

    @PutMapping("/proyecto/{proyectoId}/publico/codigo-validacion")
    public EsquemaDto.PublicoEstado cambiarCodigoValidacion(@PathVariable Long proyectoId,
                                                             @Valid @RequestBody EsquemaDto.CodigoValidacion r,
                                                             Authentication auth) {
        return publico.cambiarCodigoValidacion(proyectoId, r.codigo(), auth.getName());
    }

    @PostMapping("/proyecto/{proyectoId}/publico/abrir")
    public EsquemaDto.PublicoEstado abrir(@PathVariable Long proyectoId, Authentication auth) {
        return publico.abrir(proyectoId, auth.getName());
    }

    @PostMapping("/proyecto/{proyectoId}/publico/cerrar")
    public EsquemaDto.PublicoEstado cerrar(@PathVariable Long proyectoId, Authentication auth) {
        return publico.cerrar(proyectoId, auth.getName());
    }
}
