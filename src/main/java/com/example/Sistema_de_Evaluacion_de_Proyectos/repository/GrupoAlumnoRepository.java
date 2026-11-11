package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.GrupoAlumno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoAlumnoRepository extends JpaRepository<GrupoAlumno, Long> {
    List<GrupoAlumno> findByGrupoId(Long grupoId);
    List<GrupoAlumno> findBySeccionId(Long seccionId);
    long countByGrupoId(Long grupoId);
    boolean existsBySeccionIdAndAlumnoId(Long seccionId, Long alumnoId);
    Optional<GrupoAlumno> findByGrupoIdAndAlumnoId(Long grupoId, Long alumnoId);
}