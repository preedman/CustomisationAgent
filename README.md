# CustomisationAgent

CustomisationAgent is a Spring Boot application that demonstrates an AI-assisted software customisation workflow.

The application includes a simple task tracker and a customisation interface where a user can submit a specification and acceptance criteria. The customisation agent uses a local LLM (via Spring AI and Ollama) to read the existing code and stage the proposed changes, together with markdown documentation, in a separate `review` folder for a human to check before anything is applied to the real source tree.

## Features

- Create tasks through a simple web UI
- Store tasks using Spring Data JPA
- H2 in-memory database support
- REST API for task creation and retrieval
- REST API for submitting customisation requests
- Web screen for entering customisation specifications
- Spring AI integration using Ollama with tool calling
- Review-first customisation: the agent writes changed files into a dedicated `review` directory and never overwrites the original project files
- The `review` directory is cleaned at the start of every customisation run, so it only ever contains the output of the latest request
- Generation of markdown documentation (`review/DOCUMENTATION.md`) describing what was changed and what the new code does
- Detection of runs where the model finishes without staging any files, reported as an error with HTTP 500
- Deterministic fallback orchestrator when no AI chat client is configured
- Maven test execution and customisation completion reporting

## Technology Stack

- Java 17
- Spring Boot
- Spring Data JPA
- Spring Web MVC
- Spring AI
- Ollama
- H2 Database
- Maven
- JUnit 5

## Documentation

Additional project documentation is available in the `documentation` folder:

- [CustomisationAgentService](documentation/CustomisationAgentService.md) - documents the main customisation orchestration service.
- [CustomisationController](documentation/CustomisationController.md) - documents the REST API used to submit customisation requests.
- [CustomisationTools](documentation/CustomisationTools.md) - documents the tool operations used by the customisation agent.

## Running the Application

From the project root, run:

    ./mvnw spring-boot:run

On Windows, run:

    mvnw.cmd spring-boot:run

The application should start on:

    http://localhost:8080

## Main Screens

### Task Tracker

Open:

    http://localhost:8080/index.html

This screen allows users to create tasks.

### Customisation Screen

Open:

    http://localhost:8080/customise.html

This screen allows users to enter:

- a customer specification
- acceptance criteria

The request is submitted to the customisation API.

## How the Customisation Agent Works

When a customisation request is received, `CustomisationAgentService.executeWithAi` runs the following steps:

1. **Clean the review folder** - any content left in `review/` from a previous run is deleted.
2. **Run the AI tool loop** - a Spring AI `ChatClient` is built with `CustomisationTools` registered as tools and a system prompt that tells the model to work only through tool calls, one tool call per reply, and to always read a file before changing it. The model is directed to work in this order:
    1. Read `task/Task.java` and write the updated entity.
    2. Read `task/TaskController.java` and write it if it needs changes.
    3. Read `static/index.html` and write the updated page.
    4. Write `DOCUMENTATION.md` explaining what changed and what the new code does.
    5. Call `completeCustomisation` with a short summary.
3. **Verify the output** - after the model finishes, the service checks that at least one file was written to `review/`. If nothing was staged, the request fails with `ERROR: AI customisation completed without writing any files to the review folder.`
4. **Return the result** - successful responses are prefixed with `CUSTOMISATION_COMPLETED`. Any exception during the AI call is caught and returned as `ERROR: AI customisation execution failed: ...`.

The system prompt is deliberately strict because small local models tend to narrate a plan or print tool calls as plain text instead of actually invoking the tools.

If no `ChatClient.Builder` is available (for example, Spring AI is not configured), the service falls back to `executeUrgentTaskCustomisation`, a deterministic orchestrator that stages an "Urgent Task" change to `Task.java` and `index.html`, writes documentation, runs `TaskRepositoryTest`, and completes the customisation without calling an LLM.

### Agent Tools

The following tools in `CustomisationTools` are exposed to the model:

| Tool | Description |
|------|-------------|
| `readFile` | Reads a file by path relative to the project root, falling back to the review folder if the file only exists there. |
| `writeFile` | Writes the full content of a new or modified file into the review folder, mirroring the original relative path. |
| `applyCodeChange` | Replaces an exact code block in a file and writes the result into the review folder. If the file has already been staged, the staged copy is used as the starting point. |
| `writeDocumentation` | Writes a markdown file into the review folder (defaults to `DOCUMENTATION.md`). |
| `executeTestSuite` | Runs `mvn test` (using the Maven wrapper if present), optionally for a single test class, with a 120 second timeout, and returns a summary of the results. |
| `completeCustomisation` | Marks the customisation as complete and returns `CUSTOMISATION_COMPLETED: <summary>`. |

Paths are resolved safely: any path that would escape the project root or the review folder is rejected. Paths passed with a leading `review/` prefix are normalised so files are not nested in `review/review/`.

### Reviewing Staged Changes

