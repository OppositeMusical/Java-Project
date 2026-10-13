package com.example.taskmanager.project;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanager.auth.AppUserPrincipal;
import com.example.taskmanager.project.ProjectDtos.ProjectRequest;
import com.example.taskmanager.project.ProjectDtos.ProjectResponse;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProjectResponse> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return service.findAll(user.id());
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        return service.findById(id, user.id());
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal AppUserPrincipal user) {
        ProjectResponse created = service.create(request, user.id());
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal AppUserPrincipal user) {
        return service.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        service.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
