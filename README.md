Post-contenido — Unidad 8: Patrones Arquitectónicos II

Autor: Abel Stiven Ayala Llanes
Curso: Patrones de Diseño de Software — UFPS

Descripción

Sistema de seguimiento de hallazgos de auditoría interna, implementado en un único proyecto Spring Boot (auditoria-hallazgos) en dos partes:

Parte 1: implementación con Clean Architecture y sus cuatro círculos concéntricos: Entities (Aggregate Root con máquina de estados y Value Objects), Use Cases (casos de uso con puertos), Interface Adapters (controller y repositorio adapter) y Frameworks & Drivers (Spring Boot + JPA sobre H2).
Parte 2: extensión del mismo sistema con un dashboard agregado para el comité de auditoría y una bitácora de trazabilidad de cada cambio de estado. Antes de implementarla se hizo un análisis costo-beneficio de CQRS y Event Sourcing, que concluyó en una extensión liviana.
Estructura de paquetes
src/main/java/com/example/auditoria/
├── domain/                                    ← Círculo Entities (Java puro)
│   ├── entity/
│   │   └── HallazgoAuditoria                  Aggregate Root
│   └── valueobject/
│       ├── HallazgoId                         identidad tipada
│       ├── Severidad                          enum simple
│       ├── EstadoHallazgo                     enum con máquina de estados
│       ├── PlanRemediacion                    Value Object inmutable
│       └── TransicionInvalidaException
├── usecase/                                   ← Círculo Use Cases (sin Spring)
│   ├── *UseCase                               interfaces de entrada
│   ├── HallazgoNotFoundException
│   ├── port/                                  puertos de salida y vistas de lectura
│   └── impl/                                  implementaciones de los casos de uso
├── adapter/                                   ← Círculo Interface Adapters
│   ├── in/web/                                HallazgoController, GlobalExceptionHandler, dto/
│   └── out/persistence/                       entidades JPA, repositorios y adaptadores de los puertos
├── config/
│   └── AuditoriaConfiguration                 wiring explícito
└── AuditoriaHallazgosApplication              ← Círculo Frameworks & Drivers (Spring Boot + JPA)

La dependencia del código solo apunta hacia adentro: adapter → usecase → domain. Los paquetes domain/ y usecase/ no importan org.springframework ni jakarta.persistence.

Cómo ejecutar

Requisitos: JDK 17 o superior y Maven 3.8+.

bash
mvn clean package
mvn spring-boot:run

La aplicación queda en http://localhost:8080 y la consola H2 en /h2-console (JDBC URL jdbc:h2:mem:auditoria_db, usuario sa, sin contraseña).

Pruebas unitarias del dominio (sin @SpringBootTest):

bash
mvn test

Recorrido completo de la API en PowerShell, con la aplicación en marcha:

powershell
powershell -ExecutionPolicy Bypass -File .\pruebas.ps1

Nota (Windows): si la ruta del proyecto contiene caracteres como la ñ, mvn spring-boot:run puede fallar con ClassNotFoundException. Se soluciona ejecutando el proyecto desde una ruta sin caracteres especiales (por ejemplo C:\dev).

Herramientas utilizadas
Java 17, Spring Boot 3.3.5, Spring Data JPA, H2, Bean Validation
Apache Maven, curl / PowerShell, Git, GitHub
Parte 1 — Clean Architecture (Hallazgos de Auditoría)
Los cuatro círculos
Entities (domain/): HallazgoAuditoria es el Aggregate Root. Su estado solo cambia mediante iniciarRemediacion, cerrar y reabrir, que validan la máquina de estados ABIERTO → EN_REMEDIACION → CERRADO → REABIERTO → EN_REMEDIACION. HallazgoId, PlanRemediacion, Severidad y EstadoHallazgo son Value Objects.
Use Cases (usecase/): un caso de uso por intención de negocio (registrar, iniciar remediación, cerrar, reabrir, consultar). Dependen de puertos, no de implementaciones concretas, y se instancian por constructor.
Interface Adapters (adapter/): HallazgoController traduce HTTP a llamadas a casos de uso; los DTOs viven aquí y no en el dominio. HallazgoRepositoryAdapter traduce entre la entidad JPA y el objeto de dominio.
Frameworks & Drivers: Spring Boot, Spring Data JPA y H2. La entidad JPA vive únicamente en adapter/out/persistence, y AuditoriaConfiguration hace el wiring explícito de los casos de uso.
Endpoints de la Parte 1
Método	Ruta	Descripción	Respuestas
POST	/api/hallazgos	Registrar hallazgo	201, 400
PATCH	/api/hallazgos/{id}/iniciar-remediacion	Definir plan de remediación	200, 400, 404
PATCH	/api/hallazgos/{id}/cerrar	Cerrar hallazgo	200, 400, 404
PATCH	/api/hallazgos/{id}/reabrir	Reabrir hallazgo	200, 400, 404
GET	/api/hallazgos/{id}	Buscar por id	200, 404
GET	/api/hallazgos	Listar todos	200
Decisiones de diseño — Parte 1
1. Severidad como enum simple vs. EstadoHallazgo como enum con máquina de estados

