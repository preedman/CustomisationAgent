package com.reedmanit.CustomisationAgent.task;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskRepository taskRepository;

    @Test
    void shouldCreateTaskWithSnakeCaseJson() throws Exception {
        Task savedTask = new Task(1L, "Buy server space", true);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"task_name\": \"Buy server space\", \"urgent\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.task_name").value("Buy server space"))
                .andExpect(jsonPath("$.urgent").value(true));
    }

    @Test
    void shouldCreateTaskWithCamelCaseJson() throws Exception {
        Task savedTask = new Task(1L, "Buy server space", false);
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskName\": \"Buy server space\", \"urgent\": false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.task_name").value("Buy server space"))
                .andExpect(jsonPath("$.urgent").value(false));
    }

    @Test
    void shouldGetAllTasks() throws Exception {
        List<Task> tasks = List.of(new Task(1L, "Task 1"), new Task(2L, "Task 2"));
        when(taskRepository.findAll()).thenReturn(tasks);

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].task_name").value("Task 1"))
                .andExpect(jsonPath("$[1].task_name").value("Task 2"));
    }

    @Test
    void shouldGetTaskById() throws Exception {
        Task task = new Task(1L, "Task 1");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.task_name").value("Task 1"));
    }
}
