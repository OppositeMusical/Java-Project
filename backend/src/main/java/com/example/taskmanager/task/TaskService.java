package com.example.taskmanager.task;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.common.NotFoundException;
import com.example.taskmanager.project.ProjectService;
import com.example.taskmanager.task.TaskDtos.TaskRequest;
import com.example.taskmanager.task.TaskDtos.TaskResponse;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectService projects;

    public TaskService(TaskRepository tasks, ProjectService projects) {
        this.tasks = tasks;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> search(Long ownerId, Long projectId, TaskStatus status) {
        return tasks.search(ownerId, projectId, status).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id, Long ownerId) {
        return TaskResponse.from(getEntity(id, ownerId));
    }

    public TaskResponse create(TaskRequest request, Long ownerId) {
        Task task = new Task(projects.getEntity(request.projectId(), ownerId), request.title().strip());
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public TaskResponse update(Long id, TaskRequest request, Long ownerId) {
        Task task = getEntity(id, ownerId);
        task.setTitle(request.title().strip());
        if (!task.getProject().getId().equals(request.projectId())) {
            task.setProject(projects.getEntity(request.projectId(), ownerId));
        }
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public TaskResponse updateStatus(Long id, TaskStatus status, Long ownerId) {
        Task task = getEntity(id, ownerId);
        task.setStatus(status);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public void delete(Long id, Long ownerId) {
        tasks.delete(getEntity(id, ownerId));
    }

    private Task getEntity(Long id, Long ownerId) {
        return tasks.findByIdAndProjectOwnerId(id, ownerId).orElseThrow(() -> new NotFoundException("Task", id));
    }

    private static void apply(Task task, TaskRequest request) {
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
    }
}
