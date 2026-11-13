package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterioEsquemaRepository extends JpaRepository<CriterioEsquema, Long> {
    List<CriterioEsquema> findByEsquemaId(Long esquemaId);
    List<CriterioEsquema> findByEsquemaIdAndModalidad(Long esquemaId, CriterioEsquema.Modalidad modalidad);
}