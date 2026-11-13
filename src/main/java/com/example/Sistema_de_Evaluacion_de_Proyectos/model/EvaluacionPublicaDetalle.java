package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "evaluacion_publica_detalles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evaluacion_id", "criterio_id"}))
@Getter @Setter @NoArgsConstructor
public class EvaluacionPublicaDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private EvaluacionPublica evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterio_id", nullable = false)
    private CriterioEsquema criterio;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal nota;
}