package com.monitoring.todo.controller;

import com.monitoring.todo.model.Todo;
import com.monitoring.todo.service.TodoService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class TodoController {

    private final TodoService todoService;
    private final MeterRegistry meterRegistry;

    public TodoController(TodoService todoService, MeterRegistry meterRegistry) {
        this.todoService = todoService;
        this.meterRegistry = meterRegistry;
    }

    // ─── GET / ────────────────────────────────────────────────────────────────
    @GetMapping("/api")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "root"})
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "service", "Todo API",
            "version", "1.0.0",
            "status", "running",
            "endpoints", List.of(
                "GET  /",
                "GET  /api/todos",
                "GET  /api/todos/{id}",
                "POST /api/todos",
                "PUT  /api/todos/{id}",
                "PATCH /api/todos/{id}/complete",
                "DELETE /api/todos/{id}",
                "GET  /api/todos/filter/status",
                "GET  /api/todos/filter/priority",
                "GET  /api/stats",
                "GET  /api/slow",
                "GET  /actuator/prometheus"
            )
        ));
    }

    // ─── GET /api/todos ───────────────────────────────────────────────────────
    @GetMapping("/api/todos")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "list_todos"})
    public ResponseEntity<List<Todo>> getAllTodos() {
        return ResponseEntity.ok(todoService.findAll());
    }

    // ─── GET /api/todos/{id} ──────────────────────────────────────────────────
    @GetMapping("/api/todos/{id}")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "get_todo"})
    public ResponseEntity<?> getTodoById(@PathVariable Long id) {
        Optional<Todo> todo = todoService.findById(id);
        if (todo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Todo not found", "id", id));
        }
        return ResponseEntity.ok(todo.get());
    }

    // ─── POST /api/todos ──────────────────────────────────────────────────────
    @PostMapping("/api/todos")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "create_todo"})
    public ResponseEntity<?> createTodo(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Field 'title' is required"));
        }
        Todo todo = todoService.create(title, body.get("description"), body.get("priority"));
        return ResponseEntity.status(HttpStatus.CREATED).body(todo);
    }

    // ─── PUT /api/todos/{id} ──────────────────────────────────────────────────
    @PutMapping("/api/todos/{id}")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "update_todo"})
    public ResponseEntity<?> updateTodo(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Todo> updated = todoService.update(id, body.get("title"), body.get("description"), body.get("priority"));
        if (updated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Todo not found", "id", id));
        }
        return ResponseEntity.ok(updated.get());
    }

    // ─── PATCH /api/todos/{id}/complete ──────────────────────────────────────
    @PatchMapping("/api/todos/{id}/complete")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "complete_todo"})
    public ResponseEntity<?> completeTodo(@PathVariable Long id) {
        Optional<Todo> todo = todoService.complete(id);
        if (todo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Todo not found", "id", id));
        }
        return ResponseEntity.ok(todo.get());
    }

    // ─── DELETE /api/todos/{id} ───────────────────────────────────────────────
    @DeleteMapping("/api/todos/{id}")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "delete_todo"})
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        if (!todoService.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Todo not found", "id", id));
        }
        return ResponseEntity.ok(Map.of("message", "Todo deleted successfully", "id", id));
    }

    // ─── GET /api/todos/filter/status ─────────────────────────────────────────
    @GetMapping("/api/todos/filter/status")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "filter_status"})
    public ResponseEntity<List<Todo>> filterByStatus(@RequestParam(defaultValue = "false") boolean completed) {
        return ResponseEntity.ok(todoService.findByCompleted(completed));
    }

    // ─── GET /api/todos/filter/priority ──────────────────────────────────────
    @GetMapping("/api/todos/filter/priority")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "filter_priority"})
    public ResponseEntity<List<Todo>> filterByPriority(@RequestParam String value) {
        return ResponseEntity.ok(todoService.findByPriority(value));
    }

    // ─── GET /api/stats ───────────────────────────────────────────────────────
    @GetMapping("/api/stats")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "stats"})
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(todoService.getStats());
    }

    // ─── GET /api/slow ────────────────────────────────────────────────────────
    @GetMapping("/api/slow")
    @Timed(value = "http_request_duration_seconds", extraTags = {"endpoint", "slow"})
    public ResponseEntity<Map<String, Object>> slowEndpoint() throws InterruptedException {
        long delay = 2000 + (long)(Math.random() * 1000);
        Thread.sleep(delay);
        return ResponseEntity.ok(Map.of(
            "message", "Slow processing completed",
            "processingTimeMs", delay
        ));
    }
}
