package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    List<Asignacion> findBySubseccionId(Long subseccionId);
    List<Asignacion> findByDocenteId(Long docenteId);
    boolean existsBySubseccionIdAndMateriaId(Long subseccionId, Long materiaId);
    List<Asignacion> findBySubseccionSeccionId(Long seccionId);
    boolean existsByMateriaIdAndDocenteId(Long materiaId, Long docenteId);

}
