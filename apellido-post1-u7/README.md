# Post-contenido — Unidad 7: Gestión de Tareas con Spring Boot

**Autor:** [Nombre Apellido] · **Curso:** Programación Web — Séptimo Semestre · **Unidad 7**

## Descripción
Un único proyecto Spring Boot (un solo `pom.xml`, un solo paquete raíz `com.universidad.tareas`) con **dos capas sobre el mismo `TareaService`**:

- **Parte 1 — Vista Thymeleaf** (`@Controller`): listar con filtros por prioridad y estado, crear, editar, completar y eliminar tareas.
- **Parte 2 — API REST** (`@RestController`): expone las mismas tareas como JSON con los verbos GET, POST, PUT, PATCH y DELETE.

`TareaService` es un bean singleton: la vista web y la API leen y escriben exactamente los mismos datos en memoria. Una tarea creada con `curl` aparece en `http://localhost:8080/tareas`, y viceversa.

## Prerrequisitos
- JDK 17 o superior en el `PATH`
- Apache Maven 3.8+ (o el wrapper `mvnw`)
- Navegador web; Postman o `curl` para probar la API
- Git

## Cómo compilar y ejecutar
```bash
git clone https://github.com/[usuario]/apellido-post1-u7.git
cd apellido-post1-u7
mvn spring-boot:run        # o ./mvnw spring-boot:run
mvn test                   # ejecuta las pruebas de integración
```
- Vista web: <http://localhost:8080/tareas>
- API REST: <http://localhost:8080/api/tareas>

## Estructura
```
apellido-post1-u7/
├── pom.xml
├── README.md
├── capturas/                              # evidencias (vista web y API)
└── src/
    ├── main/java/com/universidad/tareas/
    │   ├── TareasApplication.java
    │   ├── model/       Tarea.java, Prioridad.java     (Bean Validation)
    │   ├── service/     TareaService.java              (repositorio en memoria)
    │   └── controller/  TareaController.java           (vista, Parte 1)
    │                    TareaApiController.java        (API, Parte 2)
    │                    ApiErrorHandler.java           (errores JSON, Parte 2)
    ├── main/resources/
    │   ├── application.properties
    │   └── templates/tareas/  lista.html, formulario.html
    └── test/java/com/universidad/tareas/CapasCompartidasTest.java
```

## Endpoints de la API
| Método | URL | Éxito | Error | Descripción |
|--------|-----|-------|-------|-------------|
| GET | `/api/tareas` | 200 OK | — | Lista de tareas; filtros opcionales `?prioridad=` y `?completada=` |
| GET | `/api/tareas/{id}` | 200 OK | 404 | Tarea con el ID indicado |
| POST | `/api/tareas` | 201 Created (+ `Location`) | 400 | Crea una tarea; 400 si falla la validación |
| PUT | `/api/tareas/{id}` | 200 OK | 404 / 400 | Reemplazo **completo** de la tarea |
| PATCH | `/api/tareas/{id}/completar` | 200 OK | 404 | Actualización **parcial**: solo `completada = true` |
| DELETE | `/api/tareas/{id}` | 204 No Content | 404 | Elimina la tarea |

### Rutas de la vista web
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/tareas?prioridad=&completada=` | Lista con filtros combinables |
| GET | `/tareas/nueva`, `/tareas/{id}/editar` | Formularios de creación / edición |
| POST | `/tareas/guardar` | Guarda (valida con `@Valid`); éxito → `redirect:/tareas` (PRG) |
| POST | `/tareas/{id}/completar`, `/tareas/{id}/eliminar` | Cambian estado → siempre POST |

### Ejemplos con curl
```bash
curl "http://localhost:8080/api/tareas?prioridad=ALTA&completada=false"

curl -i -X POST http://localhost:8080/api/tareas -H "Content-Type: application/json" \
  -d '{"titulo":"Documentar la API","descripcion":"Ejemplos de uso","prioridad":"MEDIA","fechaLimite":"2026-12-15"}'

# 400: título vacío
curl -i -X POST http://localhost:8080/api/tareas -H "Content-Type: application/json" \
  -d '{"titulo":"","prioridad":"ALTA","fechaLimite":"2026-12-15"}'

curl -X PATCH http://localhost:8080/api/tareas/4/completar
curl -i -X DELETE http://localhost:8080/api/tareas/4
```
> `fechaLimite` debe ser hoy o una fecha futura (regla `@FutureOrPresent`); ajuste la fecha de los ejemplos si ya pasó.

## Decisiones de diseño
**Parte 1 — Vista Thymeleaf**
- **Inyección por constructor (no `@Autowired` en campo):** hace explícita la dependencia, permite campos `final` y facilita las pruebas unitarias, porque la clase se puede instanciar a mano sin levantar el contexto de Spring.
- **`@FutureOrPresent` en lugar de `@Future`:** una tarea que vence el mismo día en que se crea es válida; `@Future` la rechazaría.
- **POST (no GET) para completar y eliminar:** una petición GET debe ser segura y sin efectos secundarios; con GET, el prefetch del navegador o un rastreador podrían borrar datos.
- **Post/Redirect/Get:** tras cada operación exitosa se responde con `redirect:/tareas`, así F5 no reenvía el formulario ni duplica operaciones. Con errores de validación **no** se redirige, para conservar los datos y mostrar los mensajes.
- **`@DateTimeFormat(iso = DATE)` en `fechaLimite`:** el `<input type="date">` envía y espera `yyyy-MM-dd`; sin la anotación Spring usaría el formato regional del servidor y fallaría el binding y el prellenado al editar.
- **Campo oculto `completada` en el formulario:** el formulario crea un objeto `Tarea` nuevo; sin este campo, editar una tarea completada la devolvería a "Pendiente".
- **404 con `ResponseStatusException`** en lugar de `RuntimeException` (que produciría un 500).

**Parte 2 — API REST**
- **Mismo `TareaService` por inyección de constructor:** no hay servicio ni modelo paralelo; ambas capas comparten datos y reglas de validación (una sola clase `Tarea`).
- **PATCH (no PUT) en `/completar`:** PUT reemplaza el recurso completo; PATCH modifica un solo atributo sin obligar al cliente a reenviar los demás.
- **Códigos HTTP semánticos:** 201 + `Location` al crear, 204 al eliminar, 404 si no existe, 400 si no valida.
- **Dos mecanismos de validación distintos, a propósito:** `BindingResult` en la vista (re-renderiza HTML con errores junto a cada campo) y `@RestControllerAdvice` en la API (JSON por campo). Unificarlos obligaría a una capa a responder en un formato que no le corresponde. El advice se limita a `TareaApiController` con `assignableTypes`.
- **Errores deterministas:** un título vacío incumple `@NotBlank` y `@Size(min=3)` a la vez; el manejador une los mensajes de cada campo, ordenados, en vez de dejar que un `HashMap` elija uno al azar.
- **JSON inválido → 400:** también se maneja `HttpMessageNotReadableException` (prioridad fuera del enum, fecha mal formateada).

**Transversales**
- **Persistencia en memoria** (`ConcurrentSkipListMap` + `AtomicLong`) en lugar de JPA/Hibernate, que se estudia en la Unidad 8. Se usan estructuras concurrentes porque el servicio es un singleton atendido por varios hilos a la vez.
- **Paquetes por capa** (`model`, `service`, `controller`): la lógica de filtrado vive en el servicio, no en los controladores.

## Pruebas
`mvn test` ejecuta `CapasCompartidasTest` (MockMvc): tarea creada por la API visible en la vista web, 201/204/404/400, PATCH, validación del formulario, PRG, edición sin perder el estado `completada` y filtros combinables.

