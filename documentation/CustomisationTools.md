# CustomisationTools

## Overview

CustomisationTools is a Spring-managed component that exposes file and test execution operations to the customisation agent.

It provides the practical capabilities that allow the customisation workflow to inspect the project, update files, run tests, and report completion.

The class is part of the agent package and is designed to be registered as a tool provider for Spring AI.

## Purpose

The purpose of CustomisationTools is to give the customisation agent controlled access to project-level operations.

The class allows the agent to:

- read files from the project
- replace exact code blocks in files
- write or overwrite files
- run the Maven test suite
- complete the customisation workflow with a summary message

These operations are used by the customisation service when applying customer-requested application changes.

## Spring Role

CustomisationTools is annotated as a Spring component.

This means Spring can discover it automatically during component scanning and inject it into other classes, such as the customisation service.

Because its public methods are annotated as tools, they can also be made available to a Spring AI chat client.

## Project Root

The class stores a project root path.

All file operations are resolved relative to this project root.

There are two constructors:

1. A default constructor.
2. A constructor that accepts a custom project root.

The default constructor uses the current working directory as the project root.

The custom project root constructor is useful for tests, because tests can provide a temporary directory and avoid modifying the real project files.

## Path Safety

Before reading or writing a file, the class resolves the requested relative path against the project root.

It normalises the resolved path and checks that the result still starts with the project root.

This prevents path traversal outside the project directory.

For example, a request attempting to access files outside the project root should be rejected.

This is important because the customisation agent has file write capability.

## Logging

The class uses SLF4J logging.

It logs successful file writes and code updates.

It also logs exceptions when file reading, file writing, code replacement, or test execution fails.

The log output helps diagnose customisation failures.

## Tool Methods

CustomisationTools exposes several tool methods.

These methods are intended to be called by the customisation agent during an automated customisation workflow.

## readFile

readFile(String relativePath)

The readFile method reads the entire content of a file.

The path must be relative to the project root.

Example relative path:

src/main/java/com/reedmanit/CustomisationAgent/task/Task.java

### Behaviour

The method:

- resolves the relative path against the project root
- checks whether the file exists
- reads the file as UTF-8 text
- returns the file content as a string

If the file does not exist, it returns an error message.

If an exception occurs, it logs the error and returns an error message.

### Typical Use

The customisation agent uses this method before making changes, so it can inspect the current state of a source file, HTML file, configuration file, or test file.

## applyCodeChange

applyCodeChange(String relativePath, String targetBlock, String replacementBlock)

The applyCodeChange method replaces an exact block of text in a file with another block of text.

The path must be relative to the project root.

### Parameters

relativePath is the file to update.

targetBlock is the exact existing text to find.

replacementBlock is the new text that should replace the target block.

### Behaviour

The method:

- resolves the target file path
- checks whether the file exists
- reads the file as UTF-8 text
- normalises Windows line endings to Unix-style line endings
- normalises the target block and replacement block in the same way
- checks whether the file contains the target block
- replaces the target block with the replacement block
- writes the updated content back to the file
- returns a success message

If the file does not exist, it returns an error message.

If the target block cannot be found, it returns an error message.

If an exception occurs, it logs the error and returns an error message.

### Important Limitation

This method requires an exact text match.

If the spacing, indentation, or surrounding text changes, the target block may not be found.

For larger or more flexible changes, writing the full file may be simpler, but it carries a higher risk of overwriting unrelated changes.

### Typical Use

The customisation agent can use this method to make small, targeted edits such as:

- adding a field to an entity
- adding a line to a JSON payload
- updating a response message
- inserting a small block of HTML
- changing a specific method body

## writeFile

writeFile(String relativePath, String content)

The writeFile method writes content to a file.

If the file already exists, it is overwritten.

If the parent directories do not exist, they are created automatically.

### Parameters

relativePath is the destination file path relative to the project root.

content is the full content to write into the file.

### Behaviour

The method:

- resolves the target path
- creates parent directories if required
- writes the supplied content as UTF-8 text
- logs a successful write
- returns a success message

If an exception occurs, it logs the error and returns an error message.

### Typical Use

The customisation agent can use this method to:

- create new files
- overwrite generated files
- create tests
- create documentation
- replace an entire source file when exact block replacement is not suitable

### Caution

Because this method overwrites the entire file, it should be used carefully.

