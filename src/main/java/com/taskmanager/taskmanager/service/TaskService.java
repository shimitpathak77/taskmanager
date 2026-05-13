package com.taskmanager.taskmanager.service;

import com.taskmanager.taskmanager.model.Task;
import com.taskmanager.taskmanager.model.Task.TaskStatus;
import com.taskmanager.taskmanager.model.User;
import com.taskmanager.taskmanager.model.Project;
import com.taskmanager.taskmanager.model.ProjectMember;
import com.taskmanager.taskmanager.repository.TaskRepository;
import com.taskmanager.taskmanager.repository.UserRepository;
import com.taskmanager.taskmanager.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;

    public Task createTask(Long projectId, String title,
                           String description, LocalDate dueDate,
                           String assigneeEmail) {
        projectService.requireProjectAdmin(projectId);
        User currentUser = projectService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User assignee = assigneeEmail != null ?
                userRepository.findByEmail(assigneeEmail)
                        .orElseThrow(() -> new RuntimeException("Assignee not found"))
                : currentUser;
                if (!projectService.isProjectMember(projectId, assignee.getId())){
                        throw new RuntimeException("Assignee must be a project member");
                }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setProject(project);
        task.setAssignee(assignee);
        task.setCreatedBy(currentUser);
        task.setStatus(TaskStatus.TODO);

        return taskRepository.save(task);
    }

    public List<Task> getTasksByProject(Long projectId) {
        projectService.requireProjectMember(projectId);
        return taskRepository.findByProjectId(projectId);
    }

    public List<Task> getMyTasks() {
        User currentUser = projectService.getCurrentUser();
        return taskRepository.findByAssigneeId(currentUser.getId());
    }

    public Task updateTaskStatus(Long taskId, TaskStatus status) {
    User currentUser = projectService.getCurrentUser();

    Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

    Long projectId = task.getProject().getId();

    ProjectMember membership = projectService.getMembership(projectId, currentUser.getId());

    boolean isAdmin = membership.getRole() == User.Role.ADMIN;
    boolean isAssignee = task.getAssignee().getId().equals(currentUser.getId());

    if (!isAdmin && !isAssignee) {
        throw new RuntimeException("Access denied");
    }

    task.setStatus(status);
    return taskRepository.save(task);
    }


    public Task updateTask(Long taskId, String title, String description,
                           LocalDate dueDate, String assigneeEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        Long projectId = task.getProject().getId();
         projectService.requireProjectAdmin(projectId);

        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (dueDate != null) task.setDueDate(dueDate);
        if (assigneeEmail != null) {
            User assignee = userRepository.findByEmail(assigneeEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (!projectService.isProjectMember(projectId, assignee.getId())) {
            throw new RuntimeException("Assignee must be a project member");
                }
            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    public Map<String, Long> getDashboardStats() {
        User currentUser = projectService.getCurrentUser();
        Long userId = currentUser.getId();

        long total = taskRepository.countByAssigneeId(userId);
        long todo = taskRepository.countByAssigneeIdAndStatus(
                userId, TaskStatus.TODO);
        long inProgress = taskRepository.countByAssigneeIdAndStatus(
                userId, TaskStatus.IN_PROGRESS);
        long done = taskRepository.countByAssigneeIdAndStatus(
                userId, TaskStatus.DONE);
        long overdue = taskRepository
                .countByAssigneeIdAndDueDateBeforeAndStatusNot(
                        userId, LocalDate.now(), TaskStatus.DONE);

        return Map.of(
                "total", total,
                "todo", todo,
                "inProgress", inProgress,
                "done", done,
                "overdue", overdue
        );
    }
}