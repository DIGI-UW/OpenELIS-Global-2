# Command: `/write-backend-test`

## Description

Generates a new backend integration test for a given service or controller,
following the OpenELIS Global 2 standards.

## Parameters

- `service`: The name of the service or controller to test (e.g.,
  `PatientService`).
- `module`: The module package name (e.g., `patient`).

## Implementation Logic

1. **Locate the target class** to understand its dependencies and methods.
2. **Identify dependencies** that need to be mocked (external) vs. autowired
   (internal).
3. **Generate the test class** using the `integration-test-template.java`.
4. **Draft a DBUnit XML dataset** structure required for the test.
5. **Provide the Maven command** to run the specific test.

## Example Usage

`/write-backend-test service=PatientService module=patient`
