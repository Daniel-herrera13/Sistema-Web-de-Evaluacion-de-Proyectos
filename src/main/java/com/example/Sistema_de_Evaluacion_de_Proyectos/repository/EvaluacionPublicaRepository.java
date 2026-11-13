package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionPublica;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionPublicaRepository extends JpaRepository<EvaluacionPublica, Long> {
    long countByProyectoId(Long proyectoId);
}