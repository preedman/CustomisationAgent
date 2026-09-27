package com.reedmanit.CustomisationAgent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

@Component
public class CustomisationTools {

    private static final Logger log = LoggerFactory.getLogger(CustomisationTools.class);
    private static final String DEFAULT_REVIEW_DIR_NAME = "review";

    private final Path projectRoot;
    private final Path reviewDir;

    public CustomisationTools() {
        this(Paths.get(".").toAbsolutePath().normalize());
    }

    public CustomisationTools(Path projectRoot) {
        this(projectRoot, projectRoot.resolve(DEFAULT_REVIEW_DIR_NAME));
    }

    public CustomisationTools(Path projectRoot, Path reviewDir) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.reviewDir = reviewDir.isAbsolute()
                ? reviewDir.normalize()
                : this.projectRoot.resolve(reviewDir).normalize();
    }

    public String cleanReviewDirectory() {
        try {
            log.debug("Cleaning review directory: {}", reviewDir);
            if (Files.exists(reviewDir)) {
                Files.walkFileTree(reviewDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        deletePath(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (!dir.equals(reviewDir)) {
                            deletePath(dir);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    private void deletePath(Path path) {
                        try {
                            path.toFile().setWritable(true);
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            try {
                                Files.setAttribute(path, "dos:readonly", false);
                                Files.deleteIfExists(path);
                            } catch (Exception ex) {
                                log.warn("Failed to delete review path {}: {}", path, ex.getMessage());
                            }
                        }
                    }
                });
                log.info("Successfully cleaned existing content from review directory: {}", reviewDir);
                return "SUCCESS: Review folder content removed successfully.";
            } else {
                log.debug("Review directory does not exist, nothing to clean.");
                return "SUCCESS: Review directory does not exist.";
            }
        } catch (Exception e) {
            log.error("Failed to clean review directory: {}", reviewDir, e);
            return "ERROR cleaning review directory: " + e.getMessage();
        }
    }

    public String cleanReviewFolder() {
        return cleanReviewDirectory();
    }

    public String clearReviewDirectory() {
        return cleanReviewDirectory();
    }

    public boolean hasReviewFiles() {
        try {
            if (!Files.exists(reviewDir)) {
                return false;
            }
            try (var stream = Files.walk(reviewDir)) {
                return stream.anyMatch(path -> !path.equals(reviewDir) && Files.isRegularFile(path));
            }
        } catch (Exception e) {
            log.error("Failed to check for review files in: {}", reviewDir, e);
            return false;
        }
    }

    @Tool(description = "Reads the entire content of a file given its relative path from the project root or review folder")
    public String readFile(@ToolParam(description = "Relative path to the file, e.g. src/main/java/com/reedmanit/CustomisationAgent/task/Task.java") String relativePath) {
        try {
            log.debug("Reading file from relative path: {}", relativePath);
            Path targetPath = resolvePath(relativePath);
            if (!Files.exists(targetPath)) {
                Path reviewPath = resolveReviewPath(relativePath);
                if (Files.exists(reviewPath)) {
                    targetPath = reviewPath;
                } else {
                    log.warn("File not found at relative path: {}", relativePath);
                    return "ERROR: File not found at " + relativePath;
                }
            }
            String content = Files.readString(targetPath, StandardCharsets.UTF_8);
            log.debug("Read {} characters from file: {}", content.length(), relativePath);
            return content;
        } catch (Exception e) {
            log.error("Failed to read file: {}", relativePath, e);
            return "ERROR reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Applies a code change and writes the resulting updated file into the separate review folder for human review, without overwriting the original file")
    public String applyCodeChange(
            @ToolParam(description = "Relative path to the original file from the project root") String relativePath,
            @ToolParam(description = "Exact existing code block to find and replace") String targetBlock,
            @ToolParam(description = "New code block to replace the target block with") String replacementBlock) {
        try {
            log.debug("Attempting to apply code change for file: {}", relativePath);
            Path reviewPath = resolveReviewPath(relativePath);
            Path originalPath = resolvePath(relativePath);

            Path sourcePath;
            if (Files.exists(reviewPath)) {
                sourcePath = reviewPath;
            } else if (Files.exists(originalPath)) {
                sourcePath = originalPath;
            } else {
                log.warn("File not found for code change at relative path: {}", relativePath);
                return "ERROR: File not found at " + relativePath;
            }

            String content = Files.readString(sourcePath, StandardCharsets.UTF_8);
            String normalizedContent = content.replace("\r\n", "\n");
            String normalizedTarget = targetBlock.replace("\r\n", "\n");
            String normalizedReplacement = replacementBlock.replace("\r\n", "\n");

            if (!normalizedContent.contains(normalizedTarget)) {
                log.warn("Target block not found in file: {}", relativePath);
                return "ERROR: Target block not found in " + relativePath;
            }

            String updatedContent = normalizedContent.replace(normalizedTarget, normalizedReplacement);
            if (reviewPath.getParent() != null && !Files.exists(reviewPath.getParent())) {
                Files.createDirectories(reviewPath.getParent());
            }
            Files.writeString(reviewPath, updatedContent, StandardCharsets.UTF_8);
            log.info("Successfully updated file in review folder: {}", reviewPath);
            return "SUCCESS: File " + relativePath + " updated in review folder successfully.";
        } catch (Exception e) {
            log.error("Failed to apply code change to: {}", relativePath, e);
            return "ERROR applying code change: " + e.getMessage();
        }
    }

    @Tool(description = "Writes the new or modified content of a file into the separate review folder for human review, without overwriting the original file")
    public String writeFile(
            @ToolParam(description = "Relative path to the file from the project root") String relativePath,
            @ToolParam(description = "Content to write into the review file") String content) {
        try {
            log.debug("Writing {} characters to review file: {}", content != null ? content.length() : 0, relativePath);
            Path targetPath = resolveReviewPath(relativePath);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                log.debug("Creating parent directories for: {}", targetPath);
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, content != null ? content : "", StandardCharsets.UTF_8);
            log.info("Successfully wrote review file: {}", targetPath);
            return "SUCCESS: File " + relativePath + " written to review folder successfully.";
        } catch (Exception e) {
            log.error("Failed to write file: {}", relativePath, e);
            return "ERROR writing file: " + e.getMessage();
        }
    }

    @Tool(description = "Writes a markdown documentation file in the review directory describing what has been changed and what the new code does")
    public String writeDocumentation(
            @ToolParam(description = "Relative path or filename for the documentation, e.g. DOCUMENTATION.md") String relativePath,
            @ToolParam(description = "Markdown documentation content describing what has been changed and what the new code does") String content) {
        try {
            String docPath = (relativePath == null || relativePath.trim().isEmpty()) ? "DOCUMENTATION.md" : relativePath.trim();
            log.debug("Writing documentation to review file: {}", docPath);
            Path targetPath = resolveReviewPath(docPath);
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.writeString(targetPath, content != null ? content : "", StandardCharsets.UTF_8);
            log.info("Successfully wrote documentation in review folder: {}", targetPath);
            return "SUCCESS: Documentation written to review folder at " + docPath;
        } catch (Exception e) {
            log.error("Failed to write documentation: {}", relativePath, e);
            return "ERROR writing documentation: " + e.getMessage();
        }
    }

    @Tool(description = "Executes the Maven test suite or a specific test class and returns the result")
    public String executeTestSuite(@ToolParam(description = "Optional test class name to target, or empty for all tests") String testClass) {
        try {
            log.info("Executing Maven test suite for test class: {}", (testClass != null && !testClass.trim().isEmpty()) ? testClass.trim() : "ALL");
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

            log.debug("Starting Maven test process with command: {}", pb.command());
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
                log.error("Maven test execution timed out after 120 seconds");
                process.destroyForcibly();
                return "ERROR: Test execution timed out after 120 seconds.";
            }

            int exitCode = process.exitValue();
            String fullOutput = output.toString();
            boolean success = exitCode == 0 && fullOutput.contains("BUILD SUCCESS");
            log.debug("Maven test execution finished. Exit code: {}, Success: {}", exitCode, success);

            StringBuilder summary = new StringBuilder();
            summary.append("Execution Exit Code: ").append(exitCode).append("\n");
            summary.append("Status: ").append(success ? "TESTS PASSED" : "TESTS FAILED").append("\n");

            for (String line : fullOutput.split("\n")) {
                if (line.contains("Tests run:") || line.contains("BUILD SUCCESS") || line.contains("BUILD FAILURE") || line.contains("ERROR")) {
                    summary.append(line.trim()).append("\n");
                }
            }

            log.info("Test execution summary:\n{}", summary);
            return summary.toString();
        } catch (Exception e) {
            log.error("Failed to execute test suite", e);
            return "ERROR executing tests: " + e.getMessage();
        }
    }

    @Tool(description = "Completes the customisation process with a summary message and status")
    public String completeCustomisation(@ToolParam(description = "Summary of changes made and review documentation status") String summary) {
        log.info("Customisation completed: {}", summary);
        return "CUSTOMISATION_COMPLETED: " + summary;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getReviewDir() {
        return reviewDir;
    }

    private Path resolvePath(String relativePath) {
        Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new IllegalArgumentException("Path traversal outside project root is not permitted: " + relativePath);
        }
        return resolved;
    }

    private Path resolveReviewPath(String relativePath) {
        String cleanPath = relativePath.replace("\\", "/");
        if (cleanPath.startsWith("review/")) {
            cleanPath = cleanPath.substring("review/".length());
        } else if (cleanPath.startsWith("./review/")) {
            cleanPath = cleanPath.substring("./review/".length());
        }
        Path resolved = reviewDir.resolve(cleanPath).normalize();
        if (!resolved.startsWith(reviewDir)) {
            throw new IllegalArgumentException("Path traversal outside review directory is not permitted: " + relativePath);
        }
        return resolved;
    }
}
