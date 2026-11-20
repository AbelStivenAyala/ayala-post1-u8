package com.example.auditoria.adapter.out.persistence;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.PlanRemediacion;
import com.example.auditoria.usecase.port.ConteoCategoria;
import com.example.auditoria.usecase.port.HallazgoRepositoryPort;
import com.example.auditoria.usecase.port.PromedioCategoria;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class HallazgoRepositoryAdapter implements HallazgoRepositoryPort {

    private final HallazgoJpaRepository jpa;

    public HallazgoRepositoryAdapter(HallazgoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(HallazgoAuditoria hallazgo) {
        jpa.save(toEntity(hallazgo));
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorId(HallazgoId id) {
        return jpa.findById(id.toString()).map(this::toDomain);
    }

    @Override
    public List<HallazgoAuditoria> buscarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<ConteoCategoria> contarPorSeveridad() {
        return jpa.contarPorSeveridad().stream()
                .map(p -> new ConteoCategoria(String.valueOf(p.getCategoria()), p.getTotal()))
                .toList();
    }

    @Override
    public List<ConteoCategoria> contarPorEstado() {
        return jpa.contarPorEstado().stream()
                .map(p -> new ConteoCategoria(String.valueOf(p.getCategoria()), p.getTotal()))
                .toList();
    }

    @Override
    public List<PromedioCategoria> promedioDiasCierrePorArea() {
        return jpa.findByEstado(EstadoHallazgo.CERRADO).stream()
                .filter(c -> c.getFechaCierre() != null && c.getFechaDeteccion() != null)
                .collect(Collectors.groupingBy(
                        HallazgoJpaRepository.CierreProjection::getAreaResponsable,
                        TreeMap::new,
                        Collectors.averagingLong(c ->
                                ChronoUnit.DAYS.between(c.getFechaDeteccion(), c.getFechaCierre()))))
                .entrySet().stream()
                .map(e -> new PromedioCategoria(e.getKey(), Math.round(e.getValue() * 100.0) / 100.0))
                .toList();
    }

    private HallazgoAuditoria toDomain(HallazgoJpaEntity e) {
        PlanRemediacion plan = e.getPlanResponsable() != null
                ? new PlanRemediacion(e.getPlanResponsable(), e.getPlanFechaLimite(), e.getPlanNotas())
                : null;
        return HallazgoAuditoria.reconstituir(
                new HallazgoId(java.util.UUID.fromString(e.getId())),
                e.getTitulo(), e.getDescripcion(), e.getAreaResponsable(), e.getSeveridad(),
                e.getFechaDeteccion(), e.getEstado(), plan, e.getFechaCierre());
    }

    private HallazgoJpaEntity toEntity(HallazgoAuditoria h) {
        HallazgoJpaEntity e = new HallazgoJpaEntity();
        e.setId(h.getId().toString());
        e.setTitulo(h.getTitulo());
        e.setDescripcion(h.getDescripcion());
        e.setAreaResponsable(h.getAreaResponsable());
        e.setSeveridad(h.getSeveridad());
        e.setEstado(h.getEstado());
        e.setFechaDeteccion(h.getFechaDeteccion());
        e.setFechaCierre(h.getFechaCierre());
        if (h.getPlanRemediacion() != null) {
            e.setPlanResponsable(h.getPlanRemediacion().responsable());
            e.setPlanFechaLimite(h.getPlanRemediacion().fechaLimite());
            e.setPlanNotas(h.getPlanRemediacion().notas());
        }
        return e;
    }
}
