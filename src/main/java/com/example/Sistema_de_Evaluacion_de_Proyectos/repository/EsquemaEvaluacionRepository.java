package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EsquemaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EsquemaEvaluacionRepository extends JpaRepository<EsquemaEvaluacion, Long> {
    Optional<EsquemaEvaluacion> findByMateriaIdAndCiclo(Long materiaId, String ciclo);
    Optional<EsquemaEvaluacion> findByMateriaIdAndCicloIsNull(Long materiaId);
    List<EsquemaEvaluacion> findAllByMateriaId(Long materiaId);

    // Ciclos de esa materia que ya tienen evaluaciones (para bloquear cambios que alterarían notas)
    @Query("select distinct s.ciclo from Evaluacion e join e.proyecto p join p.grupo g join g.asignacion a "
            + "join a.subseccion ss join ss.seccion s where a.materia.id = :materiaId")
    List<String> ciclosConEvaluacionDocente(@Param("materiaId") Long materiaId);

    @Query("select distinct s.ciclo from EvaluacionAlumno e join e.proyecto p join p.grupo g join g.asignacion a "
            + "join a.subseccion ss join ss.seccion s where a.materia.id = :materiaId")
    List<String> ciclosConEvaluacionAlumno(@Param("materiaId") Long materiaId);

    @Query("select distinct s.ciclo from EvaluacionPublica e join e.proyecto p join p.grupo g join g.asignacion a "
            + "join a.subseccion ss join ss.seccion s where a.materia.id = :materiaId")
    List<String> ciclosConEvaluacionPublica(@Param("materiaId") Long materiaId);
}