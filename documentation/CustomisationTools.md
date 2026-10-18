# CustomisationTools

## Overview

CustomisationTools is a Spring-managed component that exposes file and test execution operations to the customisation agent.

It provides the practical capabilities that allow the customisation workflow to inspect the project, update files, run tests, and report completion.

The class is part of the agent package and is designed to be registered as a tool provider for Spring AI.

## Purpose

The purpose of CustomisationTools is to give the customisation agent controlled access to project-level operations while enforcing a human-review workflow.

Instead of overwriting original files in place, all code additions and updates are written to a dedicated `review` directory.

The class allows the agent to:

- clean or clear existing content in the review directory before writing new content
- read files from the project or review folder
- apply targeted code changes to files and stage the updated files in the review directory
- write new or replacement files directly to the review directory
- write markdown documentation in the review directory describing what was changed and what the new code does
- run the Maven test suite
- complete the customisation workflow with a summary message

These operations allow a human developer to review the proposed changes and accompanying documentation before implementing them in the main codebase.

## Spring Role

CustomisationTools is annotated as a Spring component.

This means Spring can discover it automatically during component scanning and inject it into other classes, such as the customisation service.

Because its functional file and verification methods are annotated with `@Tool`, they can be made available to a Spring AI chat client, while directory lifecycle operations (such as `cleanReviewDirectory`) are managed directly by the orchestration service.

## Project Root and Review Directory

The class stores two paths:

- the project root, which original files are read from
- the review directory, which all new and modified files are written to

There are three constructors:

1. A default constructor, which uses the current working directory as the project root.
2. A constructor that accepts a custom project root. The review directory defaults to `review` under that root.
3. A constructor that accepts both a project root and a review directory. A relative review directory is resolved against the project root; an absolute one is used as given.

Spring uses the default constructor, so in the running application the review directory is `<working directory>/review`.

The two-argument constructor is useful for tests, because tests can read from the real project while staging output in a temporary directory, avoiding any change to the real `review` folder.

## Path Safety

Before reading or writing a file, the class resolves the requested relative path and normalises it.

- Reads are resolved against the project root and must stay inside it (`resolvePath`).
- Writes are resolved against the review directory and must stay inside it (`resolveReviewPath`).

This prevents path traversal outside the project or review directory. For example, a request attempting to access `../some-external-file` is rejected with an IllegalArgumentException, which the tool method catches and returns as an error string.

Because all write tools go through `resolveReviewPath`, the agent cannot write to the original project files.

## Logging

The class uses SLF4J logging.

It logs successful file writes and code updates.

It also logs exceptions when file reading, file writing, code replacement, or test execution fails.

The log output helps diagnose customisation failures.

## Methods

CustomisationTools exposes methods for review directory maintenance as well as tools callable by the customisation agent.

## cleanReviewDirectory

cleanReviewDirectory()

The cleanReviewDirectory method removes all existing files and subdirectories from the review folder if it exists, ensuring stale content from previous customisation runs is not mixed with new content. This is managed directly by the orchestration service prior to executing customisations to ensure a clean state for human review.

### Behaviour

The method:

- checks whether the review directory exists
- if it exists, traverses and deletes all files and subdirectories inside the review folder, leaving the review directory itself in place
- clears the writable / DOS read-only flag on a path if a first delete attempt fails, then retries
- logs a warning for any individual path that still cannot be deleted, and continues
- returns a success message indicating the review folder content was removed, or that the folder did not exist

If an exception occurs during traversal, it logs the error and returns an error message starting with `ERROR cleaning review directory`.

`cleanReviewFolder()` and `clearReviewDirectory()` are aliases that call `cleanReviewDirectory()`.

This method is not annotated with `@Tool`, so the AI model cannot call it.

### Typical Use

`CustomisationAgentService` calls this method at the start of both the AI workflow and the deterministic fallback workflow, before any new content or documentation is written, so the review folder only ever contains the output of the latest run.

## hasReviewFiles

hasReviewFiles()

The hasReviewFiles method inspects the review folder and determines whether any regular files are currently staged inside it.

### Behaviour

The method:

- checks whether the review directory exists
- if it exists, recursively traverses the directory tree using `Files.walk`
- returns `true` if at least one regular file exists within the directory tree, `false` otherwise
- logs any inspection errors and returns `false` safely if an exception occurs

### Typical Use

