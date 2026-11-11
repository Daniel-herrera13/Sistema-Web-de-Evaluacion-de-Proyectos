package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluacionDetalleRepository extends JpaRepository<EvaluacionDetalle, Long> {
    List<EvaluacionDetalle> findByEvaluacionId(Long evaluacionId);
}