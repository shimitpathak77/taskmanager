package com.taskmanager.taskmanager.service;

import com.taskmanager.taskmanager.model.Project;
import com.taskmanager.taskmanager.model.ProjectMember;
import com.taskmanager.taskmanager.model.User;
import com.taskmanager.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.taskmanager.repository.ProjectRepository;
import com.taskmanager.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public Project createProject(String name, String description) {
        User currentUser = getCurrentUser();

        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setCreatedBy(currentUser);
        projectRepository.save(project);

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(currentUser);
        member.setRole(User.Role.ADMIN);
        projectMemberRepository.save(member);

        return project;
    }

    public List<Project> getMyProjects() {
        User currentUser = getCurrentUser();
        List<ProjectMember> memberships =
            projectMemberRepository.findByUserId(currentUser.getId());
        return memberships.stream()
                .map(ProjectMember::getProject)
                .toList();
    }

    public Project getProjectById(Long id) {
        requireProjectMember(id);
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public ProjectMember addMember(Long projectId, String email, User.Role role) {
        requireProjectAdmin(projectId);
        Project project = getProjectById(projectId);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(
                projectId, user.getId())) {
            throw new RuntimeException("User is already a member");
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return projectMemberRepository.save(member);
    }

    public List<ProjectMember> getProjectMembers(Long projectId) {
        requireProjectMember(projectId);
        return projectMemberRepository.findByProjectId(projectId);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public ProjectMember getMembership(Long projectId, Long userId) {
    return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new RuntimeException("Access denied"));
}

public void requireProjectMember(Long projectId) {
    User currentUser = getCurrentUser();
    getMembership(projectId, currentUser.getId());
}

public void requireProjectAdmin(Long projectId) {
    User currentUser = getCurrentUser();
    ProjectMember membership = getMembership(projectId, currentUser.getId());

    if (membership.getRole() != User.Role.ADMIN) {
        throw new RuntimeException("Admin access required");
    }
}

public boolean isProjectMember(Long projectId, Long userId) {
    return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
}

}