After a successful run, the `review` folder mirrors the project layout, for example:

    review
      DOCUMENTATION.md
      src
        main
          java
            com/reedmanit/CustomisationAgent/task
              Task.java
          resources
            static
              index.html

To apply a customisation:

1. Read `review/DOCUMENTATION.md` and inspect the staged files.
2. Copy or merge the approved files from `review/` into `src/`.
3. Run the test suite to verify the change.

Note that the next customisation request will clear the review folder, so apply or save any changes you want to keep first.

## Task API

### Create a Task

    POST /tasks

Example request:

    {
      "task_name": "Buy server space",
      "urgent": true
    }

Example response:

    {
      "id": 1,
      "task_name": "Buy server space",
      "urgent": true
    }

### Get All Tasks

    GET /tasks

### Get Task by ID

    GET /tasks/{id}

Example:

    GET /tasks/1

## Customisation API

### Submit a Customisation Request

    POST /api/customisations

Example request:

    {
      "specification": "Add support for a task description. Users should be able to enter a description when creating a task.",
      "acceptanceCriteria": "The UI should include a task description field. The description should be saved in the database and returned by the task API. The Maven test suite should pass."
    }

Example success response (`200 OK`):

    {
      "result": "CUSTOMISATION_COMPLETED: ..."
    }

If the agent fails, or finishes without writing any files to the review folder, the API returns `500 Internal Server Error` with the error in the body:

    {
      "result": "ERROR: AI customisation completed without writing any files to the review folder."
    }

The request is synchronous and can take some time, as the local model performs several tool calls before returning.

## Example Customisation Specification

Specification:

    Add support for a task description.

    Users should be able to enter a description when creating a task. The description should be optional and allow longer free-text content than the task name.

    The task description must be stored in the database with the task and returned by the task API when tasks are created, listed, or retrieved by ID.

    The task creation screen should include a Task Description text area. When the user submits the form, the description should be sent to the task creation API along with the task name and urgent flag.

Acceptance criteria:

    GIVEN I am on the task creation screen
    WHEN I view the task creation form
    THEN I should see a Task Description text area.

    GIVEN I am on the task creation screen
    WHEN I enter a task name, enter a task description, optionally tick Urgent, and submit the form
    THEN the task should be created successfully
    AND the task description should be saved with the task.

    GIVEN I create a task with a description
    WHEN the task creation API returns the saved task
    THEN the response should include the task description.

    The Task entity should include a taskDescription field mapped to a database column named task_description.

    The task API should accept both taskDescription and task_description in JSON request bodies.

    The task API should return the description as task_description in JSON responses.

    The UI should submit the description value in the JSON request body.

    The Maven test suite should pass.

## Ollama Setup

This project uses Spring AI with Ollama. The model must support tool calling.

The default model is `qwen3:8b`:

    ollama pull qwen3:8b

It is configured in `application.properties`:

    spring.ai.ollama.chat.model=qwen3:8b
    spring.ai.ollama.chat.num-ctx=16384

`num-ctx` sets the context window in tokens. Ollama's small default context truncates the system prompt and tool definitions, which stops the model from calling the tools correctly, so keep this value large enough to hold the prompt, the tool definitions and the files being read.

Alternative models (`qwen2.5-coder:7b`, `mistral`) are left commented out in `application.properties` and can be swapped in for comparison.

Make sure Ollama is running before starting the application.

## Logging

Debug logging is enabled for the application package and for Spring AI / Ollama, so the prompt sent to the model, each tool call, and the model's response are visible in the console. Adjust the `logging.level.*` entries in `application.properties` to reduce the output.

## Running Tests

Run all tests with:

    ./mvnw test

On Windows:

    mvnw.cmd test

Run a specific test class with:

    ./mvnw test -Dtest=CustomisationControllerTest

`CustomisationAgentServiceTest` constructs `CustomisationTools` with a JUnit `@TempDir` as the review folder, so running the tests never touches the real project `review` directory. These tests do not require Ollama.

## H2 Console

The H2 console is available at:

    http://localhost:8080/h2-console

Default JDBC URL:

    jdbc:h2:mem:taskdb

Default username:

    sa

Default password is blank.

## Project Structure

    documentation                 - component documentation
    review                        - staged changes from the latest customisation run
    src
      main
        java
          com.reedmanit.CustomisationAgent
            agent
              CustomisationAgentService
              CustomisationAiResponse
              CustomisationController
              CustomisationTools
            task
              Task
              TaskController
              TaskRepository
            CustomisationAgentApplication
        resources
          static
            index.html
            customise.html
          application.properties
      test
        java
          com.reedmanit.CustomisationAgent

## Notes

The customisation functionality is intended for local development and experimentation.

Although the agent only writes into the `review` folder, it can read any file under the project root and run the Maven test suite. It should not be exposed publicly without authentication, authorisation, validation, and other safety controls.
