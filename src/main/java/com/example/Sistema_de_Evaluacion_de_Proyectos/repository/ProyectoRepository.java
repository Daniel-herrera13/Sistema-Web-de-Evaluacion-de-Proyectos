package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    List<Proyecto> findByGrupoId(Long grupoId);
    List<Proyecto> findByEmpresaId(Long empresaId);
    boolean existsByGrupoId(Long grupoId);
    boolean existsByEmpresaId(Long empresaId);
}