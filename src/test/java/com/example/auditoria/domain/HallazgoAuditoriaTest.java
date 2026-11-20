package com.example.auditoria.domain;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.PlanRemediacion;
import com.example.auditoria.domain.valueobject.Severidad;
import com.example.auditoria.domain.valueobject.TransicionInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Sin @SpringBootTest: el dominio se prueba como Java puro.
class HallazgoAuditoriaTest {

    private HallazgoAuditoria nuevoHallazgo() {
        return new HallazgoAuditoria(HallazgoId.nuevo(), "Credenciales por defecto", "Servidor QA",
                "Infraestructura", Severidad.ALTA, LocalDate.of(2026, 8, 1));
    }

    private PlanRemediacion plan() {
        return new PlanRemediacion("Equipo de Infraestructura", LocalDate.of(2026, 8, 20), "Rotar credenciales");
    }

    @Test
    void nuevoHallazgoEmpiezaAbierto() {
        assertEquals(EstadoHallazgo.ABIERTO, nuevoHallazgo().getEstado());
    }

    @Test
    void cicloCompletoDeVida() {
        HallazgoAuditoria h = nuevoHallazgo();

        assertEquals(EstadoHallazgo.ABIERTO, h.iniciarRemediacion(plan()));
        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.getEstado());

        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.cerrar());
        assertEquals(EstadoHallazgo.CERRADO, h.getEstado());
        assertNotNull(h.getFechaCierre());

        assertEquals(EstadoHallazgo.CERRADO, h.reabrir());
        assertEquals(EstadoHallazgo.REABIERTO, h.getEstado());
        assertNull(h.getFechaCierre());

        h.iniciarRemediacion(plan());
        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.getEstado());
    }

    @Test
    void noSePuedeCerrarSinPlan() {
        assertThrows(IllegalStateException.class, () -> nuevoHallazgo().cerrar());
    }

    @Test
    void noSePuedeReabrirUnHallazgoAbierto() {
        assertThrows(TransicionInvalidaException.class, () -> nuevoHallazgo().reabrir());
    }

    @Test
    void planNuloNoCambiaElEstado() {
        HallazgoAuditoria h = nuevoHallazgo();
        assertThrows(NullPointerException.class, () -> h.iniciarRemediacion(null));
        assertEquals(EstadoHallazgo.ABIERTO, h.getEstado());
    }

    @Test
    void reconstituirRestauraEstadoReabierto() {
        HallazgoAuditoria h = HallazgoAuditoria.reconstituir(HallazgoId.nuevo(), "T", "D", "Area",
                Severidad.BAJA, LocalDate.of(2026, 1, 1), EstadoHallazgo.REABIERTO, plan(), null);
        assertEquals(EstadoHallazgo.REABIERTO, h.getEstado());
        h.iniciarRemediacion(plan());
        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.getEstado());
    }

    @Test
    void maquinaDeEstadosSoloPermiteLasTransicionesDefinidas() {
        assertTrue(EstadoHallazgo.ABIERTO.puedeTransicionarA(EstadoHallazgo.EN_REMEDIACION));
        assertFalse(EstadoHallazgo.ABIERTO.puedeTransicionarA(EstadoHallazgo.CERRADO));
        assertFalse(EstadoHallazgo.EN_REMEDIACION.puedeTransicionarA(EstadoHallazgo.REABIERTO));
        assertTrue(EstadoHallazgo.CERRADO.puedeTransicionarA(EstadoHallazgo.REABIERTO));
        assertTrue(EstadoHallazgo.REABIERTO.puedeTransicionarA(EstadoHallazgo.EN_REMEDIACION));
    }
}
