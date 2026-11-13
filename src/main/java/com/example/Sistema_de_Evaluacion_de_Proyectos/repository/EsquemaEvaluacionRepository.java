package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EsquemaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsquemaEvaluacionRepository extends JpaRepository<EsquemaEvaluacion, Long> {
    Optional<EsquemaEvaluacion> findByMateriaId(Long materiaId);
}