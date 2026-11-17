package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "criterios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Criterio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ponderacion;

    // Un criterio pertenece a UN solo nivel; los otros tres quedan null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materia_id")
    private Materia materia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id")
    private Asignacion asignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    /**
     * Ciclo al que aplica el conjunto (formato NN-AAAA, p. ej. 02-2026).
     * null = conjunto general: se usa en cualquier ciclo que no tenga uno propio.
     * Un conjunto = todos los criterios con el mismo nivel y el mismo ciclo.
     */
    @Column(length = 20)
    private String ciclo;

    /** Los criterios retirados se conservan para las evaluaciones históricas. */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean activo = true;
}
