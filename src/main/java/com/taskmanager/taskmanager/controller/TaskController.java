package com.taskmanager.taskmanager.controller;

import com.taskmanager.taskmanager.model.Task;
import com.taskmanager.taskmanager.model.Task.TaskStatus;
import com.taskmanager.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Map<String, String> request) {
        Task task = taskService.createTask(
                Long.parseLong(request.get("projectId")),
                request.get("title"),
                request.get("description"),
                request.get("dueDate") != null ?
                        LocalDate.parse(request.get("dueDate")) : null,
                request.get("assigneeEmail")
        );
        return ResponseEntity.ok(task);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Task>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getTasksByProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Task task = taskService.updateTaskStatus(
                id,
                TaskStatus.valueOf(request.get("status"))
        );
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Task task = taskService.updateTask(
                id,
                request.get("title"),
                request.get("description"),
                request.get("dueDate") != null ?
                        LocalDate.parse(request.get("dueDate")) : null,
                request.get("assigneeEmail")
        );
        return ResponseEntity.ok(task);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Long>> getDashboard() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }
}