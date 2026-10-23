# Building an AI-Powered Software Customisation Agent with Spring AI and Spring Boot

In software engineering, adapting applications to unique customer specifications is one of the most common yet repetitive operational demands. Minor modifications—such as adding a field to an entity, exposing it across REST endpoints, updating persistence layers, adjusting web forms, and writing release documentation—consume valuable engineering hours. When developers are bogged down by repetitive boilerplate, overall system velocity decreases and time-to-delivery for high-leverage features suffers.

**CustomisationAgent** demonstrates a practical, high-utility solution: an autonomous software customisation agent embedded within a Spring Boot application. Powered by **Spring AI** and local large language models (LLMs) via **Ollama**, the agent translates natural language customer requirements and acceptance criteria into staged, verified, and documented application changes—all while enforcing a strict **human-in-the-loop, review-first architecture**.

---

## The Core Concept: Review-First AI Customisation

A primary challenge of utilizing AI for automated code modification is safety. Directly letting an LLM overwrite production or development source trees introduces risks of regressions, syntax errors, and architectural drift.

To maximize utility while eliminating the risk of unverified code changes, CustomisationAgent implements a **Review-First Architecture**:

1. **Sandboxed Staging**: The AI agent never directly modifies files under `src/`. All new or modified files are staged in an isolated `review/` directory mirroring the project structure.
2. **Automated Documentation**: Alongside code changes, the agent generates a comprehensive `DOCUMENTATION.md` detailing every file modified, field mappings, and architectural impact.
3. **Automated Verification**: The agent can invoke project test suites (via Maven) during customisation to ensure functional validity.
4. **Clean State Management**: The `review/` directory is automatically cleared at the beginning of each run, ensuring developers only inspect clean, isolated diffs for the current request.
5. **Human Approval Gate**: A developer inspects the staged diffs and generated documentation, approves the changes, and merges them into `src/`.

```
+-------------------------------------------------------------------------+
|                         Customer / Product Owner                        |
|   Submits: Specification & GIVEN/WHEN/THEN Acceptance Criteria          |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                  CustomisationAgent (Spring AI + Ollama)                |
|                                                                         |
|  1. Cleans review/ directory                                            |
|  2. Reads existing files via tools (Task.java, index.html, etc.)        |
|  3. Stages modified code into review/                                   |
|  4. Writes review/DOCUMENTATION.md                                      |
|  5. Executes test suites                                                |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                      Staged review/ Directory                           |
|   - review/DOCUMENTATION.md                                             |
|   - review/src/main/java/.../Task.java                                  |
|   - review/src/main/resources/static/index.html                         |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                          Developer Review                               |
|   Inspects diffs -> Merges to src/ -> Runs tests -> Deploys             |
+-------------------------------------------------------------------------+
```

---

## Application Architecture and Tech Stack

CustomisationAgent is built on modern Java and Spring ecosystems:

- **Java 17** & **Spring Boot**: Core application framework.
- **Spring AI**: Integrates AI capabilities and orchestrates model interactions with `@Tool`-annotated Java components.
- **Ollama (`qwen3:8b`)**: Runs open-weight LLMs locally, ensuring zero API SaaS costs, low latency, and complete data privacy (source code never leaves the host environment).
- **Spring Data JPA & H2 In-Memory Database**: Provides data persistence and schema management.
- **Spring Web MVC**: Exposes REST endpoints and serves UI web screens.
- **JUnit 5 & Maven**: Handles automated testing and build orchestration.

---

## Key Components Under the Hood

### 1. The Customisation Screen & REST API

The application provides a web UI (`/customise.html`) and a REST endpoint (`POST /api/customisations`) managed by `CustomisationController`.

Users provide two structured inputs:
- **Specification**: A plain-English description of the requested functionality (e.g., adding an urgency flag or description field).
- **Acceptance Criteria**: Verifiable conditions (typically formatted in `GIVEN / WHEN / THEN` style) defining success.

```json
{
  "specification": "Add support for marking tasks as urgent.",
  "acceptanceCriteria": "GIVEN I am on the task form, WHEN I tick Urgent and submit, THEN the task is stored with urgent=true."
}
```

### 2. CustomisationAgentService: The Orchestrator