Used by `CustomisationAgentService` after AI execution to verify that files were actually written to disk before reporting customisation completion.

## readFile

readFile(String relativePath)

The readFile method reads the entire content of a file.

The path must be relative to the project root.

Example relative path:

src/main/java/com/reedmanit/CustomisationAgent/task/Task.java

### Behaviour

The method:

- resolves the relative path against the project root
- if the file does not exist there, checks for the same path in the review folder
- reads the file as UTF-8 text
- returns the file content as a string

The review folder fallback lets the agent read back new files it has staged that do not exist in the original project.

If the file does not exist in either location, it returns an error message.

If an exception occurs, it logs the error and returns an error message.

### Typical Use

The customisation agent uses this method before making changes, so it can inspect the current state of a source file, HTML file, configuration file, or test file.

## applyCodeChange

applyCodeChange(String relativePath, String targetBlock, String replacementBlock)

The applyCodeChange method replaces an exact block of text in a source file and writes the modified file to the separate `review` directory, keeping the original project file unchanged.

The path must be relative to the project root.

### Parameters

relativePath is the file to update.

targetBlock is the exact existing text to find.

replacementBlock is the new text that should replace the target block.

### Behaviour

The method:

- resolves the target path in the review folder and the original path in the project root
- uses the staged copy in the review folder if one exists, otherwise the original file, so repeated changes to the same file build on each other
- reads the file as UTF-8 text
- normalises Windows line endings to Unix-style line endings
- normalises the target block and replacement block in the same way
- checks whether the file contains the target block
- replaces the target block with the replacement block
- ensures parent directories exist in the review folder
- writes the updated content to the review folder
- returns a success message indicating the file was updated in the review folder

If the file does not exist, it returns an error message.

If the target block cannot be found, it returns an error message.

If an exception occurs, it logs the error and returns an error message.

### Typical Use

The customisation agent can use this method to make targeted modifications (such as adding entity fields or UI elements) staged in the review directory for human verification.

The current system prompt in CustomisationAgentService directs the model to use `writeFile` with full file content instead, because small local models often fail to reproduce the exact target block that `applyCodeChange` requires. The tool remains available to the model.

## writeFile

writeFile(String relativePath, String content)

The writeFile method writes content to a file inside the separate `review` folder without modifying or overwriting existing project files.

If parent directories in the review folder do not exist, they are created automatically.

### Parameters

relativePath is the destination file path relative to the project root.

content is the full content to write into the review file.

### Behaviour

The method:

- resolves the target path under the review directory
- creates parent directories if required
- writes the supplied content as UTF-8 text
- logs a successful write
- returns a success message indicating the file was written to the review folder

If an exception occurs, it logs the error and returns an error message.

### Typical Use

The customisation agent uses this method to stage full new files or complete updated files in the review folder for human inspection. It is the write tool the system prompt tells the model to use.

The path mirrors the original project layout, so `src/main/java/.../Task.java` is written to `review/src/main/java/.../Task.java`. A leading `review/` or `./review/` in the path is stripped, so files are not nested in `review/review/`.

## writeDocumentation

writeDocumentation(String relativePath, String content)

The writeDocumentation method creates a Markdown documentation file in the review directory explaining what has been changed and what the new code does.

### Parameters

relativePath is the filename or relative path (e.g., `DOCUMENTATION.md`). If null or blank, it defaults to `DOCUMENTATION.md`.

content is the Markdown text documenting the changes and functionality.

### Behaviour

The method:

- resolves the documentation file path under the review directory
- creates parent directories if required
- writes the documentation content as UTF-8 Markdown text
- logs a successful write
- returns a success message

### Typical Use

The agent calls this tool after preparing code modifications to produce comprehensive documentation for human review before changes are applied.

## executeTestSuite

executeTestSuite(String testClass)

The executeTestSuite method runs Maven tests and returns a summary of the result.

It can run either the full test suite or a specific test class.

### Parameters

testClass is optional.

If testClass is empty or blank, the method runs all tests.

If testClass contains a class name, the method runs Maven with that specific test class.

### Maven Command Selection

The method detects whether the application is running on Windows.

On Windows, it prefers:

mvnw.cmd

If the Maven wrapper command is not present, it falls back to:

mvn.cmd

On non-Windows systems, it prefers:

./mvnw

If the Maven wrapper is not present, it falls back to:

mvn

### Behaviour

The method:

