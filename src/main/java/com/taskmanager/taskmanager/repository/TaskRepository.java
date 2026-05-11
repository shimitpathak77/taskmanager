package com.taskmanager.taskmanager.repository;

import com.taskmanager.taskmanager.model.Task;
import com.taskmanager.taskmanager.model.Task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssigneeId(Long userId);
    List<Task> findByProjectId(Long projectId);
    long countByAssigneeId(Long userId);
    long countByAssigneeIdAndStatus(Long userId, TaskStatus status);
    long countByAssigneeIdAndDueDateBeforeAndStatusNot(
        Long userId, LocalDate date, TaskStatus status);
}