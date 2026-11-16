package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {
    List<Invitacion> findByProyectoId(Long proyectoId);
    List<Invitacion> findByEvaluadorId(Long evaluadorId);
    boolean existsByProyectoIdAndEvaluadorId(Long proyectoId, Long evaluadorId);
}