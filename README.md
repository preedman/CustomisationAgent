# CustomisationAgent

CustomisationAgent is a Spring Boot application that demonstrates an AI-assisted software customisation workflow.

The application includes a simple task tracker and a customisation interface where a user can submit a specification and acceptance criteria. The customisation agent can then use those details to update the application and verify the result.

## Features

- Create tasks through a simple web UI
- Store tasks using Spring Data JPA
- H2 in-memory database support
- REST API for task creation and retrieval
- REST API for submitting customisation requests
- Web screen for entering customisation specifications
- Spring AI integration using Ollama
- Tool-based customisation workflow for reading files, writing files, applying code changes, running tests, and completing customisations

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
- 


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

Example response:

    {
      "result": "CUSTOMISATION_COMPLETED: ..."
    }

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

This project uses Spring AI with Ollama.

A suitable local model is:

    ollama pull qwen2.5-coder:7b

The application can be configured in application.properties with:

    spring.ai.ollama.chat.model=qwen2.5-coder:7b

Make sure Ollama is running before starting the application.

## Running Tests

Run all tests with:

    ./mvnw test

On Windows:

    mvnw.cmd test

Run a specific test class with:

    ./mvnw test -Dtest=CustomisationControllerTest

## H2 Console

The H2 console is available at:

    http://localhost:8080/h2-console

Default JDBC URL:

    jdbc:h2:mem:taskdb

Default username:

    sa

Default password is blank.

## Project Structure

    src
      main
        java
          com.reedmanit.CustomisationAgent
            agent
              CustomisationAgentService
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

Because the customisation agent can modify source files and run tests, it should not be exposed publicly without authentication, authorisation, validation, and other safety controls.



