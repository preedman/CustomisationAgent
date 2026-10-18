# CustomisationController

## Overview

CustomisationController is the REST controller that exposes the customisation API for the application.

It allows a user interface, script, or external HTTP client to submit a customisation request containing a specification and acceptance criteria.

The controller passes that request to CustomisationAgentService, which is responsible for executing the customisation workflow.

## Purpose

The purpose of this controller is to provide an HTTP entry point into the customisation agent.

Without this controller, customisation requests would need to be triggered directly from Java code or tests.

With this controller, users can submit requests over HTTP using a JSON payload.

## Package

The controller is part of the agent package.

This keeps it grouped with the customisation service and customisation tools.

## Spring Role

CustomisationController is annotated as a REST controller.

This means Spring will detect it during component scanning and expose its request handling methods as HTTP endpoints.

It is also mapped to the base path:

/api/customisations

All endpoints in this controller are relative to that base path.

## Main Endpoint

The controller exposes one main endpoint:

POST /api/customisations

This endpoint accepts a customisation request body and returns a customisation response body.

## Request Format

The endpoint expects a JSON request body with the following fields:

- specification
- acceptanceCriteria

The specification field describes what change should be made to the application.

The acceptanceCriteria field describes how the change should be verified.

Example request body:

{
"specification": "Add support for a task description.",
"acceptanceCriteria": "The task creation screen should include a task description field. The description should be saved and returned by the task API."
}

## Response Format

The endpoint returns a JSON response body with the following field:

- result

The result field contains the message returned by CustomisationAgentService.

Example success response body (HTTP 200 OK):

{
"result": "CUSTOMISATION_COMPLETED: Task description support added and tests passed."
}

Example failure response body (HTTP 500 Internal Server Error):

{
"result": "ERROR: AI customisation completed without writing any files to the review folder."
}

## Dependencies

CustomisationController depends on:

- CustomisationAgentService

The dependency is provided through constructor injection.

## Constructor

The constructor accepts a CustomisationAgentService instance.

Spring uses this constructor to inject the service when creating the controller.

Constructor injection is preferred because it makes the dependency explicit and allows the controller to be tested with a mocked service.

## customise Method

The customise method handles HTTP POST requests to the controller base path.

Because the controller base path is /api/customisations, the full endpoint path is:

POST /api/customisations

## Method Input

The method accepts a CustomisationRequest object from the HTTP request body.

Spring automatically deserialises the incoming JSON payload into this request object.

The request contains:

- specification
- acceptanceCriteria

## Method Behaviour

The method logs the incoming specification and acceptance criteria, then calls:

customisationAgentService.executeWithAi(specification, acceptanceCriteria)

The service returns a result string.

The controller wraps that result string in a CustomisationResponse object.

If the result starts with `ERROR`, the controller logs a warning and returns HTTP 500 Internal Server Error with the response body. This covers cases where the AI call throws an exception, or the model finishes without staging any files in the review folder.

Otherwise, it returns HTTP 200 OK with the response body.

## Method Output

The method returns:

ResponseEntity<CustomisationResponse>

A successful request returns HTTP 200 OK.

A failed customisation returns HTTP 500 Internal Server Error.

In both cases the JSON response contains the customisation result, so the client can display the error message returned by the service.

## CustomisationRequest Record

CustomisationRequest is a Java record used to represent the incoming request body.

It contains two fields:

- specification
- acceptanceCriteria

Because it is a record, Java automatically provides:

- a constructor
- accessor methods
- equals
- hashCode
- toString

The generated accessor methods are:

- specification()
- acceptanceCriteria()

## CustomisationResponse Record

CustomisationResponse is a Java record used to represent the outgoing response body.

It contains one field:

- result

Because it is a record, Java automatically provides:

- a constructor
- accessor method
- equals
- hashCode
- toString

The generated accessor method is:

- result()

## Example HTTP Request

A client can call the endpoint with a POST request.

Example:

curl -X POST http://localhost:8080/api/customisations \
-H "Content-Type: application/json" \
-d '{
"specification": "Add support for a task description.",
"acceptanceCriteria": "The UI should allow the user to enter a task description. The description should be stored in the database and returned by the task API. Tests should pass."
}'

## Example HTTP Response

A successful response may look like this:

{
"result": "CUSTOMISATION_COMPLETED: Task description support added successfully."
}

The request is synchronous. Because the local model performs several tool calls before returning, the response can take some time.

## Example Use Case

A user opens the customisation screen in the browser.

The user enters:

Specification:

Add support for a task description.

Acceptance criteria:

The task creation screen should include a task description field. The description should be saved in the database and returned by the task API. The Maven test suite should pass.

