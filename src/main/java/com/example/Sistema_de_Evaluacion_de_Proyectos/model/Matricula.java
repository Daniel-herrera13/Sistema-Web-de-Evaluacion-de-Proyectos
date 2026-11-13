package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Un alumno solo puede estar matriculado en UNA subsección (unique en alumno_id). */
@Entity
@Table(name = "matriculas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"alumno_id"}))
@Getter
@Setter
public class Matricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "alumno_id")
    private Usuario alumno;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subseccion_id")
    private Subseccion subseccion;
}