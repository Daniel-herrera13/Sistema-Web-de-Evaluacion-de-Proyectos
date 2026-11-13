package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones_alumno",
        uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "evaluador_id", "evaluado_id"}))
@Getter @Setter @NoArgsConstructor
public class EvaluacionAlumno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    // Se guarda solo para evitar duplicados; ninguna respuesta de la API lo expone
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private Usuario evaluador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluado_id", nullable = false)
    private Usuario evaluado;

    @UpdateTimestamp
    private LocalDateTime fecha;
}