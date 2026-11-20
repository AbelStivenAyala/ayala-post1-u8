package com.example.auditoria.adapter.out.persistence;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Registro append-only: las columnas se marcan updatable = false y solo se inserta. */
@Entity
@Table(name = "historial_cambios_estado")
public class HistorialCambioEstadoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String hallazgoId;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private EstadoHallazgo estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false, nullable = false)
    private EstadoHallazgo estadoNuevo;

    @Column(updatable = false, length = 1000)
    private String motivo;

    @Column(updatable = false, nullable = false)
    private LocalDateTime fecha;

    public HistorialCambioEstadoJpaEntity() {
    }

    public Long getId() { return id; }
    public String getHallazgoId() { return hallazgoId; }
    public void setHallazgoId(String hallazgoId) { this.hallazgoId = hallazgoId; }
    public EstadoHallazgo getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoHallazgo estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoHallazgo getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoHallazgo estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
