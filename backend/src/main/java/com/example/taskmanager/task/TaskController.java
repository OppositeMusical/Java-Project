package com.example.taskmanager.task;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.auth.AppUserPrincipal;
import com.example.taskmanager.task.TaskDtos.StatusUpdateRequest;
import com.example.taskmanager.task.TaskDtos.TaskRequest;
import com.example.taskmanager.task.TaskDtos.TaskResponse;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskResponse> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal AppUserPrincipal user) {
        return service.search(user.id(), projectId, status);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return service.findById(id, user.id());
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal AppUserPrincipal user) {
        TaskResponse created = service.create(request, user.id());
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal AppUserPrincipal user) {
        return service.update(id, request, user.id());
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal AppUserPrincipal user) {
        return service.updateStatus(id, request.status(), user.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        service.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
