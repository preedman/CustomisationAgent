package com.reedmanit.CustomisationAgent.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomisationToolsTest {

    @TempDir
    Path tempDir;

    private CustomisationTools tools;

    @BeforeEach
    void setUp() {
        tools = new CustomisationTools(tempDir);
    }

    @Test
    void shouldWriteFileToReviewDirectoryWithoutOverwritingOriginal() throws IOException {
        String relativePath = "src/test/Sample.txt";
        String content = "Hello Software Customisation Agent";

        String writeResult = tools.writeFile(relativePath, content);
        assertThat(writeResult).contains("SUCCESS");
        assertThat(writeResult).contains("review folder");

        Path reviewFile = tempDir.resolve("review").resolve(relativePath);
        Path originalFile = tempDir.resolve(relativePath);

        assertThat(Files.exists(reviewFile)).isTrue();
        assertThat(Files.readString(reviewFile)).isEqualTo(content);
        assertThat(Files.exists(originalFile)).isFalse();

        String readResult = tools.readFile(relativePath);
        assertThat(readResult).isEqualTo(content);
    }

    @Test
    void shouldApplyCodeChangeToReviewFolderWithoutModifyingOriginalFile() throws IOException {
        String relativePath = "SampleClass.java";
        String initialContent = """
                public class SampleClass {
                    private String name;
                }
                """;
        Path originalFile = tempDir.resolve(relativePath);
        Files.writeString(originalFile, initialContent);

        String targetBlock = "private String name;";
        String replacementBlock = "private String name;\n    private boolean urgent = false;";

        String updateResult = tools.applyCodeChange(relativePath, targetBlock, replacementBlock);
        assertThat(updateResult).contains("SUCCESS");
        assertThat(updateResult).contains("review folder");

        // Verify original file is unchanged
        assertThat(Files.readString(originalFile)).isEqualTo(initialContent);

        // Verify review file contains modified code
        Path reviewFile = tempDir.resolve("review").resolve(relativePath);
        assertThat(Files.exists(reviewFile)).isTrue();
        assertThat(Files.readString(reviewFile)).contains("private boolean urgent = false;");
    }

    @Test
    void shouldWriteMarkdownDocumentationToReviewDirectory() throws IOException {
        String docContent = """
                # Customisation Documentation
                ## Changes Made
                - Added urgent field to Task.
                ## What New Code Does
                - Allows tracking high priority tasks.
                """;

        String writeDocResult = tools.writeDocumentation("DOCUMENTATION.md", docContent);
        assertThat(writeDocResult).contains("SUCCESS");

        Path docFile = tempDir.resolve("review/DOCUMENTATION.md");
        assertThat(Files.exists(docFile)).isTrue();
        assertThat(Files.readString(docFile)).isEqualTo(docContent);
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

    @Test
    void shouldPreventPathTraversal() {
        String writeResult = tools.writeFile("../outside.txt", "evil");
        assertThat(writeResult).contains("ERROR");
        assertThat(writeResult).contains("Path traversal");

        String readResult = tools.readFile("../outside.txt");
        assertThat(readResult).contains("ERROR");
        assertThat(readResult).contains("Path traversal");
    }

    @Test
    void shouldCleanExistingReviewDirectoryContentIfExists() throws IOException {
        Path reviewDir = tempDir.resolve("review");
        Path subDir = reviewDir.resolve("src/main/java");
        Files.createDirectories(subDir);

        Path file1 = subDir.resolve("OldTask.java");
        Path file2 = reviewDir.resolve("OLD_DOCUMENTATION.md");
        Files.writeString(file1, "old content");
        Files.writeString(file2, "old doc content");

        assertThat(Files.exists(file1)).isTrue();
        assertThat(Files.exists(file2)).isTrue();

        String result = tools.cleanReviewDirectory();
        assertThat(result).contains("SUCCESS");

        assertThat(Files.exists(file1)).isFalse();
        assertThat(Files.exists(file2)).isFalse();
        assertThat(Files.exists(subDir)).isFalse();
    }

    @Test
    void shouldHandleCleanReviewDirectoryWhenDirectoryDoesNotExist() {
        Path reviewDir = tempDir.resolve("review");
        assertThat(Files.exists(reviewDir)).isFalse();

        String result = tools.cleanReviewDirectory();
        assertThat(result).contains("SUCCESS");
    }

    @Test
    void shouldDetectWhetherReviewFilesExist() throws IOException {
        assertThat(tools.hasReviewFiles()).isFalse();

        Path reviewDir = tempDir.resolve("review");
        Files.createDirectories(reviewDir);
        assertThat(tools.hasReviewFiles()).isFalse();

        Path subDir = reviewDir.resolve("src/main/java");
        Files.createDirectories(subDir);
        assertThat(tools.hasReviewFiles()).isFalse();

        Path sampleFile = subDir.resolve("Sample.java");
        Files.writeString(sampleFile, "public class Sample {}");
        assertThat(tools.hasReviewFiles()).isTrue();
    }
}
