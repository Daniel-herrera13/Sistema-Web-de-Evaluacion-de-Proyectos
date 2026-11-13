package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluacionAlumno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluacionAlumnoRepository extends JpaRepository<EvaluacionAlumno, Long> {
    List<EvaluacionAlumno> findByProyectoId(Long proyectoId);
    List<EvaluacionAlumno> findByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
    Optional<EvaluacionAlumno> findByProyectoIdAndEvaluadorIdAndEvaluadoId(Long proyectoId, Long evaluadorId, Long evaluadoId);
}