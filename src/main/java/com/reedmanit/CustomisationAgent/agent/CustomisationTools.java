package com.reedmanit.CustomisationAgent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
public class CustomisationTools {

    private static final Logger log = LoggerFactory.getLogger(CustomisationTools.class);
    private final Path projectRoot;

    public CustomisationTools() {
        this(Paths.get(".").toAbsolutePath().normalize());
    }

    public CustomisationTools(Path projectRoot) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Tool(description = "Reads the entire content of a file given its relative path from the project root")
    public String readFile(@ToolParam(description = "Relative path to the file from the project root, e.g. src/main/java/com/reedmanit/CustomisationAgent/task/Task.java") String relativePath) {
        try {
            Path targetPath = resolvePath(relativePath);
            if (!Files.exists(targetPath)) {
                return "ERROR: File not found at " + relativePath;
            }
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file: {}", relativePath, e);
            return "ERROR reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Replaces a specific target code block with a replacement code block in the file at the given relative path")
    public String applyCodeChange(
            @ToolParam(description = "Relative path to the file from the project root") String relativePath,
            @ToolParam(description = "Exact existing code block to find and replace") String targetBlock,
            @ToolParam(description = "New code block to replace the target block with") String replacementBlock) {
        try {
            Path targetPath = resolvePath(relativePath);
            if (!Files.exists(targetPath)) {
                return "ERROR: File not found at " + relativePath;
            }

            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            String normalizedContent = content.replace("\r\n", "\n");
            String normalizedTarget = targetBlock.replace("\r\n", "\n");
            String normalizedReplacement = replacementBlock.replace("\r\n", "\n");

            if (!normalizedContent.contains(normalizedTarget)) {
                return "ERROR: Target block not found in " + relativePath;
            }

            String updatedContent = normalizedContent.replace(normalizedTarget, normalizedReplacement);
            Files.writeString(targetPath, updatedContent, StandardCharsets.UTF_8);
            log.info("Successfully updated file: {}", relativePath);
            return "SUCCESS: File " + relativePath + " updated successfully.";
        } catch (Exception e) {
            log.error("Failed to apply code change to: {}", relativePath, e);
            return "ERROR applying code change: " + e.getMessage();
        }
    }

    @Tool(description = "Writes or overwrites the entire content of a file at the given relative path")
    public String writeFile(
            @ToolParam(description = "Relative path to the file from the project root") String relativePath,
            @ToolParam(description = "Content to write into the file") String content) {
        try {
            Path targetPath = resolvePath(relativePath);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
            log.info("Successfully wrote file: {}", relativePath);
            return "SUCCESS: File " + relativePath + " written successfully.";
        } catch (Exception e) {
            log.error("Failed to write file: {}", relativePath, e);
            return "ERROR writing file: " + e.getMessage();
        }
    }

    @Tool(description = "Executes the Maven test suite or a specific test class and returns the result")
    public String executeTestSuite(@ToolParam(description = "Optional test class name to target, or empty for all tests") String testClass) {
        try {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            String mvnCmd = isWindows ? (Files.exists(projectRoot.resolve("mvnw.cmd")) ? projectRoot.resolve("mvnw.cmd").toString() : "mvn.cmd")
                    : (Files.exists(projectRoot.resolve("mvnw")) ? projectRoot.resolve("mvnw").toString() : "mvn");

            ProcessBuilder pb;
            if (testClass != null && !testClass.trim().isEmpty()) {
                pb = new ProcessBuilder(mvnCmd, "test", "-Dtest=" + testClass.trim());
            } else {
                pb = new ProcessBuilder(mvnCmd, "test");
            }

            pb.directory(projectRoot.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "ERROR: Test execution timed out after 120 seconds.";
            }

            int exitCode = process.exitValue();
            String fullOutput = output.toString();
            boolean success = exitCode == 0 && fullOutput.contains("BUILD SUCCESS");

            StringBuilder summary = new StringBuilder();
            summary.append("Execution Exit Code: ").append(exitCode).append("\n");
            summary.append("Status: ").append(success ? "TESTS PASSED" : "TESTS FAILED").append("\n");

            for (String line : fullOutput.split("\n")) {
                if (line.contains("Tests run:") || line.contains("BUILD SUCCESS") || line.contains("BUILD FAILURE") || line.contains("ERROR")) {
                    summary.append(line.trim()).append("\n");
                }
            }

            return summary.toString();
        } catch (Exception e) {
            log.error("Failed to execute test suite", e);
            return "ERROR executing tests: " + e.getMessage();
        }
    }

    @Tool(description = "Completes the customisation process with a summary message and status")
    public String completeCustomisation(@ToolParam(description = "Summary of changes made and verification status") String summary) {
        log.info("Customisation completed: {}", summary);
        return "CUSTOMISATION_COMPLETED: " + summary;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    private Path resolvePath(String relativePath) {
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new IllegalArgumentException("Path traversal outside project root is not permitted: " + relativePath);
        }
        return resolved;
    }
}
