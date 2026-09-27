package com.reedmanit.CustomisationAgent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomisationAgentService {

    private static final Logger log = LoggerFactory.getLogger(CustomisationAgentService.class);

    private final CustomisationTools tools;
    private final Optional<ChatClient.Builder> chatClientBuilder;

    @Autowired
    public CustomisationAgentService(CustomisationTools tools, Optional<ChatClient.Builder> chatClientBuilder) {
        this.tools = tools;
        this.chatClientBuilder = chatClientBuilder;
    }

    public CustomisationAgentService(CustomisationTools tools) {
        this(tools, Optional.empty());
    }

    /**
     * Executes the customisation prompt using the configured Spring AI ChatClient with tool callbacks.
     */
    public String executeWithAi(String specification, String acceptanceCriteria) {
        log.info("Executing customisation with AI - specification: '{}', acceptanceCriteria: '{}'",
                specification, acceptanceCriteria);
        if (chatClientBuilder.isEmpty()) {
            log.warn("ChatClient.Builder not present, falling back to deterministic orchestrator.");
            return executeUrgentTaskCustomisation();
        }

        try {
            // Ensure any existing content in review directory is removed before writing new content
            tools.cleanReviewDirectory();

            log.debug("Configuring ChatClient with default tools and system prompt...");
            ChatClient chatClient = chatClientBuilder.get()
                    .defaultTools(tools)
                    .defaultSystem("""
                        You are a Software Customisation AI Agent for a Spring Boot application.
                        IMPORTANT: Do NOT overwrite existing project files directly.
                        Instead, create and write all new and modified code into the separate review folder using the writeFile or applyCodeChange tools so that it can be reviewed by a human before implementing the changes.
                        In addition, you MUST generate and write a markdown documentation file in the review directory (e.g. DOCUMENTATION.md) using the writeDocumentation tool describing what has been changed and what the new code does.
                        You MUST execute tool calls to write the staged files and documentation to disk before completing.

                        When given a customer specification and acceptance criteria, generate and execute the plan:
                        1. Storage/Database Layer: Read the existing JPA entities with readFile and write updated entity code to the review directory using writeFile or applyCodeChange.
                        2. API/Controller Layer: Read existing controllers/endpoints with readFile and write updated/new controllers to the review directory using writeFile or applyCodeChange.
                        3. UI Layer: Read existing HTML/JS views with readFile and write updated views to the review directory using writeFile or applyCodeChange.
                        4. Documentation: Generate and write a markdown documentation file (DOCUMENTATION.md) in the review folder using writeDocumentation explaining what was changed and what the new code does.
                        5. Verification: Run the test suite if appropriate using executeTestSuite.
                        6. Completion: Call completeCustomisation when done.
                        """)
                    .build();

            String prompt = String.format("""
                Customer Specification:
                %s

                Acceptance Criteria:
                %s

                Please execute the customisation plan now using your tools, creating the new code and markdown documentation in the review folder.
                """, specification, acceptanceCriteria);

            log.debug("Sending prompt to AI ChatClient:\n{}", prompt);
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Received AI response: {}", response);

            boolean filesStaged = tools.hasReviewFiles();
            if (!filesStaged) {
                log.error("AI customisation completed without writing any files to the review folder.");
                return "ERROR: AI customisation completed without writing any files to the review folder.";
            }

            if (response != null && !response.trim().isEmpty()) {
                String formattedResponse = response.trim();
                log.info("AI customisation completed successfully with response: \n{}", formattedResponse);
                if (!formattedResponse.contains("CUSTOMISATION_COMPLETED")) {
                    formattedResponse = "CUSTOMISATION_COMPLETED:\n" + formattedResponse;
                }
                return formattedResponse;
            } else {
                log.info("AI customisation completed and files are staged in the review directory.");
                return "CUSTOMISATION_COMPLETED: Customisation completed and files are staged in the review folder.";
            }
        } catch (Exception e) {
            log.error("AI customisation execution failed ({}), stopping customisation processing.", e.getMessage(), e);
            return "ERROR: AI customisation execution failed: " + e.getMessage();
        }
    }

    /**
     * Executes the end-to-end "Urgent Task" customisation plan autonomously through the tools,
     * staging the new code and markdown documentation in the review directory.
     */
    public String executeUrgentTaskCustomisation() {
        log.info("Starting autonomous Customisation Agent loop for 'Urgent Task'...");

        // Ensure any existing content in review directory is removed before writing new content
        tools.cleanReviewDirectory();

        // Step 1: Storage / Entity Update in Review Folder
        String taskJavaPath = "src/main/java/com/reedmanit/CustomisationAgent/task/Task.java";
        String taskContent = tools.readFile(taskJavaPath);

        String updatedTaskCode = """
package com.reedmanit.CustomisationAgent.task;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_name")
    @JsonProperty("task_name")
    @JsonAlias({"taskName", "task_name"})
    private String taskName;

    @Column(name = "is_urgent")
    @JsonProperty("urgent")
    @JsonAlias({"urgent", "is_urgent", "isUrgent"})
    private boolean urgent = false;

    public Task() {
    }

    public Task(String taskName) {
        this.taskName = taskName;
        this.urgent = false;
    }

    public Task(String taskName, boolean urgent) {
        this.taskName = taskName;
        this.urgent = urgent;
    }

    public Task(Long id, String taskName) {
        this.id = id;
        this.taskName = taskName;
        this.urgent = false;
    }

    public Task(Long id, String taskName, boolean urgent) {
        this.id = id;
        this.taskName = taskName;
        this.urgent = urgent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", taskName='" + taskName + '\'' +
                ", urgent=" + urgent +
                '}';
    }
}
""";
        tools.writeFile(taskJavaPath, updatedTaskCode);
        log.info("Step 1 (Storage): Task entity created in review folder with 'urgent' field.");

        // Step 2: UI / HTML View Update in Review Folder
        String htmlPath = "src/main/resources/static/index.html";
        String updatedHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Task Tracker</title>
</head>
<body>
    <h1>Task Tracker</h1>
    <form id="taskForm" action="/tasks" method="POST">
        <div>
            <label for="taskName">Task Name</label>
            <input type="text" id="taskName" name="taskName" placeholder="Enter task name" required>
        </div>
        <div>
            <input type="checkbox" id="urgent" name="urgent">
            <label for="urgent">Urgent</label>
        </div>
        <div>
            <button type="submit" id="submitBtn">Submit</button>
        </div>
    </form>

    <div id="result"></div>

    <script>
        document.getElementById('taskForm').addEventListener('submit', async function (e) {
            e.preventDefault();
            const taskName = document.getElementById('taskName').value;
            const isUrgent = document.getElementById('urgent').checked;
            try {
                const response = await fetch('/tasks', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        task_name: taskName,
                        urgent: isUrgent
                    })
                });
                if (response.ok) {
                    const savedTask = await response.json();
                    document.getElementById('result').innerText = 'Task created successfully with ID: ' + savedTask.id;
                } else {
                    document.getElementById('result').innerText = 'Failed to create task';
                }
            } catch (err) {
                document.getElementById('result').innerText = 'Error: ' + err.message;
            }
        });
    </script>
