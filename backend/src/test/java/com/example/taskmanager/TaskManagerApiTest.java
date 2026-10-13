package com.example.taskmanager;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.taskmanager.auth.AppUserDetailsService;
import com.jayway.jsonpath.JsonPath;

/** Project and task endpoints, called as the seeded demo user. */
@SpringBootTest
@Transactional
class TaskManagerApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserDetailsService userDetailsService;

    private MockMvc mvc;

    @BeforeEach
    void signInAsDemoUser() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/")
                        .with(user(userDetailsService.loadUserByUsername("demo@example.com")))
                        .with(csrf()))
                .build();
    }

    @Test
    void listsSeededProjectsWithTaskCounts() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].name").value("Website Redesign"))
                .andExpect(jsonPath("$[1].taskCount").value(3))
                .andExpect(jsonPath("$[1].openTaskCount").value(2));
    }

    @Test
    void createsUpdatesAndDeletesTask() throws Exception {
        long projectId = createProject("Test project");

        MvcResult created = mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  Write tests  ", "priority": "HIGH", "dueDate": "2030-01-15", "projectId": %d}
                                """.formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2030-01-15"))
                .andExpect(jsonPath("$.projectName").value("Test project"))
                .andReturn();
        long taskId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(patch("/api/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write more tests", "status": "DONE", "priority": "LOW", "projectId": %d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write more tests"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.dueDate").doesNotExist());

        mvc.perform(get("/api/tasks").param("projectId", String.valueOf(projectId)).param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/tasks/{id}", taskId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks/{id}", taskId)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidTask() throws Exception {
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.projectId").exists());
    }

    @Test
    void returnsNotFoundForUnknownProject() throws Exception {
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Orphan\", \"projectId\": 9999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project 9999 not found"));
    }

    @Test
    void deletingProjectDeletesItsTasks() throws Exception {
        long projectId = createProject("Doomed");
        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Task\", \"projectId\": %d}".formatted(projectId)))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/projects/{id}", projectId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/projects/{id}", projectId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks").param("projectId", String.valueOf(projectId)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private long createProject(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
