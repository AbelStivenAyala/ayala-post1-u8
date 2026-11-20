package com.example.auditoria.adapter.in.web.dto;

import com.example.auditoria.domain.entity.HallazgoAuditoria;

import java.time.LocalDate;

public record HallazgoResponse(
        String id,
        String titulo,
        String descripcion,
        String areaResponsable,
        String severidad,
        String estado,
        LocalDate fechaDeteccion,
        LocalDate fechaCierre,
        String planResponsable,
        LocalDate planFechaLimite,
        String planNotas
) {
    public static HallazgoResponse from(HallazgoAuditoria h) {
        var plan = h.getPlanRemediacion();
        return new HallazgoResponse(
                h.getId().toString(),
                h.getTitulo(),
                h.getDescripcion(),
                h.getAreaResponsable(),
                h.getSeveridad().name(),
                h.getEstado().name(),
                h.getFechaDeteccion(),
                h.getFechaCierre(),
                plan != null ? plan.responsable() : null,
                plan != null ? plan.fechaLimite() : null,
                plan != null ? plan.notas() : null);
    }
}
