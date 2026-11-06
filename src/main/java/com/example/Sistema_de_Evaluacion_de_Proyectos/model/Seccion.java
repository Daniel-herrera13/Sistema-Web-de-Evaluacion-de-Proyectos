package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "secciones")
@Getter @Setter @NoArgsConstructor
public class Seccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @Column(nullable = false, length = 20)
    private String ciclo;
}