package com.taskmanager.taskmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.taskmanager.repository.ProjectMemberRepository;
import com.taskmanager.taskmanager.repository.TaskRepository;
import com.taskmanager.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskManagerIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ProjectMemberRepository projectMemberRepository;
    @Autowired TaskRepository taskRepository;

    @Test
    void fullProjectTaskFlowWorksWithRbac() throws Exception {
        String adminToken = signup("Admin", "admin@test.com", "password123");
        String memberToken = signup("Member", "member@test.com", "password123");
        signup("Outsider", "outsider@test.com", "password123");

        Long projectId = createProject(adminToken);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "member@test.com",
                                "role", "MEMBER"
                        ))))
                .andExpect(status().isOk());

        Long taskId = createTask(adminToken, projectId, "member@test.com");

        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Build tests"));

        mockMvc.perform(put("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/tasks/dashboard")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.inProgress").value(1));
    }

    @Test
    void memberCannotAddAnotherMember() throws Exception {
        String adminToken = signup("Admin", "admin2@test.com", "password123");
        String memberToken = signup("Member", "member2@test.com", "password123");
        signup("Other", "other@test.com", "password123");

        Long projectId = createProject(adminToken);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "member2@test.com", "role", "MEMBER"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "other@test.com", "role", "MEMBER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotAssignTaskToNonProjectMember() throws Exception {
        String adminToken = signup("Admin", "admin3@test.com", "password123");
        signup("Outsider", "outsider3@test.com", "password123");

        Long projectId = createProject(adminToken);

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId.toString(),
                                "title", "Invalid assignment",
                                "description", "Should fail",
                                "dueDate", LocalDate.now().plusDays(1).toString(),
                                "assigneeEmail", "outsider3@test.com"
                        ))))
                .andExpect(status().isBadRequest());
    }

    private String signup(String name, String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private Long createProject(String token) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Demo Project",
                                "description", "Integration test project"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private Long createTask(String token, Long projectId, String assigneeEmail) throws Exception {
        String response = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "projectId", projectId.toString(),
                                "title", "Build tests",
                                "description", "Write integration tests",
                                "dueDate", LocalDate.now().plusDays(1).toString(),
                                "assigneeEmail", assigneeEmail
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