- builds the correct Maven command
- sets the working directory to the project root
- starts the Maven process
- captures combined standard output and error output
- waits up to 120 seconds for completion
- destroys the process if it times out
- checks the exit code
- checks whether the output contains BUILD SUCCESS
- extracts important summary lines
- returns a concise test execution summary

### Returned Summary

The returned summary includes:

- the process exit code
- a status of TESTS PASSED or TESTS FAILED
- Maven test summary lines
- BUILD SUCCESS or BUILD FAILURE lines
- ERROR lines

### Timeout

The test process has a 120 second timeout.

If the process does not finish within that time, it is forcibly stopped and an error message is returned.

### Typical Use

The deterministic fallback workflow calls this method to run `TaskRepositoryTest`.

The tool is also available to the AI model, but the current system prompt does not ask the model to run tests.

Note that the tests run against the original project source, not the staged files in the review folder, so they do not verify the proposed changes.

## completeCustomisation

completeCustomisation(String summary)

The completeCustomisation method marks the customisation process as complete.

It accepts a human-readable summary of what was changed and how it was verified.

### Behaviour

The method:

- logs the customisation summary
- returns the summary prefixed with CUSTOMISATION_COMPLETED

### Typical Use

The customisation agent should call this as its final tool call, after it has staged its changes and written the documentation.

The returned message can be shown to the user through the customisation API or web page.

## getProjectRoot

getProjectRoot()

The getProjectRoot method returns the project root path currently used by this tool class.

### Typical Use

This is useful for tests or diagnostics.

It allows other code to confirm which root directory file operations are using.

## getReviewDir

getReviewDir()

The getReviewDir method returns the absolute, normalised review directory path that write operations use.

### Typical Use

Tests use this to locate and assert on staged files.

## resolvePath

resolvePath(String relativePath)

The resolvePath method is a private helper used when reading original project files.

It is responsible for safely converting a relative path into an absolute normalised path under the project root.

### Behaviour

The method:

- resolves the relative path against the project root
- normalises the result
- checks that the resolved path still starts with the project root
- returns the resolved path

If the resolved path would escape the project root, it throws an IllegalArgumentException.

### Security Role

This method is an important safety control.

It prevents callers from using paths such as:

../some-external-file

to read files outside the project.

## resolveReviewPath

resolveReviewPath(String relativePath)

The resolveReviewPath method is a private helper used by every operation that writes a file, and by the review folder fallback in readFile and applyCodeChange.

### Behaviour

The method:

- converts backslashes to forward slashes
- strips a leading `review/` or `./review/` prefix, since models often include it
- resolves the remaining path against the review directory
- normalises the result
- checks that the resolved path still starts with the review directory
- returns the resolved path

If the resolved path would escape the review directory, it throws an IllegalArgumentException.

### Security Role

This method is what guarantees that the agent can only write inside the review folder and never to the original project files.

## Error Handling Strategy

The public tool methods do not usually throw exceptions back to the caller.

Instead, they catch exceptions, log the details, and return error strings.

This is useful for AI tool calls because the agent receives a readable result and can decide how to proceed.

Typical error responses include:

- ERROR: File not found
- ERROR reading file
- ERROR: Target block not found
- ERROR applying code change
- ERROR writing file
- ERROR writing documentation
- ERROR cleaning review directory
- ERROR executing tests
- ERROR: Test execution timed out

## Character Encoding

All file reads and writes use UTF-8.

This helps keep behaviour consistent across operating systems.

## Line Ending Handling

The applyCodeChange method normalises CRLF line endings to LF line endings before matching and replacing text.

This makes exact block replacement more reliable across Windows, macOS, and Linux.

However, after replacement, the updated file is written using the normalised LF line endings.

## Tool Annotation Usage

The following methods are annotated as tools and exposed to the AI model:

- readFile
- applyCodeChange
- writeFile
- writeDocumentation
- executeTestSuite
- completeCustomisation

cleanReviewDirectory (and its aliases), hasReviewFiles, getProjectRoot and getReviewDir are not tools. They are called directly by Java code.

The annotations describe what each tool does and what each parameter means.

This allows Spring AI to expose these Java methods as callable tools to the chat model.

The tool descriptions are important because they guide the AI model when deciding which operation to use.

## Typical Customisation Workflow

A typical customisation workflow using this class is:

