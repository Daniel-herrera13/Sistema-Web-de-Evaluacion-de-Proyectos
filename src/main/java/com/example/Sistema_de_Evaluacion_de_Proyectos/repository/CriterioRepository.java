package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Criterio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CriterioRepository extends JpaRepository<Criterio, Long> {
    List<Criterio> findByMateriaId(Long id);
    List<Criterio> findByAsignacionId(Long asignacionId);
    List<Criterio> findByGrupoId(Long id);
    List<Criterio> findByProyectoId(Long id);
    List<Criterio> findByMateriaIdAndActivoTrue(Long id);
    List<Criterio> findByAsignacionIdAndActivoTrue(Long asignacionId);
    List<Criterio> findByGrupoIdAndActivoTrue(Long id);
    List<Criterio> findByProyectoIdAndActivoTrue(Long id);
    List<Criterio> findAllByActivoTrue();

    /** Cuántas notas de docentes/jurado usan estos criterios (si es > 0, el conjunto ya se usó). */
    @Query("select count(d) from EvaluacionDetalle d where d.criterio.id in :ids")
    long contarUsos(@Param("ids") Collection<Long> ids);
}
