package com.example.auditoria.config;

import com.example.auditoria.usecase.CerrarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHistorialUseCase;
import com.example.auditoria.usecase.IniciarRemediacionUseCase;
import com.example.auditoria.usecase.ObtenerDashboardAuditoriaUseCase;
import com.example.auditoria.usecase.ReabrirHallazgoUseCase;
import com.example.auditoria.usecase.RegistrarHallazgoUseCase;
import com.example.auditoria.usecase.impl.CerrarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHistorialService;
import com.example.auditoria.usecase.impl.IniciarRemediacionService;
import com.example.auditoria.usecase.impl.ObtenerDashboardAuditoriaService;
import com.example.auditoria.usecase.impl.ReabrirHallazgoService;
import com.example.auditoria.usecase.impl.RegistrarHallazgoService;
import com.example.auditoria.usecase.port.HallazgoRepositoryPort;
import com.example.auditoria.usecase.port.HistorialAuditoriaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * Wiring explicito de los casos de uso. Los casos de uso no conocen Spring, asi que la
 * transaccion se aplica aqui, en el circulo exterior: cada ejecucion (guardar el hallazgo
 * + registrar en la bitacora) ocurre dentro de UNA misma transaccion.
 */
@Configuration
public class AuditoriaConfiguration {

    @Bean
    public RegistrarHallazgoUseCase registrarHallazgoUseCase(HallazgoRepositoryPort repo, TransactionTemplate tx) {
        return transaccional(RegistrarHallazgoUseCase.class, new RegistrarHallazgoService(repo), tx);
    }

    @Bean
    public IniciarRemediacionUseCase iniciarRemediacionUseCase(HallazgoRepositoryPort repo,
                                                               HistorialAuditoriaPort historial,
                                                               TransactionTemplate tx) {
        return transaccional(IniciarRemediacionUseCase.class, new IniciarRemediacionService(repo, historial), tx);
    }

    @Bean
    public CerrarHallazgoUseCase cerrarHallazgoUseCase(HallazgoRepositoryPort repo,
                                                       HistorialAuditoriaPort historial,
                                                       TransactionTemplate tx) {
        return transaccional(CerrarHallazgoUseCase.class, new CerrarHallazgoService(repo, historial), tx);
    }

    @Bean
    public ReabrirHallazgoUseCase reabrirHallazgoUseCase(HallazgoRepositoryPort repo,
                                                         HistorialAuditoriaPort historial,
                                                         TransactionTemplate tx) {
        return transaccional(ReabrirHallazgoUseCase.class, new ReabrirHallazgoService(repo, historial), tx);
    }

    @Bean
    public ConsultarHallazgoUseCase consultarHallazgoUseCase(HallazgoRepositoryPort repo, TransactionTemplate tx) {
        return transaccional(ConsultarHallazgoUseCase.class, new ConsultarHallazgoService(repo), tx);
    }

    @Bean
    public ObtenerDashboardAuditoriaUseCase obtenerDashboardAuditoriaUseCase(HallazgoRepositoryPort repo,
                                                                             TransactionTemplate tx) {
        return transaccional(ObtenerDashboardAuditoriaUseCase.class, new ObtenerDashboardAuditoriaService(repo), tx);
    }

    @Bean
    public ConsultarHistorialUseCase consultarHistorialUseCase(HallazgoRepositoryPort repo,
                                                               HistorialAuditoriaPort historial,
                                                               TransactionTemplate tx) {
        return transaccional(ConsultarHistorialUseCase.class, new ConsultarHistorialService(repo, historial), tx);
    }

    /** Decorador generico: ejecuta cada metodo del caso de uso dentro de una transaccion. */
    private <T> T transaccional(Class<T> contrato, T objetivo, TransactionTemplate tx) {
        Object proxy = Proxy.newProxyInstance(
                contrato.getClassLoader(),
                new Class<?>[]{contrato},
                (p, metodo, args) -> tx.execute(status -> {
                    try {
                        return metodo.invoke(objetivo, args);
                    } catch (InvocationTargetException ex) {
                        Throwable causa = ex.getCause();
                        if (causa instanceof RuntimeException re) {
                            throw re; // provoca rollback y conserva la excepcion de negocio
                        }
                        if (causa instanceof Error er) {
                            throw er;
                        }
                        throw new IllegalStateException(causa);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException(ex);
                    }
                }));
        return contrato.cast(proxy);
    }
}
