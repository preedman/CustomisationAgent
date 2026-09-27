package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class CustomisationAgentServiceTest {

    @Autowired
    private CustomisationAgentService agentService;

    @Autowired
    private CustomisationTools tools;

    @Test
    void shouldExecuteUrgentTaskCustomisationOrchestration() throws Exception {
        String result = agentService.executeUrgentTaskCustomisation();

        assertThat(result)
                .as("Customisation orchestration should complete successfully")
                .contains("CUSTOMISATION_COMPLETED");

        Path reviewTaskJava = tools.getReviewDir().resolve("src/main/java/com/reedmanit/CustomisationAgent/task/Task.java");
        assertThat(Files.exists(reviewTaskJava))
                .as("Staged Task entity should exist in review folder")
                .isTrue();

        String taskCode = Files.readString(reviewTaskJava);
        assertThat(taskCode)
                .as("Review Task entity should have urgent field")
                .contains("boolean urgent");

        Path reviewHtml = tools.getReviewDir().resolve("src/main/resources/static/index.html");
        assertThat(Files.exists(reviewHtml))
                .as("Staged static index.html should exist in review folder")
                .isTrue();

        String html = Files.readString(reviewHtml);
        assertThat(html)
                .as("Review static index.html should contain urgent checkbox")
                .contains("id=\"urgent\"");

        Path reviewDoc = tools.getReviewDir().resolve("DOCUMENTATION.md");
        assertThat(Files.exists(reviewDoc))
                .as("Markdown documentation should exist in review folder")
                .isTrue();

        String docContent = Files.readString(reviewDoc);
        assertThat(docContent)
                .as("Documentation should explain what was changed and what the new code does")
                .contains("Customisation Documentation")
                .contains("Changes Summary")
                .contains("What the New Code Does");
    }

    @Test
    void shouldFallbackToUrgentTaskCustomisationWhenChatClientBuilderIsEmpty() {
        CustomisationTools spyTools = spy(tools);
        CustomisationAgentService fallbackService = new CustomisationAgentService(spyTools, Optional.empty());

        String result = fallbackService.executeWithAi("Add urgent field", "Urgent checkbox saved");

        assertThat(result).contains("CUSTOMISATION_COMPLETED");
        verify(spyTools).cleanReviewDirectory();
    }

    @Test
    void shouldExecuteCustomisationWithAiSuccessfullyWhenAiReturnsValidResponseAndStagesFiles() {
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(mockBuilder.defaultTools(any())).thenReturn(mockBuilder);
        when(mockBuilder.defaultSystem(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn("Customisation complete.");

        CustomisationTools mockTools = mock(CustomisationTools.class);
        when(mockTools.hasReviewFiles()).thenReturn(true);
        CustomisationAgentService service = new CustomisationAgentService(mockTools, Optional.of(mockBuilder));

        String result = service.executeWithAi("Add urgent feature", "Urgent checkbox functional");

        assertThat(result).isNotNull();
        assertThat(result).contains("CUSTOMISATION_COMPLETED");
        assertThat(result).contains("Customisation complete.");
        verify(mockTools).cleanReviewDirectory();
    }

    @Test
    void shouldLogErrorAndStopProcessingWhenNoFilesStagedInReviewFolder() {
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(mockBuilder.defaultTools(any())).thenReturn(mockBuilder);
        when(mockBuilder.defaultSystem(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn("I completed the task in text only.");

        CustomisationTools mockTools = mock(CustomisationTools.class);
        when(mockTools.hasReviewFiles()).thenReturn(false);
        CustomisationAgentService service = spy(new CustomisationAgentService(mockTools, Optional.of(mockBuilder)));

        String result = service.executeWithAi("Add urgent feature", "Urgent checkbox functional");

        assertThat(result)
                .as("Should return error message when no files are staged in review folder")
                .startsWith("ERROR: AI customisation completed without writing any files to the review folder.");

        verify(service, never()).executeUrgentTaskCustomisation();
    }

    @Test
    void shouldCompleteCustomisationWhenAiReturnsEmptyContentButFilesAreStaged() {
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(mockBuilder.defaultTools(any())).thenReturn(mockBuilder);
        when(mockBuilder.defaultSystem(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content()).thenReturn("");

        CustomisationTools mockTools = mock(CustomisationTools.class);
        when(mockTools.hasReviewFiles()).thenReturn(true);
        CustomisationAgentService service = new CustomisationAgentService(mockTools, Optional.of(mockBuilder));

        String result = service.executeWithAi("Add urgent feature", "Urgent checkbox functional");

        assertThat(result)
                .as("Should indicate customisation completed when files are staged")
                .contains("CUSTOMISATION_COMPLETED");
    }

    @Test
    void shouldLogErrorAndStopProcessingWhenAiExecutionThrowsException() {
        ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
        ChatClient mockChatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec mockRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockCallSpec = mock(ChatClient.CallResponseSpec.class);

        when(mockBuilder.defaultTools(any())).thenReturn(mockBuilder);
        when(mockBuilder.defaultSystem(anyString())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockChatClient);

        when(mockChatClient.prompt()).thenReturn(mockRequestSpec);
        when(mockRequestSpec.user(anyString())).thenReturn(mockRequestSpec);
        when(mockRequestSpec.call()).thenReturn(mockCallSpec);
        when(mockCallSpec.content())
                .thenThrow(new RuntimeException("Connection to AI model failed"));

        CustomisationTools mockTools = mock(CustomisationTools.class);
        CustomisationAgentService service = spy(new CustomisationAgentService(mockTools, Optional.of(mockBuilder)));

        String result = service.executeWithAi("Add urgent feature", "Urgent checkbox functional");

        assertThat(result)
                .as("Should return error message when AI execution throws exception")
                .startsWith("ERROR: AI customisation execution failed: Connection to AI model failed");

        verify(service, never()).executeUrgentTaskCustomisation();
    }

    @Test
    void shouldRemoveExistingContentInReviewFolderBeforeWritingNewContent() throws Exception {
        Path reviewDir = tools.getReviewDir();
        Files.createDirectories(reviewDir);
        Path staleFile = reviewDir.resolve("stale-old-file.txt");
        Files.writeString(staleFile, "stale content from previous run");
        assertThat(Files.exists(staleFile)).isTrue();

        String result = agentService.executeUrgentTaskCustomisation();

        assertThat(result).contains("CUSTOMISATION_COMPLETED");
        assertThat(Files.exists(staleFile))
                .as("Existing content in review folder should have been removed before writing new content")
                .isFalse();

        Path reviewTaskJava = reviewDir.resolve("src/main/java/com/reedmanit/CustomisationAgent/task/Task.java");
        assertThat(Files.exists(reviewTaskJava)).isTrue();
    }
}
