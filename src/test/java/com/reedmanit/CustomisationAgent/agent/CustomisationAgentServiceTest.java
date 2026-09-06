package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CustomisationAgentServiceTest {

    @Autowired
    private CustomisationAgentService agentService;

    @Autowired
    private CustomisationTools tools;

    @Test
    void shouldExecuteUrgentTaskCustomisationOrchestration() {
        String result = agentService.executeUrgentTaskCustomisation();

        assertThat(result)
                .as("Customisation orchestration should complete successfully")
                .contains("CUSTOMISATION_COMPLETED");

        Path taskJava = Paths.get("src/main/java/com/reedmanit/CustomisationAgent/task/Task.java");
        assertThat(Files.exists(taskJava)).isTrue();

        String taskCode = tools.readFile("src/main/java/com/reedmanit/CustomisationAgent/task/Task.java");
        assertThat(taskCode)
                .as("Task entity should have urgent field")
                .contains("boolean urgent");

        String html = tools.readFile("src/main/resources/static/index.html");
        assertThat(html)
                .as("Static index.html should contain urgent checkbox")
                .contains("id=\"urgent\"");
    }

    @Test
    void shouldExecuteCustomisationWithAiOrchestrator() {
        String spec = "I need to track which tasks are high priority. Please add an 'Urgent' checkbox to the task creation screen.";
        String criteria = "GIVEN I am on the task creation screen WHEN I enter a task name AND check the 'Urgent' box AND click Submit THEN the system should successfully save the task as urgent.";

        String result = agentService.executeWithAi(spec, criteria);

        assertThat(result).isNotNull();
        assertThat(result).contains("CUSTOMISATION_COMPLETED");
    }
}
