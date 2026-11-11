package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "evaluacion_detalles",
        uniqueConstraints = @UniqueConstraint(name = "uq_evaluacion_criterio", columnNames = {"evaluacion_id", "criterio_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluacion_id", nullable = false)
    private Evaluacion evaluacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterio_id", nullable = false)
    private Criterio criterio;

    @Column(name = "nota_otorgada", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaOtorgada;
}