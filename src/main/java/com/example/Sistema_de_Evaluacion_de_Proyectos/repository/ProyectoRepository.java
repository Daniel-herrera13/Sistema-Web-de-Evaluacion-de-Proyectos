package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    Optional<Proyecto> findByGrupoId(Long grupoId);
    List<Proyecto> findBySeccionId(Long seccionId);
    List<Proyecto> findByMateriaId(Long materiaId);
}