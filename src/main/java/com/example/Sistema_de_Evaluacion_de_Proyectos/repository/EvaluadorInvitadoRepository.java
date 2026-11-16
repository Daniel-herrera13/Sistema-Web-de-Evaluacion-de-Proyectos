package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EvaluadorInvitado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluadorInvitadoRepository extends JpaRepository<EvaluadorInvitado, Long> {
    List<EvaluadorInvitado> findByProyectoId(Long proyectoId);
    boolean existsByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
    List<EvaluadorInvitado> findByEvaluadorId(Long evaluadorId);   // ← probablemente el único que falte
}