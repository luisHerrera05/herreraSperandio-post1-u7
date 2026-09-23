package com.universidad.tareas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Traduce los errores de la API a JSON estructurado. Se limita a TareaApiController:
 * los errores de validación del formulario Thymeleaf se resuelven aparte, con
 * BindingResult dentro de TareaController.guardar(), porque ese flujo debe volver
 * a renderizar una vista HTML con los mensajes junto a cada campo, no devolver JSON.
 */
@RestControllerAdvice(assignableTypes = TareaApiController.class)
public class ApiErrorHandler {

    // 400: falla @Valid sobre @RequestBody → { "campo": "mensaje" }
    // Un mismo campo puede violar varias reglas (un título vacío incumple @NotBlank y @Size(min=3));
    // los mensajes se ordenan y se unen con "; " para que la respuesta sea determinista.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, TreeSet<String>> porCampo = new TreeMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                porCampo.computeIfAbsent(error.getField(), k -> new TreeSet<>())
                        .add(error.getDefaultMessage()));
        Map<String, String> errores = new TreeMap<>();
        porCampo.forEach((campo, mensajes) -> errores.put(campo, String.join("; ", mensajes)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    // 400: JSON mal formado, prioridad fuera del enum o fecha con formato incorrecto
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Cuerpo JSON inválido: revise la sintaxis, que prioridad sea ALTA, MEDIA o BAJA "
                        + "y que fechaLimite tenga formato yyyy-MM-dd"));
    }
}
