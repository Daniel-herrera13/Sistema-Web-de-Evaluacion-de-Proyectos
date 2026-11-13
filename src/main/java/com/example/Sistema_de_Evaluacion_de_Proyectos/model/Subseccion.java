package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Subsección de una sección: SOFT41 + "A" => SOFT41A. */
@Entity
@Table(name = "subsecciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seccion_id", "letra"}))
@Getter
@Setter
public class Subseccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String letra;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion;
}