package com.taskmanager.taskmanager.controller;

import com.taskmanager.taskmanager.model.Project;
import com.taskmanager.taskmanager.model.ProjectMember;
import com.taskmanager.taskmanager.model.User;
import com.taskmanager.taskmanager.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Project> createProject(
            @RequestBody Map<String, String> request) {
        Project project = projectService.createProject(
                request.get("name"),
                request.get("description")
        );
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ProjectMember> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        User.Role role = request.get("role") != null ?
                User.Role.valueOf(request.get("role")) : User.Role.MEMBER;
        ProjectMember member = projectService.addMember(
                id,
                request.get("email"),
                role
        );
        return ResponseEntity.ok(member);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMember>> getMembers(
            @PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectMembers(id));
    }
}