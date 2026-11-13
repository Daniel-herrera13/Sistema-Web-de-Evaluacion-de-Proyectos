package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByAsignacionId(Long asignacionId);
    boolean existsByAsignacionIdAndNombreIgnoreCase(Long asignacionId, String nombre);
    boolean existsByAsignacionIdAndNombreIgnoreCaseAndIdNot(Long asignacionId, String nombre, Long id);
}