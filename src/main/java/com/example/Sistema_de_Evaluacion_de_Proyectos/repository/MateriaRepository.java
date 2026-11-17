package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("select distinct m from Materia m where m.docentePrincipal.id = :docenteId " +
            "or m.id in (select a.materia.id from Asignacion a where a.docente.id = :docenteId)")
    List<Materia> findImpartidasPorDocente(@Param("docenteId") Long docenteId);
}
