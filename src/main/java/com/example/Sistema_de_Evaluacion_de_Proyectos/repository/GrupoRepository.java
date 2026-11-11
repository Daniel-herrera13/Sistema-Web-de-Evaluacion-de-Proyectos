package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findBySeccionId(Long seccionId);
    boolean existsBySeccionIdAndNombreIgnoreCase(Long seccionId, String nombre);
    boolean existsBySeccionIdAndNombreIgnoreCaseAndIdNot(Long seccionId, String nombre, Long id);
}