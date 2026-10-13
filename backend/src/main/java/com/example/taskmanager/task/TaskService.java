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
    public List<TaskResponse> search(Long projectId, TaskStatus status) {
        return tasks.search(projectId, status).stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return TaskResponse.from(getEntity(id));
    }

    public TaskResponse create(TaskRequest request) {
        Task task = new Task(projects.getEntity(request.projectId()), request.title().strip());
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getEntity(id);
        task.setTitle(request.title().strip());
        if (!task.getProject().getId().equals(request.projectId())) {
            task.setProject(projects.getEntity(request.projectId()));
        }
        apply(task, request);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = getEntity(id);
        task.setStatus(status);
        return TaskResponse.from(tasks.saveAndFlush(task));
    }

    public void delete(Long id) {
        tasks.delete(getEntity(id));
    }

    private Task getEntity(Long id) {
        return tasks.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
    }

    private static void apply(Task task, TaskRequest request) {
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
    }
}
