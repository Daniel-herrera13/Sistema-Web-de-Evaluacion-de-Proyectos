package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ResultadoDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Criterio;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Evaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionDetalle;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionDetalleRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EvaluadorInvitadoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculoNotaService {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ProyectoRepository proyectos;
    private final EvaluadorInvitadoRepository invitados;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionDetalleRepository detalles;

    /** Nota de un evaluador: suma de (nota x ponderación / 100). Escala 0-10. */
    public BigDecimal notaEvaluador(List<EvaluacionDetalle> det) {
        return det.stream()
                .map(d -> d.getNotaOtorgada()
                        .multiply(d.getCriterio().getPonderacion())
                        .divide(CIEN, 6, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal promedio(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }

    /** Resultado del proyecto: nota por evaluador, nota final del grupo y promedio por criterio. */
    @Transactional(readOnly = true)
    public ResultadoDto.Resumen resumen(Long proyectoId) {
        Proyecto p = proyectos.findById(proyectoId)
                .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
        Grupo g = p.getGrupo();
        Seccion s = g.getSeccion();

        List<EvaluadorInvitado> inv = invitados.findByProyectoId(proyectoId);
        Map<Long, Evaluacion> porEvaluador = evaluaciones.findByProyectoId(proyectoId).stream()
                .collect(Collectors.toMap(e -> e.getEvaluador().getId(), e -> e));

        List<ResultadoDto.PorEvaluador> filas = new ArrayList<>();
        List<BigDecimal> notasEnviadas = new ArrayList<>();
        Map<Long, Criterio> criterios = new LinkedHashMap<>();
        Map<Long, List<BigDecimal>> notasPorCriterio = new LinkedHashMap<>();

        for (EvaluadorInvitado i : inv) {
            Usuario u = i.getEvaluador();
            Evaluacion e = porEvaluador.get(u.getId());
            boolean enviada = e != null && "ENVIADA".equals(e.getEstado());
            BigDecimal nota = null;
            if (enviada) {
                List<EvaluacionDetalle> det = detalles.findByEvaluacionId(e.getId());
                nota = notaEvaluador(det);
                notasEnviadas.add(nota);
                for (EvaluacionDetalle d : det) {
                    Long cid = d.getCriterio().getId();
                    criterios.putIfAbsent(cid, d.getCriterio());
                    notasPorCriterio.computeIfAbsent(cid, k -> new ArrayList<>()).add(d.getNotaOtorgada());
                }
            }
            filas.add(new ResultadoDto.PorEvaluador(u.getId(), u.getNombre() + " " + u.getApellido(),
                    u.getRol().getNombre(), e == null ? "SIN_INICIAR" : e.getEstado(), nota,
                    enviada ? e.getComentarios() : null));
        }

        List<ResultadoDto.PorCriterio> porCriterio = notasPorCriterio.entrySet().stream()
                .map(en -> {
                    Criterio c = criterios.get(en.getKey());
                    return new ResultadoDto.PorCriterio(c.getId(), c.getNombre(), c.getPonderacion(),
                            promedio(en.getValue()));
                }).toList();

        return new ResultadoDto.Resumen(
                p.getId(), p.getTitulo(), g.getId(), g.getNombre(),
                s.getId(), s.getNombre(), s.getCiclo(),
                s.getMateria().getId(), s.getMateria().getNombre(),
                p.getEmpresa() == null ? null : p.getEmpresa().getNombre(), p.getEstado(),
                inv.size(), notasEnviadas.size(),
                !inv.isEmpty() && notasEnviadas.size() == inv.size(),
                notasEnviadas.isEmpty() ? null : promedio(notasEnviadas),
                Boolean.TRUE.equals(p.getResultadosPublicados()),
                filas, porCriterio);
    }
}