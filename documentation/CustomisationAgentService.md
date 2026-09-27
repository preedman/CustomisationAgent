# CustomisationAgentService

## Overview

CustomisationAgentService is the main orchestration service for the customisation agent.

Its responsibility is to take a customer specification and acceptance criteria, then coordinate the process of customising the application. It can either use a Spring AI ChatClient with registered tools, or fall back to a deterministic built-in customisation workflow.

The service is part of the agent package.

## Purpose

This service provides the application-level customisation workflow.

It supports two execution modes:

1. AI-driven customisation using Spring AI and tool callbacks.
2. Deterministic fallback customisation for the urgent-task feature.

## Main Responsibilities

- Accept a customer specification.
- Accept acceptance criteria.
- Build an AI prompt for the customisation task.
- Register customisation tools with the AI chat client.
- Instruct the AI model to stage all new and modified code in a separate review directory instead of overwriting existing project files.
- Instruct the AI model to generate a markdown documentation file in the review directory describing what has been changed and what the new code does.
- Fall back to a built-in urgent-task customisation if ChatClient.Builder is unavailable.
- Use CustomisationTools to read files, stage files in the review folder, write markdown documentation, run tests, and complete the customisation process.

## Dependencies

The service depends on:

- CustomisationTools
- Optional<ChatClient.Builder>

### CustomisationTools

CustomisationTools provides the file and test execution operations used by the customisation agent.

These include:

- reading source files
- writing source files
- applying code changes
- running Maven tests
- completing the customisation process

### ChatClient.Builder

ChatClient.Builder is provided by Spring AI when an AI chat model is configured.

It is wrapped in Optional so that the application can still run if the AI chat client is not available.

If the builder is missing, the service falls back to the deterministic urgent-task workflow.

## Constructors

### Primary Constructor

The primary constructor accepts both dependencies:

- CustomisationTools
- Optional<ChatClient.Builder>

This constructor is used by Spring dependency injection.

### Convenience Constructor

A second constructor accepts only CustomisationTools.

This is useful for tests or scenarios where no AI chat client is available.

Internally, it delegates to the primary constructor with Optional.empty().

## Methods

## executeWithAi

executeWithAi(String specification, String acceptanceCriteria)

This method is the main entry point for AI-driven customisation.

It accepts:

- specification - the requested application change
- acceptanceCriteria - the conditions that must be met for the change to be accepted

### Behaviour

The method first checks whether a ChatClient.Builder is available.

If no builder is available, it logs a warning and calls:

executeUrgentTaskCustomisation()

If a builder is available, it creates a ChatClient and registers CustomisationTools as the default tools.

It also defines a system prompt that instructs the AI agent to follow a multi-step customisation plan.

Before invoking the chat client, `CustomisationAgentService` calls `cleanReviewDirectory()` directly to ensure stale files from previous runs are removed.

### AI Customisation Plan

The system prompt asks the AI agent to follow these steps:

1. Storage/Database Layer
   Read existing entity files with `readFile` and write updated JPA entities to the separate review folder using `writeFile` or `applyCodeChange`.

2. API/Controller Layer
   Read existing endpoints with `readFile` and write updated/new controllers to the review folder using `writeFile` or `applyCodeChange`.

3. UI Layer
   Read existing views with `readFile` and write updated HTML/JS files to the review folder using `writeFile` or `applyCodeChange`.

4. Documentation
   Generate and write a markdown documentation file (`DOCUMENTATION.md`) in the review folder using `writeDocumentation` explaining what has been changed and what the new code does.

5. Verification
   Optionally run the test suite using `executeTestSuite`.

6. Completion
   Complete the customisation process using `completeCustomisation`.

### Prompt Construction

The method combines the customer specification and acceptance criteria into a single user prompt.

The prompt asks the AI model to execute the customisation plan using the available tools, directing new code and markdown documentation into the review folder.

### AI Invocation & Multi-Turn Tool Calling

The service calls the chat client using standard text/content evaluation (`.content()`) rather than constraining the response to a single-turn structured JSON schema (`.entity(...)`). This allows local and remote LLMs (such as Ollama models) to execute multiple tool turns (`readFile`, `writeFile`, `applyCodeChange`, `writeDocumentation`, `executeTestSuite`, `completeCustomisation`) uninterrupted.

