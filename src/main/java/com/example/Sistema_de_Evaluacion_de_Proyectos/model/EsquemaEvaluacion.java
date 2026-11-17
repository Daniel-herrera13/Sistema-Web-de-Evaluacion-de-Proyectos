package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Esquema de modalidades de una materia. Uno por (materia, ciclo); ciclo null = esquema general. */
@Entity
@Table(name = "esquemas_evaluacion",
        uniqueConstraints = @UniqueConstraint(name = "uq_esquema_materia_ciclo", columnNames = {"materia_id", "ciclo"}))
@Getter @Setter @NoArgsConstructor
public class EsquemaEvaluacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    /** Formato NN-AAAA. null = general (se usa en los ciclos que no tengan esquema propio). */
    @Column(length = 20)
    private String ciclo;

    @Column(name = "peso_docente_jurado", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoDocenteJurado = new BigDecimal("100");

    @Column(name = "peso_autoevaluacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoAutoevaluacion = BigDecimal.ZERO;

    @Column(name = "peso_publico", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoPublico = BigDecimal.ZERO;
}