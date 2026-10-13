package com.example.taskmanager.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.auth.AppUserRepository;
import com.example.taskmanager.common.NotFoundException;
import com.example.taskmanager.project.ProjectDtos.ProjectRequest;
import com.example.taskmanager.project.ProjectDtos.ProjectResponse;
import com.example.taskmanager.task.TaskRepository;
import com.example.taskmanager.task.TaskStatus;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final AppUserRepository users;

    public ProjectService(ProjectRepository projects, TaskRepository tasks, AppUserRepository users) {
        this.projects = projects;
        this.tasks = tasks;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(Long ownerId) {
        return projects.findAllByOwnerIdOrderByNameAsc(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id, Long ownerId) {
        return toResponse(getEntity(id, ownerId));
    }

    public ProjectResponse create(ProjectRequest request, Long ownerId) {
        Project project = projects.save(
                new Project(users.getReferenceById(ownerId), request.name().strip(), request.description()));
        return toResponse(project);
    }

    public ProjectResponse update(Long id, ProjectRequest request, Long ownerId) {
        Project project = getEntity(id, ownerId);
        project.setName(request.name().strip());
        project.setDescription(request.description());
        return toResponse(project);
    }

    public void delete(Long id, Long ownerId) {
        Project project = getEntity(id, ownerId);
        tasks.deleteByProjectId(project.getId());
        projects.deleteById(project.getId());
    }

    /** Other users' projects are reported as not found, so their ids don't leak. */
    @Transactional(readOnly = true)
    public Project getEntity(Long id, Long ownerId) {
        return projects.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new NotFoundException("Project", id));
    }

    private ProjectResponse toResponse(Project project) {
        long total = tasks.countByProjectId(project.getId());
        long done = tasks.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE);
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                total,
                total - done,
                project.getCreatedAt());
    }
}
