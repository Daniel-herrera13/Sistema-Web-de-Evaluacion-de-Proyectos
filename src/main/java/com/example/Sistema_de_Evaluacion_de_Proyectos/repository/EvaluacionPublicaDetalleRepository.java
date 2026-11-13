package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionPublicaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluacionPublicaDetalleRepository extends JpaRepository<EvaluacionPublicaDetalle, Long> {
    List<EvaluacionPublicaDetalle> findByEvaluacion_ProyectoId(Long proyectoId);
}