package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Criterio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterioRepository extends JpaRepository<Criterio, Long> {
    List<Criterio> findByMateriaId(Long id);
    List<Criterio> findByAsignacionId(Long asignacionId);
    List<Criterio> findByGrupoId(Long id);
    List<Criterio> findByProyectoId(Long id);
}