package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciclos_academicos", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@Getter @Setter @NoArgsConstructor
public class CicloAcademico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false)
    private boolean activo = true;
}
