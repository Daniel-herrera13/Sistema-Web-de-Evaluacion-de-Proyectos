package com.example.Sistema_de_Evaluacion_de_Proyectos.controller;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.ExportService;
import com.example.Sistema_de_Evaluacion_de_Proyectos.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService service;
    private final ExportService export;

    @GetMapping("/proyectos")
    public List<ReporteDto.Fila> listar(ReporteDto.Filtros filtros, Authentication auth) {
        return service.listar(filtros, auth.getName());
    }

    @GetMapping("/proyectos/{id}")
    public ResultadoDto.Resumen detalle(@PathVariable Long id, Authentication auth) {
        return service.detalle(id, auth.getName());
    }

    @GetMapping("/proyectos/exportar")
    public ResponseEntity<byte[]> exportarListado(@RequestParam String formato,
                                                  ReporteDto.Filtros filtros, Authentication auth) {
        return descarga(formato, service.tablaListado(filtros, auth.getName()), "reporte-proyectos");
    }

    @GetMapping("/proyectos/{id}/exportar")
    public ResponseEntity<byte[]> exportarDetalle(@PathVariable Long id, @RequestParam String formato,
                                                  Authentication auth) {
        return descarga(formato, service.tablaDetalle(id, auth.getName()), "proyecto-" + id);
    }

    private ResponseEntity<byte[]> descarga(String formato, ReporteDto.Tabla tabla, String base) {
        String f = formato == null ? "" : formato.toLowerCase();
        byte[] datos;
        MediaType tipo;
        String ext;
        if (f.equals("pdf")) {
            datos = export.pdf(tabla);
            tipo = MediaType.APPLICATION_PDF;
            ext = "pdf";
        } else if (f.equals("xlsx") || f.equals("excel")) {
            datos = export.excel(tabla);
            tipo = XLSX;
            ext = "xlsx";
        } else {
            throw Http.invalido("Formato no soportado. Use pdf o xlsx");
        }
        String nombre = base + "-" + LocalDate.now() + "." + ext;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(tipo)
                .body(datos);
    }
}