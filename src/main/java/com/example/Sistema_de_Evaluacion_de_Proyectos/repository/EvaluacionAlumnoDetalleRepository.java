package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumnoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluacionAlumnoDetalleRepository extends JpaRepository<EvaluacionAlumnoDetalle, Long> {
    List<EvaluacionAlumnoDetalle> findByEvaluacionId(Long evaluacionId);
    List<EvaluacionAlumnoDetalle> findByEvaluacion_ProyectoId(Long proyectoId);
}