package com.example.Sistema_de_Evaluacion_de_Proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluadores_invitados",
        uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "evaluador_id"}))
@Getter @Setter @NoArgsConstructor
public class EvaluadorInvitado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluador_id")
    private Usuario evaluador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitado_por_id")
    private Usuario invitadoPor;

    @Column(nullable = false)
    private LocalDateTime fechaInvitacion = LocalDateTime.now();
}