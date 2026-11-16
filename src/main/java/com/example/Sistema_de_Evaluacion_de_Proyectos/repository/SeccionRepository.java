package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    boolean existsByNombreIgnoreCaseAndCicloIgnoreCase(String nombre, String ciclo);

    boolean existsByNombreIgnoreCaseAndCicloIgnoreCaseAndIdNot(String nombre, String ciclo, Long id);

    /** Secciones donde la materia tiene alguna asignación (teoría y práctica salen de las subsecciones). */
    @Query("select distinct a.subseccion.seccion from Asignacion a where a.materia.id = :materiaId")
    List<Seccion> findByMateriaAsignada(@Param("materiaId") Long materiaId);

    /** Nombres de las materias asignadas a una sección. */
    @Query("select distinct a.materia.nombre from Asignacion a "
            + "where a.subseccion.seccion.id = :seccionId order by a.materia.nombre")
    List<String> nombresDeMaterias(@Param("seccionId") Long seccionId);

    default List<Seccion> findByMateriaId(Long materiaId) {
        return findByMateriaAsignada(materiaId);
    }
}