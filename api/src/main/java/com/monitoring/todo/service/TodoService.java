package com.monitoring.todo.service;

import com.monitoring.todo.model.Todo;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final Map<Long, Todo> todos = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    // Prometheus metrics
    private final Counter todosCreated;
    private final Counter todosDeleted;
    private final Counter todosCompleted;
    private final Counter todosNotFound;
    private final Gauge activeTodos;
    private final Gauge completedTodos;
    private final MeterRegistry meterRegistry;

    public TodoService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.todosCreated = Counter.builder("todo_created_total")
                .description("Total number of todos created")
                .register(meterRegistry);

        this.todosDeleted = Counter.builder("todo_deleted_total")
                .description("Total number of todos deleted")
                .register(meterRegistry);

        this.todosCompleted = Counter.builder("todo_completed_total")
                .description("Total number of todos marked as completed")
                .register(meterRegistry);

        this.todosNotFound = Counter.builder("todo_not_found_total")
                .description("Total number of not found errors")
                .register(meterRegistry);

        this.activeTodos = Gauge.builder("todo_active_count", todos,
                map -> map.values().stream().filter(t -> !t.isCompleted()).count())
                .description("Current number of active (incomplete) todos")
                .register(meterRegistry);

        this.completedTodos = Gauge.builder("todo_completed_count", todos,
                map -> map.values().stream().filter(Todo::isCompleted).count())
                .description("Current number of completed todos")
                .register(meterRegistry);

        // Seed with sample data
        seedData();
    }

    private void seedData() {
        create("Aprender Prometheus", "Estudiar métricas y PromQL", "HIGH");
        create("Configurar Grafana", "Crear dashboards de monitoreo", "HIGH");
        create("Documentar proyecto", "Escribir README detallado", "MEDIUM");
        create("Hacer pruebas de carga", "Ejecutar script de tráfico sintético", "LOW");
    }

    public List<Todo> findAll() {
        return new ArrayList<>(todos.values());
    }

    public List<Todo> findByCompleted(boolean completed) {
        return todos.values().stream()
                .filter(t -> t.isCompleted() == completed)
                .collect(Collectors.toList());
    }

    public List<Todo> findByPriority(String priority) {
        return todos.values().stream()
                .filter(t -> priority.equalsIgnoreCase(t.getPriority()))
                .collect(Collectors.toList());
    }

    public Optional<Todo> findById(Long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            todosNotFound.increment();
        }
        return Optional.ofNullable(todo);
    }

    public Todo create(String title, String description, String priority) {
        Long id = idCounter.getAndIncrement();
        Todo todo = new Todo(id, title, description, priority != null ? priority.toUpperCase() : "MEDIUM");
        todos.put(id, todo);
        todosCreated.increment();

        // Tag by priority
        meterRegistry.counter("todo_created_by_priority_total", "priority", todo.getPriority()).increment();
        return todo;
    }

    public Optional<Todo> update(Long id, String title, String description, String priority) {
        Todo todo = todos.get(id);
        if (todo == null) {
            todosNotFound.increment();
            return Optional.empty();
        }
        if (title != null) todo.setTitle(title);
        if (description != null) todo.setDescription(description);
        if (priority != null) todo.setPriority(priority.toUpperCase());
        todo.setUpdatedAt(LocalDateTime.now());
        return Optional.of(todo);
    }

    public Optional<Todo> complete(Long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            todosNotFound.increment();
            return Optional.empty();
        }
        todo.setCompleted(true);
        todo.setUpdatedAt(LocalDateTime.now());
        todosCompleted.increment();
        return Optional.of(todo);
    }

    public boolean delete(Long id) {
        if (todos.remove(id) != null) {
            todosDeleted.increment();
            return true;
        }
        todosNotFound.increment();
        return false;
    }

    public Map<String, Object> getStats() {
        long total = todos.size();
        long completed = todos.values().stream().filter(Todo::isCompleted).count();
        long active = total - completed;
        Map<String, Long> byPriority = todos.values().stream()
                .collect(Collectors.groupingBy(Todo::getPriority, Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("active", active);
        stats.put("byPriority", byPriority);
        return stats;
    }
}