When the user submits the form, the browser sends a POST request to:

/api/customisations

The controller receives the request, calls the customisation service, and returns the service result. The proposed changes and a `DOCUMENTATION.md` file are staged in the `review` folder for the user to inspect.

## Relationship With The UI

The static customisation page can submit requests to this controller.

The page gathers the specification and acceptance criteria from text areas and sends them as JSON to the API.

The response result can then be displayed to the user.

## Relationship With CustomisationAgentService

This controller does not perform customisation itself.

It delegates the work to CustomisationAgentService.

This keeps the controller focused on HTTP concerns and keeps customisation orchestration inside the service layer.

## Relationship With CustomisationTools

This controller does not directly use CustomisationTools.

CustomisationTools is used by CustomisationAgentService during the actual customisation workflow.

This separation keeps responsibilities clear:

- CustomisationController handles HTTP requests and responses.
- CustomisationAgentService coordinates the customisation process.
- CustomisationTools performs file, code, and test operations.

## Testing

This controller can be tested using a web MVC test.

`CustomisationControllerTest` uses a WebMvcTest for CustomisationController with a mocked CustomisationAgentService.

The success test:

- submits a POST request to /api/customisations with a JSON request body
- verifies that the response status is 200 OK
- verifies that the JSON response contains the expected result
- verifies that CustomisationAgentService.executeWithAi was called with the expected specification and acceptance criteria

The failure test (`shouldReturnServerErrorWhenCustomisationFails`):

- mocks the service to return an `ERROR: ...` result
- verifies that the response status is 500 Internal Server Error
- verifies that the JSON response contains the error message

## Validation Considerations

The current controller accepts the request body and passes values directly to the service.

Possible improvements include validating that:

- specification is present
- specification is not blank
- acceptanceCriteria is present
- acceptanceCriteria is not blank
- the request body is valid JSON
- the request body does not exceed an acceptable size

Validation could be added using Jakarta Bean Validation annotations and request validation.

## Error Handling Considerations

CustomisationAgentService reports failures by returning a result string starting with `ERROR` rather than throwing. The controller detects this prefix, logs a warning, and returns HTTP 500 Internal Server Error with the error message in the `result` field.

If the service does throw an exception, Spring will handle it using default error handling unless a custom exception handler is added.

Relying on a string prefix is simple but fragile, because any change to the service's error text could change the HTTP status.

Possible improvements include:

- returning a structured error response with a status field
- replacing the string prefix check with a status enum or exception from the service
- adding a global exception handler
- mapping validation errors to HTTP 400 Bad Request

## Security Considerations

This endpoint can trigger application customisation.

This results in files being staged in the review folder, project files being read by the AI model, and potentially tests being executed. The original source files are not changed.

Because of this, the endpoint should be protected before being used outside a local development environment.

Recommended safeguards include:

- authentication
- authorisation
- request validation
- rate limiting
- audit logging
- environment restrictions
- disabling the endpoint in production
- approval workflow before applying changes
- sandboxed execution

## API Design Notes

The endpoint path uses the /api prefix to distinguish it from static pages and task-specific endpoints.

The plural resource name customisations indicates that the endpoint accepts customisation requests.

The endpoint uses POST because submitting a customisation request may change application state and may trigger file changes.

## Limitations

Current limitations include:

- no request validation
- no authentication
- no custom error response format beyond the HTTP 500 status and the error text
- error detection depends on the `ERROR` prefix of the service result
- no asynchronous job tracking
- no request ID
- no customisation history
- no progress reporting
- no cancellation endpoint
- no distinction between successful completion and fallback completion beyond the result text

## Possible Future Improvements

Possible future improvements include:

- add request validation using Jakarta Bean Validation
- add a CustomisationStatus enum
- return a richer response object
- include request ID in the response
- execute customisations asynchronously
- provide a GET endpoint to check customisation status
- provide a customisation history endpoint
- add authentication and authorisation
- add role-based access control
- add structured error handling
- add OpenAPI documentation
- add endpoint tests for invalid requests
- add endpoint tests for exceptions thrown by the service
- add correlation IDs for tracing

## Suggested Documentation File Name

This documentation can be saved as:

documentation/CustomisationController.md

## Summary

CustomisationController provides the REST API entry point for customising the application.

It accepts a specification and acceptance criteria in JSON format, delegates execution to CustomisationAgentService, and returns the resulting customisation message to the client, with HTTP 200 OK on success and HTTP 500 Internal Server Error when the service reports an error.

It is intentionally simple and acts as the bridge between the web/API layer and the customisation agent service.
