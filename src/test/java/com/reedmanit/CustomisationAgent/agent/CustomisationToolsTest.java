package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CustomisationToolsTest {

    @TempDir
    Path tempDir;

    private CustomisationTools tools;

    @BeforeEach
    void setUp() {
        tools = new CustomisationTools(tempDir);
    }

    @Test
    void shouldWriteAndReadFile() throws IOException {
        String relativePath = "src/test/Sample.txt";
        String content = "Hello Software Customisation Agent";

        String writeResult = tools.writeFile(relativePath, content);
        assertThat(writeResult).contains("SUCCESS");

        String readResult = tools.readFile(relativePath);
        assertThat(readResult).isEqualTo(content);
    }

    @Test
    void shouldApplyCodeChangeSuccessfully() throws IOException {
        String relativePath = "SampleClass.java";
        String initialContent = """
                public class SampleClass {
                    private String name;
                }
                """;
        tools.writeFile(relativePath, initialContent);

        String targetBlock = "private String name;";
        String replacementBlock = "private String name;\n    private boolean urgent = false;";

        String updateResult = tools.applyCodeChange(relativePath, targetBlock, replacementBlock);
        assertThat(updateResult).contains("SUCCESS");

        String updatedContent = tools.readFile(relativePath);
        assertThat(updatedContent).contains("private boolean urgent = false;");
    }

    @Test
    void shouldReturnErrorWhenFileNotFound() {
        String result = tools.readFile("nonexistent/File.txt");
        assertThat(result).contains("ERROR");
    }

    @Test
    void shouldCompleteCustomisationWithSummary() {
        String summary = "Added urgent field to Task and updated UI";
        String result = tools.completeCustomisation(summary);
        assertThat(result).contains("CUSTOMISATION_COMPLETED: Added urgent field to Task and updated UI");
    }
}
