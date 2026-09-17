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
2. **Identify the real internal path** and external effects to isolate. Inspect
   `AppTestConfig` substitutions. Choose rollback or committed transaction
   ownership; concurrency/commit tests need explicit cleanup of affected data.
3. **Generate the test class** using
   `.ai/skills/backend-integration-testing/templates/integration-test-template.java.template`.
   Replace every placeholder and the failing sentinel with working code.
4. **Create owned initial records** through real services where appropriate.
   If a DBUnit dataset is needed, load it from `src/test/resources/testdata/` in
   `@Before`. Account for cascading truncation outside the XML; fixture reload
   does not replace teardown of committed state. Use services for the business
   transition under test, not fixture SQL.
5. **Run the specific test** with `mvn test -Dtest=<GeneratedIntegrationTest>`
   and report the result.

## Example Usage

`/write-backend-test service=PatientService module=patient`
