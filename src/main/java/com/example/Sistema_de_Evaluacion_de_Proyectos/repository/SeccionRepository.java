package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findByMateriaId(Long materiaId);
    boolean existsByMateriaIdAndNombreIgnoreCaseAndCicloIgnoreCase(Long materiaId, String nombre, String ciclo);
    boolean existsByMateriaIdAndNombreIgnoreCaseAndCicloIgnoreCaseAndIdNot(Long materiaId, String nombre, String ciclo, Long id);
}