package com.universidad.tareas.controller;

import com.universidad.tareas.model.Prioridad;
import com.universidad.tareas.model.Tarea;
import com.universidad.tareas.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TareaApiController {

    private final TareaService servicio;

    // Mismo TareaService de la Parte 1: al ser un @Service singleton
    // gestionado por Spring, no se crea una segunda instancia ni un
    // servicio paralelo. TareaController (vista) y TareaApiController
    // (API) leen y escriben sobre exactamente los mismos datos en memoria.
    public TareaApiController(TareaService servicio) {
        this.servicio = servicio;
    }

    // GET /api/tareas?prioridad=ALTA&completada=false → 200 OK + JSON filtrado
    @GetMapping
    public ResponseEntity<List<Tarea>> listar(
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Boolean completada) {
        return ResponseEntity.ok(servicio.filtrar(prioridad, completada));
    }

    // GET /api/tareas/{id} → 200 OK + JSON, o 404 Not Found
    @GetMapping("/{id}")
    public ResponseEntity<Tarea> buscar(@PathVariable Long id) {
        return servicio.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tareas → 201 Created + Location + JSON creado, o 400 si falla @Valid
    @PostMapping
    public ResponseEntity<Tarea> crear(@Valid @RequestBody Tarea tarea) {
        tarea.setId(null); // el id lo asigna siempre el servidor; se ignora el que envíe el cliente
        Tarea creada = servicio.guardar(tarea);
        URI ubicacion = URI.create("/api/tareas/" + creada.getId());
        return ResponseEntity.created(ubicacion).body(creada);
    }

    // PUT /api/tareas/{id} → reemplazo completo del recurso: 200 OK, 404 o 400
    @PutMapping("/{id}")
    public ResponseEntity<Tarea> actualizar(@PathVariable Long id, @Valid @RequestBody Tarea tarea) {
        return servicio.buscarPorId(id)
                .map(existente -> {
                    tarea.setId(id);
                    return ResponseEntity.ok(servicio.guardar(tarea));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // PATCH /api/tareas/{id}/completar → actualización PARCIAL (solo "completada"): 200 OK, o 404
    @PatchMapping("/{id}/completar")
    public ResponseEntity<Tarea> completar(@PathVariable Long id) {
        return servicio.marcarCompletada(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/tareas/{id} → 204 No Content, o 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!servicio.eliminar(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
