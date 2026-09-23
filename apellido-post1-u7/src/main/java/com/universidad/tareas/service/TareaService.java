package com.universidad.tareas.service;

import com.universidad.tareas.model.Prioridad;
import com.universidad.tareas.model.Tarea;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio en memoria. Es un bean singleton: Spring inyecta LA MISMA instancia
 * en TareaController (vista) y en TareaApiController (API), por eso ambas capas
 * comparten los mismos datos.
 *
 * Al ser un singleton atendido por varios hilos (uno por petición), se usan
 * estructuras seguras para concurrencia: ConcurrentSkipListMap (ordenado por id)
 * y AtomicLong para generar los identificadores.
 */
@Service
public class TareaService {

    private final Map<Long, Tarea> tareas = new ConcurrentSkipListMap<>();
    private final AtomicLong contadorId = new AtomicLong(1);

    public TareaService() {
        // Datos de ejemplo para arrancar
        guardar(new Tarea(null, "Configurar entorno Spring Boot", "Instalar JDK 17, Maven y el IDE",
                Prioridad.ALTA, LocalDate.now().plusDays(1), false));
        guardar(new Tarea(null, "Diseñar el modelo de dominio", "Definir clase Tarea y enum Prioridad",
                Prioridad.MEDIA, LocalDate.now().plusDays(3), false));
        guardar(new Tarea(null, "Escribir pruebas unitarias", "Cubrir TareaService con JUnit",
                Prioridad.BAJA, LocalDate.now().plusDays(7), true));
    }

    public List<Tarea> obtenerTodas() {
        return List.copyOf(tareas.values());
    }

    /** Filtro combinable: si un parámetro es null, no se aplica esa condición. */
    public List<Tarea> filtrar(Prioridad prioridad, Boolean completada) {
        return tareas.values().stream()
                .filter(t -> prioridad == null || t.getPrioridad() == prioridad)
                .filter(t -> completada == null || t.isCompletada() == completada)
                .toList();
    }

    public Optional<Tarea> buscarPorId(Long id) {
        return Optional.ofNullable(tareas.get(id));
    }

    public Tarea guardar(Tarea tarea) {
        if (tarea.getId() == null) {
            tarea.setId(contadorId.getAndIncrement());
        }
        tareas.put(tarea.getId(), tarea);
        return tarea;
    }

    public Optional<Tarea> marcarCompletada(Long id) {
        Tarea tarea = tareas.get(id);
        if (tarea == null) {
            return Optional.empty();
        }
        tarea.setCompletada(true);
        return Optional.of(tarea);
    }

    public boolean eliminar(Long id) {
        return tareas.remove(id) != null;
    }
}
