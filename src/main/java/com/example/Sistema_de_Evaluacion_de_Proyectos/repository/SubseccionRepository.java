package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Subseccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubseccionRepository extends JpaRepository<Subseccion, Long> {
    List<Subseccion> findBySeccionId(Long seccionId);
}
