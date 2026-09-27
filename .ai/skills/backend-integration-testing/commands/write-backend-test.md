# Command: `/write-backend-test`

## Description

Generates a new service-layer integration test following the OpenELIS Global 2
standards.

## User Input

```text
$ARGUMENTS
```

## Parameters

- `service`: The service to test (for example, `PatientService`).
- `module`: The module package name (e.g., `patient`).

## Implementation Logic

1. **Locate the target class** to understand its dependencies and methods.
2. **Identify dependencies** that need to be mocked (external) vs. autowired
   (internal).
3. **Generate the test class** using
   `.ai/skills/backend-integration-testing/templates/integration-test-template.java.template`.
   Replace every placeholder and the failing sentinel with working code.
4. **Create or reuse a DBUnit XML dataset** under `src/test/resources/testdata/`
   and load it in `@Before`.
5. **Run the specific test** with `mvn test -Dtest=<GeneratedIntegrationTest>`
   and report the result.

## Example Usage

`/write-backend-test service=PatientService module=patient`
