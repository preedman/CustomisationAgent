package com.reedmanit.CustomisationAgent.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        log.info("Creating new task: {}", task);
        Task savedTask = taskRepository.save(task);
        log.debug("Task created successfully: {}", savedTask);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        log.info("Fetching all tasks");
        List<Task> tasks = taskRepository.findAll();
        log.debug("Found {} tasks in repository", tasks.size());
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        log.info("Fetching task by ID: {}", id);
        return taskRepository.findById(id)
                .map(task -> {
                    log.debug("Found task for ID {}: {}", id, task);
                    return ResponseEntity.ok(task);
                })
                .orElseGet(() -> {
                    log.warn("Task with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
