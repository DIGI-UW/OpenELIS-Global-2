# Example request: MenuService active menus

Use the backend integration testing skill to write an integration test for
`MenuService.getAllActiveMenus()`.

Use the existing DBUnit fixture at `src/test/resources/testdata/menu.xml` and
the real Spring service. Verify the exact `elementId` values returned and prove
that every returned menu is active. Do not mock `MenuService` or `MenuDAO`, and
do not modify production code.

## Generated code

[GeneratedMenuServiceIntegrationTest.java](GeneratedMenuServiceIntegrationTest.java)
is the complete test produced for this request.

## OpenELIS sources used

- `src/main/java/org/openelisglobal/menu/service/MenuService.java`
- `src/main/java/org/openelisglobal/menu/service/MenuServiceImpl.java`
- `src/main/java/org/openelisglobal/menu/valueholder/Menu.java`
- `src/test/java/org/openelisglobal/BaseWebContextSensitiveTest.java`
- `src/test/resources/testdata/menu.xml`

## Verification

Generated from PR #3578 head `3201a7db2dbca99488cdd2eb7820fa97f0bbe6e7`.

For verification, the generated file was placed at
`src/test/java/org/openelisglobal/menu/GeneratedMenuServiceIntegrationTest.java`
and run with:

```bash
mvn test -Dtest=GeneratedMenuServiceIntegrationTest
```

Verification result on August 27, 2026, using Java 21:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
