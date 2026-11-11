package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "evaluaciones",
        uniqueConstraints = @UniqueConstraint(name = "uq_evaluador_proyecto", columnNames = {"proyecto_id", "evaluador_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private Usuario evaluador;

    @Column(name = "calificacion_final", precision = 5, scale = 2)
    private BigDecimal calificacionFinal = BigDecimal.ZERO;

    @Column(length = 20)
    private String estado = "BORRADOR";

    @Column(columnDefinition = "TEXT")
    private String comentarios;

    @UpdateTimestamp
    @Column(name = "fecha")
    private LocalDateTime fecha;
}