package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Subseccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubseccionRepository extends JpaRepository<Subseccion, Long> {

    // Ya lo usaba código existente
    List<Subseccion> findBySeccionId(Long seccionId);

    // Nuevos, para la API de subsecciones
    List<Subseccion> findBySeccionIdOrderByLetraAsc(Long seccionId);

    boolean existsBySeccionIdAndLetraIgnoreCase(Long seccionId, String letra);
}