1. The orchestrator cleans the review folder with cleanReviewDirectory.
2. The agent reads relevant files with readFile.
3. The agent writes updated files to the review folder using writeFile (or applyCodeChange).
4. The agent writes DOCUMENTATION.md to the review folder using writeDocumentation.
5. The agent completes the workflow using completeCustomisation.
6. The orchestrator confirms that files were staged using hasReviewFiles.

## Example Workflow

For a feature request such as adding a task description, the customisation agent, following the order in the system prompt, would:

1. readFile `task/Task.java`, then writeFile the entity with a new taskDescription field.
2. readFile `task/TaskController.java`, then writeFile it if it needs changes.
3. readFile `static/index.html`, then writeFile the page with a Task Description textarea and updated JavaScript that submits the description.
4. writeDocumentation `DOCUMENTATION.md` describing the changes.
5. completeCustomisation with a short summary.

A human then reviews the files in `review/` and copies the approved changes into `src/`.

## Design Strengths

CustomisationTools has several useful design characteristics:

- simple public API
- clear tool descriptions
- UTF-8 file handling
- project-root based path resolution for reads
- review-directory based path resolution for writes, so original files are never overwritten
- path traversal protection
- support for temporary project and review directories in tests
- Maven wrapper support
- test timeout handling
- readable success and error messages

## Design Limitations

The class also has some limitations:

- applyCodeChange requires exact text matches
- writeFile replaces the entire staged file
- executeTestSuite runs against the original source, not the staged review files
- test execution timeout is fixed at 120 seconds
- Maven output summary is basic
- errors are returned as strings rather than structured objects
- there is no tool for applying approved review files to the project; this is a manual step
- the review folder is cleared on every run, so unapplied changes from a previous run are lost
- no source control integration is provided
- no concurrency protection is provided if multiple customisations run at once, which would share the same review folder

## Possible Future Improvements

Possible improvements include:

- returning structured result objects instead of plain strings
- generating a diff between the original and staged files
- adding a step to apply approved review files to the project
- running tests against the staged changes, for example in a copy of the project
- using a separate review folder per run instead of clearing a shared one
- adding Git branch or commit integration
- adding configurable test timeouts
- adding support for running different Maven goals
- adding support for Gradle projects
- adding more flexible patch-based editing
- adding validation for file types that can be staged
- adding an allowlist of readable directories
- adding audit logs for every file change
- adding concurrency locking around write operations

## Security Considerations

This class can read any file under the project root, write files into the review folder, delete the review folder's contents, and execute Maven tests.

That means it should be treated as a powerful development-time tool.

It should not be exposed in an uncontrolled production environment.

Recommended safeguards include:

- authentication before customisation requests can be submitted
- authorisation to restrict who can trigger code changes
- input validation for specifications and acceptance criteria
- restricting which files the agent can read, as it can currently read configuration and other sensitive files under the project root
- human approval before applying staged changes (currently enforced by the manual copy step)
- audit logging
- sandboxed execution
- source control isolation
- disabling the tool in production profiles

## Testing Considerations

The constructors that accept a project root and a review directory are useful for testing.

Tests can create a temporary directory with JUnit `@TempDir` and pass it into CustomisationTools, either as the project root or as the review directory.

`CustomisationAgentServiceTest`, for example, uses `new CustomisationTools(Paths.get("."), reviewDir)` so it reads the real project files but stages output in a temporary directory, never touching the real `review` folder.

This allows tests to verify read, write, and code replacement behaviour without modifying the real project.

Important behaviours to test include:

- writing a file to the review folder and reading it back
- confirming the original file is unchanged after a write
- applying a code change successfully
- returning an error when a file does not exist
- rejecting path traversal attempts for reads and writes
- stripping a leading `review/` prefix from write paths
- cleaning the review directory
- hasReviewFiles before and after a write
- completing a customisation with a summary
- handling missing target blocks
- creating parent directories when writing files

## Operational Considerations

When executeTestSuite is called, it starts an external Maven process.

This means the runtime environment must have either:

- the Maven wrapper scripts available, or
- Maven installed and available on the system path

The process also needs permission to execute the Maven wrapper.

On Unix-like systems, the Maven wrapper may need executable permissions.

## Summary

CustomisationTools is the operational toolset used by the customisation agent.

It provides controlled read access to the project, review folder management, staging of code changes and documentation in the review folder, Maven test execution, and customisation completion reporting.

It is central to the customisation workflow because it turns the agent's planned changes into staged files that a human can review before they are applied to the project.