### Review Folder Verification

Following the AI execution, `CustomisationAgentService` checks `tools.hasReviewFiles()` to confirm that modified or generated files actually exist in the `review/` staging directory. If the AI finishes or returns without executing any write tools (resulting in an empty review directory), the service detects this condition, logs an error, and returns an error response indicating that no files were staged.

### Error Handling

If the AI execution throws an exception, returns without staging files, or fails to create content in the review folder, the method logs an error and stops processing, returning an error message instead of falling back to deterministic execution.

## executeUrgentTaskCustomisation

executeUrgentTaskCustomisation()

This method provides a deterministic built-in customisation workflow.

It implements the urgent-task feature staging all new/modified files and documentation in the review folder without relying on AI model output.

## Deterministic Urgent Task Workflow

The workflow performs the following operations.

### Step 1: Storage / Entity Update in Review Folder

The service reads the task entity file and writes the updated version containing urgent task support into the review directory (`review/src/main/java/com/reedmanit/CustomisationAgent/task/Task.java`).

The updated task entity includes:

- an urgent boolean field
- database column mapping
- JSON property mapping
- JSON aliases
- constructors
- getter and setter methods

### Step 2: UI / HTML View Update in Review Folder

The service reads the static task page and writes an updated version into the review directory (`review/src/main/resources/static/index.html`) containing:

- a task name input
- an urgent checkbox
- JavaScript form handling
- JSON submission to the task API
- basic success and failure messaging

### Step 3: Markdown Documentation Generation

The service generates a comprehensive Markdown documentation file (`review/DOCUMENTATION.md`) detailing:

- An overview of the feature
- Exact files and code changed across database and UI layers
- Explanation of what the new code does
- Review and implementation guidance for human reviewers

### Step 4: Verification

The service runs the Maven test suite for the repository test class.

The test execution result is logged.

### Step 5: Completion

The service calls the customisation completion tool with a summary message.

The returned value indicates that the customisation process has completed with staged changes and documentation ready for review.

## Fallback Behaviour

The fallback behaviour allows the application to run the built-in urgent-task customisation when AI configuration (`ChatClient.Builder`) is not available.

Fallback happens when:

- ChatClient.Builder is not present in the application context

If an AI chat client is configured but execution fails or returns a null response, the service logs an error and stops processing without executing the fallback workflow.

## Logging

The service uses SLF4J logging.

It logs:

- when AI support is unavailable
- when AI execution fails
- when the urgent-task customisation starts
- when each deterministic workflow step completes
- when test execution results are available

## Design Notes

The service currently mixes two responsibilities:

1. General AI-driven customisation.
2. A hard-coded urgent-task customisation.

This is useful for demonstration and fallback purposes, but in a larger application the deterministic workflow could be moved into a separate class.

Possible future improvements include:

- creating a generic customisation planner
- separating fallback workflows from AI orchestration
- returning structured result objects instead of plain strings
- validating specification and acceptance criteria before execution
- adding authentication around customisation execution
- adding safeguards before allowing source file writes
- improving test selection based on changed files

## Example Usage

A controller or another service can call:

executeWithAi(specification, acceptanceCriteria)

Example specification:

Add support for a task description. Users should be able to enter a description when creating a task.

Example acceptance criteria:

The task creation screen should include a task description field. The description should be saved in the database and returned by the task API. The Maven test suite should pass.

The service will attempt to execute the request through the AI chat client and customisation tools.

If AI execution is unavailable, it will run the deterministic fallback customisation.

## Security Considerations

This service can trigger source-code modification through CustomisationTools.

Because of that, it should only be used in controlled development environments unless additional safeguards are added.

Recommended safeguards include:

- authentication
- authorisation
- input validation
- file write restrictions
- approval workflows before applying changes
- audit logging
- sandboxed execution
- branch-based source control isolation

## Related Files

- CustomisationTools
- CustomisationController
- CustomisationControllerTest
- CustomisationAgentServiceTest
- customise.html

## Summary

CustomisationAgentService is the central service that coordinates application customisation.

It accepts a specification and acceptance criteria, attempts to use Spring AI with tool callbacks, and falls back to a deterministic urgent-task workflow if ChatClient.Builder is unavailable. If AI execution encounters an error or returns a null response, the service logs an error and halts execution without triggering the fallback.