`CustomisationAgentService` is the central orchestration engine. It coordinates the lifecycle of a customisation run:

- **State Preparation**: Invokes `CustomisationTools.cleanReviewDirectory()` to wipe stale artifacts.
- **Spring AI ChatClient Configuration**: Registers customisation tools and applies a system prompt designed for tool-calling discipline.
- **Tool-Call Execution**: Small local models can sometimes narrate plans or output plain text instead of executing actions. The service enforces strict prompt rules requiring the model to execute tools sequentially (Entity -> Controller -> UI -> Documentation -> Complete).
- **Deterministic Fallback**: If no AI model is configured (such as in offline or test environments), the service falls back to `executeUrgentTaskCustomisation()`, executing an automated, deterministic customisation loop without downtime.
- **Output Validation**: Ensures that files were properly written to `review/` before reporting success.

### 3. CustomisationTools: Safe Agent Tooling

Exposed to Spring AI via the `@Tool` annotation, `CustomisationTools` provides safe file operations and test execution:

- `readFile(path)`: Reads source files relative to the project root (or staged files in `review/`).
- `writeFile(path, content)`: Writes complete replacement files strictly into the `review/` directory.
- `applyCodeChange(path, find, replace)`: Performs targeted search-and-replace edits within `review/`.
- `writeDocumentation(filename, markdown)`: Saves structured technical markdown documentation for human reviewers.
- `executeTestSuite(testClassName)`: Invokes Maven programmatically to run targeted unit/acceptance test suites.
- `completeCustomisation(summary)`: Finalizes the run with an executive summary.

Path traversal safeguards (`resolvePath` and `resolveReviewPath`) ensure that the AI agent cannot read outside the project boundaries or write outside the `review/` staging directory.

---

## End-to-End Walkthrough: Adding an "Urgent Task" Feature

Let's look at how the customisation workflow operates in practice:

1. **Submitting the Request**:
   The user opens `http://localhost:8080/customise.html` and inputs the requirement to flag high-priority tasks with an "Urgent" checkbox.

2. **Agent Execution**:
   - The agent reads `src/main/java/com/reedmanit/CustomisationAgent/task/Task.java`.
   - It writes an updated `Task.java` into `review/` containing the new `is_urgent` database column mapping, boolean getter/setter methods, and Jackson annotations.
   - It reads `src/main/resources/static/index.html` and writes an updated version into `review/` with the new checkbox and updated JavaScript payload handler.
   - It writes `review/DOCUMENTATION.md` outlining all modifications and review guidelines.
   - It executes `TaskRepositoryTest` to verify that existing data persistence contracts hold.

3. **Human Review and Merging**:
   The developer reviews `review/DOCUMENTATION.md`, verifies the changes in `review/`, copies the files into `src/`, and runs `mvn test`. The customisation is complete with minimal manual effort and maximum assurance.

---

## Maximizing Utility: Why This Pattern Matters

The architectural patterns demonstrated in **CustomisationAgent** offer substantial operational advantages:

- **Resource Optimization**: Automates repetitive coding tasks, allowing engineers to focus on high-impact architectural and business logic.
- **Cost and Privacy Efficiency**: Leveraging local LLMs through Ollama keeps operational costs at zero per token and ensures enterprise source code remains strictly on-premises.
- **Reliability and Quality Assurance**: The staged review workflow eliminates the risks of unverified AI code generation while maintaining full auditability through auto-generated markdown documentation.
- **Bridging Business and Engineering**: Stakeholders can express requirements directly using standard acceptance criteria, accelerating the feedback loop from specification to working code.

---

## Getting Started

To explore or extend the CustomisationAgent:

1. **Clone the repository** and ensure Java 17 and Maven are installed.
2. **Start Ollama** with the default model:
   ```bash
   ollama run qwen3:8b
   ```
3. **Launch the Spring Boot application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Access the Web Interfaces**:
   - Task Tracker: `http://localhost:8080/index.html`
   - Customisation Interface: `http://localhost:8080/customise.html`

CustomisationAgent demonstrates how combining Spring AI with local language models and disciplined safety patterns provides an efficient, reliable blueprint for next-generation AI-assisted software engineering.
