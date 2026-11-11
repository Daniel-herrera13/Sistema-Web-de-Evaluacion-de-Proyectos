package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluadorInvitadoRepository extends JpaRepository<EvaluadorInvitado, Long> {
    List<EvaluadorInvitado> findByProyectoId(Long proyectoId);
    Optional<EvaluadorInvitado> findByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
    boolean existsByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
}