package com.example.taskmanager.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ProjectService(ProjectRepository projects, TaskRepository tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projects.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = projects.save(new Project(request.name().strip(), request.description()));
        return toResponse(project);
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getEntity(id);
        project.setName(request.name().strip());
        project.setDescription(request.description());
        return toResponse(project);
    }

    public void delete(Long id) {
        Project project = getEntity(id);
        tasks.deleteByProjectId(project.getId());
        projects.deleteById(project.getId());
    }

    @Transactional(readOnly = true)
    public Project getEntity(Long id) {
        return projects.findById(id).orElseThrow(() -> new NotFoundException("Project", id));
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
