package com.example.Sistema_de_Evaluacion_de_Proyectos.repository;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}