For existing source files, the agent should first read the current file and ensure that important content is preserved.

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

The customisation agent uses this method during the verification step after applying code changes.

It helps confirm whether the modified application still builds and passes tests.

## completeCustomisation

completeCustomisation(String summary)

The completeCustomisation method marks the customisation process as complete.

It accepts a human-readable summary of what was changed and how it was verified.

### Behaviour

The method:

- logs the customisation summary
- returns the summary prefixed with CUSTOMISATION_COMPLETED

### Typical Use

The customisation agent should call this after it has finished making changes and running verification.

The returned message can be shown to the user through the customisation API or web page.

## getProjectRoot

getProjectRoot()

The getProjectRoot method returns the project root path currently used by this tool class.

### Typical Use

This is useful for tests or diagnostics.

It allows other code to confirm which root directory file operations are using.

## resolvePath

resolvePath(String relativePath)

The resolvePath method is a private helper used by all file operations.

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

to read or write files outside the project.

## Error Handling Strategy

The public tool methods do not usually throw exceptions back to the caller.

Instead, they catch exceptions, log the details, and return error strings.

This is useful for AI tool calls because the agent receives a readable result and can decide how to proceed.

Typical error responses include:

- ERROR: File not found
- ERROR reading file
- ERROR applying code change
- ERROR writing file
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

Several methods are annotated as tools.

These annotations describe what each tool does and what each parameter means.

This allows Spring AI to expose these Java methods as callable tools to the chat model.

The tool descriptions are important because they guide the AI model when deciding which operation to use.

## Typical Customisation Workflow

A typical customisation workflow using this class is:

1. Read relevant files with readFile.
2. Decide what needs to change.
3. Apply targeted changes using applyCodeChange or write updated files using writeFile.
4. Run tests using executeTestSuite.
5. Complete the workflow using completeCustomisation.

## Example Workflow

For a feature request such as adding a task description, the customisation agent might:

1. Read the task entity file.
2. Add a taskDescription field.
3. Read the task controller tests.
4. Add or update tests for the new field.
5. Read the task creation HTML page.
6. Add a Task Description textarea.
7. Update JavaScript to submit the description.
8. Run Maven tests.
9. Return a completion summary.

## Design Strengths

CustomisationTools has several useful design characteristics:

- simple public API
- clear tool descriptions
- UTF-8 file handling
- project-root based path resolution
- path traversal protection
- support for temporary test directories
- Maven wrapper support
- test timeout handling
- readable success and error messages

## Design Limitations

The class also has some limitations:

- applyCodeChange requires exact text matches
- writeFile overwrites entire files
- test execution timeout is fixed at 120 seconds
- Maven output summary is basic
- errors are returned as strings rather than structured objects
- no approval step is required before writing files
- no rollback mechanism is provided
- no source control integration is provided
- no concurrency protection is provided if multiple customisations run at once

## Possible Future Improvements

Possible improvements include:

- returning structured result objects instead of plain strings
- adding a dry-run mode
- adding a diff preview before writes
- adding backup file creation before overwriting
- adding Git branch or commit integration
- adding configurable test timeouts
- adding support for running different Maven goals
- adding support for Gradle projects
- adding more flexible patch-based editing
- adding validation for file types that can be modified
- adding an allowlist of editable directories
- adding audit logs for every file change
- adding rollback support
- adding concurrency locking around write operations

## Security Considerations

This class can read and write project files and execute Maven tests.

That means it should be treated as a powerful development-time tool.

It should not be exposed in an uncontrolled production environment.

Recommended safeguards include:

- authentication before customisation requests can be submitted
- authorisation to restrict who can trigger code changes
- input validation for specifications and acceptance criteria
- directory allowlists for file modifications
- read-only mode for review
- human approval before applying changes
- audit logging
- sandboxed execution
- source control isolation
- disabling the tool in production profiles

## Testing Considerations

The constructor that accepts a project root is useful for testing.

Tests can create a temporary directory and pass it into CustomisationTools.

This allows tests to verify read, write, and code replacement behaviour without modifying the real project.

Important behaviours to test include:

- writing and reading a file
- applying a code change successfully
- returning an error when a file does not exist
- rejecting path traversal attempts
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

It provides controlled file access, code replacement, file writing, Maven test execution, and customisation completion reporting.

It is central to the customisation workflow because it turns the agent's planned changes into real project modifications and verification steps.
