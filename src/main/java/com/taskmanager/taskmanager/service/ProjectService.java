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
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public ProjectMember addMember(Long projectId, String email, User.Role role) {
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
        return projectMemberRepository.findByProjectId(projectId);
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}