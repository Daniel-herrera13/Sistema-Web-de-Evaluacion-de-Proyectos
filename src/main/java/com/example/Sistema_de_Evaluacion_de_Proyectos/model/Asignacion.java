package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Una materia dentro de una subsección, con SU docente (puede cambiar por materia y subsección). */
@Entity
@Table(name = "asignaciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subseccion_id", "materia_id"}))
@Getter
@Setter
public class Asignacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subseccion_id")
    private Subseccion subseccion;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "docente_id")
    private Usuario docente;
}