EstadoHallazgo encapsula una regla de negocio real: qué transiciones son válidas. Por eso puedeTransicionarA(...) vive dentro del enum y el agregado lo consulta antes de cambiar de estado. Así la regla tiene un único lugar y no se repite en cada caso de uso.

Severidad es solo una clasificación (CRITICA, ALTA, MEDIA, BAJA): ninguna severidad es "más válida" que otra en un momento dado y no tiene reglas propias. Darle el mismo nivel de comportamiento sería sobre-modelado, con métodos que no respaldan ninguna regla real. Si en el futuro apareciera una regla (por ejemplo, plazos máximos de remediación según la severidad), ese sería el momento de convertirla en un enum con comportamiento.

2. PlanRemediacion como Value Object embebido vs. agregado separado

PlanRemediacion se modela como Value Object dentro del agregado HallazgoAuditoria. El criterio es el límite de consistencia transaccional (Sección 3.3 de la guía): un hallazgo no puede pasar a EN_REMEDIACION sin un plan válido ni cerrarse sin uno ya definido, y esa invariante debe cumplirse siempre dentro de la misma transacción, sin ventanas de inconsistencia.

Si el plan fuera un agregado independiente con su propio repositorio, esa regla cruzaría dos agregados y dos transacciones, y habría momentos en que el hallazgo y su plan no concuerdan. Además, el plan no tiene identidad ni ciclo de vida propios fuera de su hallazgo, por lo que un Value Object inmutable es suficiente.

Capturas — Parte 1
Prueba	Captura
Registrar hallazgo (201)	registrar
Datos inválidos (400)	400 validación
Cerrar sin plan de remediación (400)	400 cerrar
Iniciar remediación (200)	remediación
Cerrar hallazgo (200)	cerrar
Reabrir hallazgo (200)	reabrir
Hallazgo inexistente (404)	404
Listar hallazgos (200)	listar
Parte 2 — Análisis costo-beneficio de CQRS y Event Sourcing

El comité de auditoría planteó dos requisitos: un dashboard consolidado (hallazgos por severidad, por estado y promedio de días de cierre por área) y trazabilidad legal de cada cambio de estado. Ambos son el tipo de necesidad que suele usarse para justificar CQRS (lecturas con forma distinta a las escrituras) y Event Sourcing (reconstruir el historial). Antes de implementar, apliqué los criterios de la Sección 7 de la guía:

Escala y carga. Es un sistema académico con un único desarrollador y sin usuarios concurrentes reales. No existe una diferencia de escala entre lecturas y escrituras que justifique infraestructura separada: escalar las lecturas de forma independiente resolvería un problema que este sistema no tiene.

Complejidad de las consultas. El dashboard pide conteos por severidad, conteos por estado y un promedio de días entre detección y cierre por área. Son agregaciones simples sobre el mismo esquema. No requieren otra base de datos, otro esquema ni un modelo de lectura precalculado.

Consistencia. El comité consulta el dashboard antes de cada reunión mensual, como un reporte bajo demanda. No necesita tiempo real. Con una consulta directa, el dato siempre es consistente con el estado actual, sin la consistencia eventual que traería un modelo de lectura separado y sincronizado.

Naturaleza de la trazabilidad. Cumplimiento necesita reconstruir cronológicamente cada cambio: de qué estado a cuál, cuándo y por qué motivo. Eso es una bitácora ordenada, no la capacidad de reproducir eventos para reconstruir el estado completo del agregado. Una tabla de auditoría adicional que coexiste con el estado actual lo cubre sin convertir el historial en la fuente de verdad.

Señales de sobre-ingeniería (Sección 7.2). No hay un experto de negocio disponible para modelar eventos, el equipo es una sola persona sin experiencia previa con Event Sourcing, y el costo de mantener dos modelos (comandos y consultas) o un event store con replay es desproporcionado frente a un problema que se resuelve con unas pocas consultas y una tabla.

Conclusión: CQRS y Event Sourcing completos no se justifican todavía. Se implementó la extensión liviana: tres métodos de consulta agregada en el mismo HallazgoRepositoryPort (sin repositorio de lectura separado) y una bitácora append-only escrita en la misma transacción que cada transición de estado.

Qué se agregó
HallazgoRepositoryPort se extiende con contarPorSeveridad, contarPorEstado y promedioDiasCierrePorArea. HallazgoJpaRepository los implementa con proyecciones sobre el mismo esquema.
HistorialAuditoriaPort, con su adaptador y su entidad JPA (historial_cambios_estado), registra cada transición exitosa. Los casos de uso de iniciar remediación, cerrar y reabrir invocan registrar(...) justo después de guardar el hallazgo.
Los casos de uso ObtenerDashboardAuditoriaUseCase y ConsultarHistorialUseCase exponen la información a través de dos endpoints nuevos.
Endpoints de la Parte 2
Método	Ruta	Descripción	Respuestas
GET	/api/hallazgos/dashboard	Dashboard consolidado	200
GET	/api/hallazgos/{id}/historial	Historial cronológico de un hallazgo	200, 404
Decisiones de diseño — Parte 2
3. CQRS y Event Sourcing completos vs. extensión liviana del repositorio

