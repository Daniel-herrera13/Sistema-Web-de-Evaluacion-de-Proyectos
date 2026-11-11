package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "grupos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @ManyToMany
    @JoinTable(
            name = "grupo_alumnos",
            joinColumns = @JoinColumn(name = "grupo_id"),
            inverseJoinColumns = @JoinColumn(name = "alumno_id")
    )
    private Set<Usuario> alumnos = new HashSet<>();
}