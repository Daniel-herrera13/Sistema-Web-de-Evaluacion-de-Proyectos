package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasFiltro;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EstadisticasReporteDTO;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.EstadisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estadísticas para las gráficas del reporte.
 * El acceso por rol se define en SecurityConfig (ADMIN y DOCENTE) y se vuelve a validar en el servicio.
 */
@RestController
@RequestMapping("/api/reportes/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping
    public ResponseEntity<EstadisticasReporteDTO> obtener(
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) Long seccionId,
            @RequestParam(required = false) Long grupoId,
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ciclo,
            @RequestParam(required = false) Boolean publicado,
            @RequestParam(required = false) Long proyectoId,
            Authentication authentication) {

        EstadisticasFiltro filtro = new EstadisticasFiltro(
                materiaId, seccionId, grupoId, empresaId, estado, ciclo, publicado, proyectoId);
        return ResponseEntity.ok(estadisticasService.obtener(filtro, authentication));
    }
}
