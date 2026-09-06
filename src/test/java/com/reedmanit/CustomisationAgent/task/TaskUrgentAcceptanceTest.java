package com.reedmanit.CustomisationAgent.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Customer BDD Acceptance Test for the "Urgent Task" Checkbox Customisation.
 *
 * Specification:
 * "I need to track which tasks are high priority. Please add an 'Urgent' checkbox to the task creation screen."
 *
 * Customer Acceptance Criteria:
 * GIVEN I am on the task creation screen
 * WHEN I enter a task name AND check the "Urgent" box AND click Submit
 * THEN the system should successfully save the task as urgent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskUrgentAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("GIVEN on task creation screen THEN urgent checkbox and label are present")
    void givenOnTaskCreationScreen_thenUrgentCheckboxIsPresent() throws Exception {
        MvcResult result = mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();

        assertThat(html)
                .as("Task creation screen must contain task name input")
                .contains("name=\"taskName\"");

        assertThat(html)
                .as("Task creation screen must contain an 'Urgent' checkbox input")
                .contains("id=\"urgent\"")
                .contains("type=\"checkbox\"");

        assertThat(html)
                .as("Task creation screen must contain an 'Urgent' label")
                .contains("Urgent");
    }

    @Test
    @DisplayName("GIVEN task details with urgent checked WHEN submitted THEN task is saved as urgent in DB and API")
    void givenTaskWithUrgentChecked_whenSubmitted_thenSavedAsUrgent() throws Exception {
        // GIVEN task data with urgent = true
        String payload = """
                {
                    "task_name": "Deploy security patch to production",
                    "urgent": true
                }
                """;

        // WHEN submitted to POST /tasks
        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.task_name").value("Deploy security patch to production"))
                .andExpect(jsonPath("$.urgent").value(true))
                .andReturn();

        // THEN verify in database persistence
        Number idNum = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        Long taskId = idNum.longValue();

        Optional<Task> savedTask = taskRepository.findById(taskId);
        assertThat(savedTask).isPresent();
        assertThat(savedTask.get().getTaskName()).isEqualTo("Deploy security patch to production");
        assertThat(savedTask.get().isUrgent()).isTrue();

        // AND verify GET /tasks/{id} returns urgent = true
        mockMvc.perform(get("/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.urgent").value(true));
    }

    @Test
    @DisplayName("GIVEN task details without urgent checked WHEN submitted THEN task is saved as non-urgent")
    void givenTaskWithoutUrgent_whenSubmitted_thenSavedAsNonUrgent() throws Exception {
        String payload = """
                {
                    "task_name": "Order coffee beans",
                    "urgent": false
                }
                """;

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.task_name").value("Order coffee beans"))
                .andExpect(jsonPath("$.urgent").value(false))
                .andReturn();

        Number idNum = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        Long taskId = idNum.longValue();

        Optional<Task> savedTask = taskRepository.findById(taskId);
        assertThat(savedTask).isPresent();
        assertThat(savedTask.get().isUrgent()).isFalse();
    }
}
