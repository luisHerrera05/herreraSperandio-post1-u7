package com.universidad.tareas;

import com.universidad.tareas.model.Prioridad;
import com.universidad.tareas.model.Tarea;
import com.universidad.tareas.service.TareaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración: verifican que la vista Thymeleaf y la API REST
 * comparten el mismo TareaService y que los códigos HTTP son los esperados.
 * (El contexto de Spring es único, así que cada prueba usa títulos propios
 * y no asume el número total de tareas.)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CapasCompartidasTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TareaService servicio;

    private String json(String titulo) {
        return """
            {"titulo":"%s","descripcion":"prueba","prioridad":"MEDIA","fechaLimite":"%s"}
            """.formatted(titulo, LocalDate.now().plusDays(5));
    }

    @Test
    void tareaCreadaPorApiApareceEnLaVistaWebYSeVaAlEliminarla() throws Exception {
        String resultado = mvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Tarea compartida XYZ")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.completada").value(false))
                .andReturn().getResponse().getContentAsString();

        // La vista Thymeleaf (otro controlador) ve la tarea creada por la API
        mvc.perform(get("/tareas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tarea compartida XYZ")));

        Long id = Long.valueOf(resultado.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        mvc.perform(delete("/api/tareas/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tareas/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/tareas"))
                .andExpect(content().string(not(containsString("Tarea compartida XYZ"))));
    }

    @Test
    void apiDevuelve400ConJsonPorCampoCuandoFallaLaValidacion() throws Exception {
        mvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.titulo", containsString("El título es obligatorio")));
    }

    @Test
    void apiDevuelve400ConPrioridadFueraDelEnum() throws Exception {
        mvc.perform(post("/api/tareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Titulo valido").replace("MEDIA", "URGENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void patchCompletaLaTareaYIdInexistenteDa404() throws Exception {
        mvc.perform(patch("/api/tareas/1/completar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completada").value(true));
        mvc.perform(patch("/api/tareas/99999/completar")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/tareas/99999")).andExpect(status().isNotFound());
    }

    @Test
    void formularioConTituloVacioVuelveAlFormularioConErrores() throws Exception {
        mvc.perform(post("/tareas/guardar")
                        .param("titulo", "")
                        .param("prioridad", "ALTA")
                        .param("fechaLimite", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("tareas/formulario"))
                .andExpect(model().attributeHasFieldErrors("tarea", "titulo"));
    }

    @Test
    void formularioValidoRedirigeConPostRedirectGet() throws Exception {
        mvc.perform(post("/tareas/guardar")
                        .param("titulo", "Creada desde formulario")
                        .param("prioridad", "BAJA")
                        .param("fechaLimite", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tareas"));
    }

    @Test
    void editarUnaTareaCompletadaNoLaDevuelveAPendiente() throws Exception {
        Tarea t = servicio.guardar(new Tarea(null, "Ya completada", "x",
                Prioridad.ALTA, LocalDate.now().plusDays(1), true));
        mvc.perform(post("/tareas/guardar")
                        .param("id", t.getId().toString())
                        .param("titulo", "Ya completada (editada)")
                        .param("prioridad", "ALTA")
                        .param("completada", "true")
                        .param("fechaLimite", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().is3xxRedirection());
        assertTrue(servicio.buscarPorId(t.getId()).orElseThrow().isCompletada());
    }

    @Test
    void servicioFiltraDeFormaCombinable() {
        Tarea a = servicio.guardar(new Tarea(null, "Filtro A", "", Prioridad.ALTA, LocalDate.now(), false));
        Tarea b = servicio.guardar(new Tarea(null, "Filtro B", "", Prioridad.ALTA, LocalDate.now(), true));
        List<Tarea> altaPendiente = servicio.filtrar(Prioridad.ALTA, false);
        assertTrue(altaPendiente.contains(a));
        assertTrue(altaPendiente.stream().noneMatch(t -> t.getId().equals(b.getId())));
        assertEquals(servicio.obtenerTodas().size(), servicio.filtrar(null, null).size());
    }
}
