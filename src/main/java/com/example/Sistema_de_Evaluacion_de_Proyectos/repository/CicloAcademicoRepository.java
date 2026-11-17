package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CicloAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CicloAcademicoRepository extends JpaRepository<CicloAcademico, Long> {
    List<CicloAcademico> findAllByOrderByCodigoDesc();
    List<CicloAcademico> findByActivoTrueOrderByCodigoDesc();
    Optional<CicloAcademico> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
}