</body>
</html>
""";
        tools.writeFile(htmlPath, updatedHtml);
        log.info("Step 2 (UI): static/index.html created in review folder with 'Urgent' checkbox.");

        // Step 3: Markdown Documentation Generation
        String documentationMarkdown = """
# Customisation Documentation: Urgent Task Feature

## Overview
This customisation adds support for tracking high-priority / urgent tasks within the Task Tracker application.
All new and modified code is staged in the separate `review` folder to allow thorough human review before implementation.

## Changes Summary

### 1. Storage / Database Layer
- **File:** `src/main/java/com/reedmanit/CustomisationAgent/task/Task.java` (staged in `review/`)
- **What Changed:**
  - Added boolean field `urgent` (default `false`) mapped to database column `is_urgent`.
  - Added Jackson annotations `@JsonProperty("urgent")` and `@JsonAlias({"urgent", "is_urgent", "isUrgent"})`.
  - Added constructors, getter `isUrgent()`, setter `setUrgent(boolean)`, and updated `toString()`.
- **What the New Code Does:** Enables storing, persisting, and serializing/deserializing task urgency status in the database and API payloads.

### 2. UI Layer
- **File:** `src/main/resources/static/index.html` (staged in `review/`)
- **What Changed:**
  - Added an Urgent checkbox (`<input type="checkbox" id="urgent" name="urgent">`) with an associated label.
  - Updated form submission JavaScript to read the checkbox state and include `"urgent": isUrgent` in the JSON request payload.
- **What the New Code Does:** Allows users creating tasks in the web UI to mark a task as urgent, which is then submitted to the backend API.

## Human Review & Implementation Guide
1. Inspect the staged files in the `review` folder to ensure code quality and conformity.
2. Once approved, copy/merge the files from `review/` to the main source directory `src/`.
3. Run the test suite (`mvn test`) to verify end-to-end integration.
""";
        tools.writeDocumentation("DOCUMENTATION.md", documentationMarkdown);
        log.info("Step 3 (Documentation): Markdown documentation written to review/DOCUMENTATION.md.");

        // Step 4: Verification via Maven Test execution
        String testResults = tools.executeTestSuite("TaskRepositoryTest");
        log.info("Step 4 (Verification): Test results - {}", testResults);

        return tools.completeCustomisation("Urgent Task checkbox feature successfully staged in review folder with documentation for human review.");
    }
}
