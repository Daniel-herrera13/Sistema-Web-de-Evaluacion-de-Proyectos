package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {
    Optional<Evaluacion> findByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
    List<Evaluacion> findByProyectoId(Long proyectoId);
    List<Evaluacion> findByEvaluadorId(Long evaluadorId);
}