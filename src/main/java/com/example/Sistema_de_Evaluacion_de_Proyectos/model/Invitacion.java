package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Un evaluador (jurado o docente) invitado a evaluar un proyecto. */
@Entity
@Table(name = "invitaciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "evaluador_id"}))
@Getter @Setter @NoArgsConstructor
public class Invitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluador_id")
    private Usuario evaluador;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invitado_por_id")
    private Usuario invitadoPor;

    @Column(nullable = false)
    private LocalDateTime fechaInvitacion = LocalDateTime.now();
}