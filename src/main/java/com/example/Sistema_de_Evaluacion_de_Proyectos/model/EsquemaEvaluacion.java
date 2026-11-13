package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "esquemas_evaluacion")
@Getter @Setter @NoArgsConstructor
public class EsquemaEvaluacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false, unique = true)
    private Materia materia;

    @Column(name = "peso_docente_jurado", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoDocenteJurado = new BigDecimal("100");

    @Column(name = "peso_autoevaluacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoAutoevaluacion = BigDecimal.ZERO;

    @Column(name = "peso_publico", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoPublico = BigDecimal.ZERO;
}