Se eligió la extensión liviana, por las razones del análisis anterior: sin usuarios concurrentes, consultas alcanzables con agregaciones simples sobre el mismo esquema, dashboard bajo demanda sin necesidad de tiempo real, y sin experto de negocio ni experiencia previa con eventos.

Lo que se sacrifica: no hay escalado independiente de las lecturas ni modelos de lectura especializados, y cada consulta del dashboard se calcula en el momento. Lo que se gana: un solo modelo, un solo repositorio y un puerto extendido, en lugar de un segundo stack completo con su propia sincronización. Qué haría reconsiderar la decisión: una carga de lectura que degrade las escrituras, varios consumidores con vistas muy distintas del mismo dato, o la necesidad de consultar el estado de un hallazgo en una fecha pasada.

4. Bitácora simple (HistorialCambioEstado) vs. Event Store completo

Un Event Store exigiría que HallazgoAuditoria dejara de persistir su estado actual y se reconstruyera por replay de eventos en cada lectura. Eso es un cambio de fondo sobre un agregado que ya funciona, sin una necesidad real de reproducir estados intermedios ni de alimentar proyecciones futuras desconocidas.

La bitácora exigida por Cumplimiento solo necesita mostrar la secuencia de cambios; no necesita ser la fuente de verdad del estado. Por eso se implementó como una tabla adicional, escrita en la misma transacción que cada transición, que coexiste con HallazgoJpaEntity y nunca se usa para reconstruir el estado. Esto responde a las señales de sobre-ingeniería de la Sección 7.2 de la guía: no hay experto de eventos, no hay experiencia previa con el patrón, y el costo no es proporcional al problema.

Capturas — Parte 2
Prueba	Captura
Dashboard consolidado (200)	dashboard
Historial cronológico de un hallazgo (200)	historial

Notas de implementación y limitaciones
Hidratación del agregado. HallazgoAuditoria.reconstituir(...) restaura el estado, el plan y la fecha de cierre tal como se guardaron, sin reproducir transiciones. Reconstruir llamando a iniciarRemediacion, cerrar o reabrir fallaría para un hallazgo REABIERTO (porque reabrir() solo es válido desde CERRADO) y reescribiría fechaCierre con la fecha actual.
Transacciones sin Spring en los casos de uso. Como usecase/ no puede importar Spring, AuditoriaConfiguration envuelve cada caso de uso en una transacción (TransactionTemplate). Así, guardar el hallazgo y registrar en la bitácora ocurre en una sola transacción: si algo falla, ambos se revierten.
Los casos de uso no devuelven DTOs. Los DTOs pertenecen al círculo de adaptadores. ConsultarHallazgoUseCase devuelve el objeto de dominio y el controller lo convierte en HallazgoResponse, para que usecase/ no dependa de adapter/.
Promedio de días de cierre. Se obtienen las columnas necesarias de los hallazgos cerrados con una proyección cerrada, y el promedio por área se calcula en el adaptador. Es suficiente a esta escala; con grandes volúmenes convendría moverlo a una agregación en SQL.
Inmutabilidad de la bitácora. Es append-only por diseño de la aplicación (solo existe la operación de insertar y las columnas son updatable = false), pero la tabla no está protegida a nivel de base de datos: quien tuviera acceso directo podría alterarla. Una exigencia legal estricta pediría permisos de solo-inserción, triggers o encadenamiento de hashes.
Autor del cambio. La bitácora registra qué cambió, cuándo y por qué motivo, pero no quién lo hizo, porque el sistema no tiene autenticación. Agregarlo requeriría identificar al usuario en cada petición.

Conclusiones

En la Parte 1 aprendí a mantener la regla de dependencia hacia adentro: el dominio y los casos de uso son Java puro, y todo lo que conoce Spring o JPA queda en los círculos exteriores, aunque eso implique resolver detalles como rehidratar el agregado o aplicar la transacción desde la configuración. En la Parte 2, lo que más dificultó la decisión fue que los dos requisitos parecían pedir CQRS y Event Sourcing por su descripción; al aplicar los criterios de la guía a la escala real del laboratorio, resultaron cubiertos por una agregación simple y una tabla de bitácora. Entendí que un patrón se justifica por un problema concreto de escala, consistencia o complejidad, y no por cómo suena el requisito. Reconsideraría la decisión si hubiera una carga de lectura que afectara las escrituras, varios consumidores con vistas distintas del mismo dato, o una exigencia de consultar estados pasados o de inmutabilidad verificable.