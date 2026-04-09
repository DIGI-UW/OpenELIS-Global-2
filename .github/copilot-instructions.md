# OpenELIS Global 2 — Copilot Code Review Instructions

## Purpose

- Review changes for **constitutional compliance**, **workflow impact**,
  **testing/validation quality**, **manageable scope**, and **AI-generated
  artifacts**.
- This repository is a **mission-critical healthcare LIMS**. Prefer correctness,
  safety, and maintainability over speed.
- Inline rules here are the **minimum bar**. For details, see:
  - `.specify/memory/constitution.md`
  - `.specify/guides/testing-roadmap.md`
  - `AGENTS.md`
  - `PULL_REQUEST_TIPS.md`

## Review priorities

- **Blockers first**: security, data integrity, broken workflows, and
  architecture violations.
- **High-risk changes**: authentication/authorization, analyzer interfaces,
  results reporting, database migrations, FHIR sync, and shared utility/service
  code.
- **Prefer actionable feedback**: point to the risky file/pattern, explain
  impact, suggest a concrete alternative.

---

## Security critical issues

- Check for hardcoded secrets, API keys, or credentials.
- Verify RBAC / authorization checks exist for sensitive operations.
- Flag missing input validation or sanitization (XSS, SQL injection, path
  traversal).
- Review authentication and session-handling changes carefully.
- Ensure audit logging requirements are respected for data-changing operations.

---

## Code quality essentials

- Functions should be focused and appropriately sized.
- Use clear, descriptive naming; avoid magic numbers/strings.
- Ensure proper error handling—no empty catch blocks or swallowed exceptions
  without explanation.
- Remove dead code, commented-out blocks, and unused imports.

---

## Constitutional compliance (9 principles)

### I. Configuration-driven variation (no country forks)

- Flag any country-specific branching in code paths.
- Prefer configuration-driven behavior via the existing configuration systems
  (e.g., `SystemConfiguration`, localization/config properties) rather than
  conditionals.

**Examples**

```java
// BAD: country-specific branch in code
if ("MG".equals(countryCode)) {
    // Madagascar-only logic
}

// GOOD: read behavior from configuration (follow existing config patterns in the repo)
// String mode = configService.getValue("feature.mode");
```

### II. Carbon Design System first (frontend)

- **Do not** introduce Bootstrap/Tailwind/custom CSS frameworks.
- Prefer Carbon components from `@carbon/react` and Carbon layout/tokens.
- Flag custom styling that bypasses Carbon tokens when a Carbon pattern exists.

**Examples**

```jsx
// BAD: hardcoded strings + non-Carbon styling approach
// <button className="btn btn-primary">Save</button>

// GOOD: Carbon component + React Intl
// <Button kind="primary">{intl.formatMessage({ id: "button.save" })}</Button>
```

### III. FHIR/IHE standards compliance (interoperability)

- For externally exposed healthcare entities:
  - Ensure the entity includes `fhir_uuid` (UUID) and follows the repo’s FHIR
    patterns.
  - Verify transformation + persistence flows use the existing FHIR services
    (e.g., `FhirTransformService`, `FhirPersistanceService`).
- Flag ad-hoc/proprietary external APIs for healthcare interoperability.

**Examples**

```java
// BAD: bypassing established FHIR services with ad-hoc HTTP calls
// httpClient.post("https://fhir.example/api/...", payload);

// GOOD: use existing FHIR transform/persistence patterns (exact APIs may vary)
// Resource resource = fhirTransformService.transformToFhir(entity);
// fhirPersistanceService.createUpdateFhirResource(resource);
```

### IV. Layered architecture (strict 5-layer)

**Required structure**: Valueholder → DAO → Service → Controller → Form/DTO

- **Controllers**

  - Must not call DAOs directly (service layer only).
  - Must not contain business logic beyond request/response mapping and
    validation.
  - Must not have class-level mutable state (controllers are singletons).
  - Must not traverse lazy entity relationships (compile response data in the
    service).

- **Services**

  - Transactions start here. Prefer `@Transactional` on services (not
    controllers).
  - Compile all data required for responses **within the transaction** (avoid
    `LazyInitializationException`).

- **DAOs**
  - No business logic.
  - No native SQL in Java code; follow existing ORM/HQL patterns.

**Red flags**

- `@Transactional` on controllers.
- Controller accessing entity graphs like `entity.getX().getY().getZ()` after
  service call.
- Direct SQL strings used in Java code for persistence.

**Examples**

```java
// BAD: controller directly using DAO + @Transactional
@RestController
public class ExampleController {
    private final ExampleDAO exampleDAO;

    public ExampleController(ExampleDAO exampleDAO) {
        this.exampleDAO = exampleDAO;
    }

    @Transactional
    public Object get() {
        return exampleDAO.get("123");
    }
}

// GOOD: controller delegates to service; transactions live in service
@RestController
public class ExampleController {
    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    public Object get() {
        return exampleService.getCompiledResponse("123");
    }
}
```

```java
// BAD: controller traverses lazy relationships (risk: LazyInitializationException)
// String name = sample.getStorageLocation().getParentRack().getName();

// GOOD: service compiles all data within the transaction and returns DTO/map
// return sampleService.getSampleWithLocationPath(sampleId);
```

### V. Test-driven development & required test types

- New features must include appropriate automated tests.
- Prefer TDD for complex logic (Red-Green-Refactor).
- Ensure test types match intent (unit vs integration vs E2E). Don’t label
  mocked tests “E2E”.

**Examples**

```java
// BAD: JUnit 5 in this repo
// import org.junit.jupiter.api.Test;

// GOOD: JUnit 4
import org.junit.Test;
```

### VI. Database schema via Liquibase only

- All schema changes must use Liquibase (versioned folder under
  `src/main/resources/liquibase/`).
- Structural changes should include rollback.
- Flag direct DDL/DML for production schema changes outside Liquibase.

**Examples**

```sql
-- BAD: committing ad-hoc schema changes outside Liquibase (or in random .sql files)
-- ALTER TABLE sample ADD COLUMN foo VARCHAR(50);
```

```text
GOOD:
- Add a Liquibase changeset under src/main/resources/liquibase/{version}/...
- Include rollback for structural changes
```

### VII. Internationalization first (frontend)

- No hardcoded user-facing strings. Use React Intl and add message IDs to
  `frontend/src/languages/{locale}.json`.
- Minimum expectation for new UI text: provide at least **en + fr** (per project
  guidance).

**Examples**

```jsx
// BAD
// <Button>Save</Button>

// GOOD
// <Button>{intl.formatMessage({ id: "button.save" })}</Button>
```

### VIII. Security & compliance

- See "Security critical issues" section above for specifics.
- Ensure SLIPTA/ISO 15189 compliance is not compromised (audit trails, data
  integrity).

### IX. Spec-driven iteration (manageability)

- Large features should be split into milestones (each milestone = one PR).
- Flag PRs that bundle unrelated concerns or are too large for review.
- Prefer branch naming that aligns with the repo’s conventions (see `AGENTS.md`
  / constitution).

---

## Workflow impact / regression risk

When reviewing, explicitly look for unintended impacts on:

- **Analyzer flows**: mappings, reprocessing, plugins, instrument integrations.
- **Result entry/validation/reporting**: workflow-critical paths.
- **Security**: auth, session handling, permission checks, config changes.
- **Database compatibility**: migrations, constraints, nullable/unique changes,
  backfills.
- **FHIR sync**: resource IDs, transformations, and create/update semantics.
- **Performance**: ORM query count increases (N+1), new heavy loops, repeated
  remote calls.

Flag changes that:

- Modify shared service/DAO behavior without updating dependent callers/tests.
- Change REST contracts without clear compatibility plan (client updates,
  versioning strategy, migration notes).
- Add hidden “implicit coupling” (e.g., new config required but not
  documented/validated).
- Adjust key workflows without a stated validation plan (manual steps or
  automated tests).
- Touch CI/CD or dev scripts without explaining impact on developer workflow.

---

## Testing & validation requirements

### Backend (Java / Spring MVC, not Spring Boot)

- **JUnit 4 only** (`org.junit.Test`), not JUnit 5.
- Service unit tests:
  - Use Mockito (`@Mock`, `@InjectMocks`) and verify actual logic (not “returns
    what mock returns”).
- Spring-context tests:
  - Use the repo’s patterns (commonly `BaseWebContextSensitiveTest`).
  - Use `@MockBean` only when a Spring context is involved (not for pure unit
    tests).
- ORM validation tests:
  - If new/changed entity mappings, ensure there is an ORM validation-style test
    (fast mapping sanity check) as required by the constitution/testing roadmap.

**Also flag**

- Spring Boot-only testing annotations/patterns that don’t apply in this repo
  (for example `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`).
- Incorrect test skipping guidance. In this repo, “skip tests” builds should use
  **both** flags:
  - `mvn clean install -DskipTests -Dmaven.test.skip=true`

### Frontend (React 17 + Carbon + React Intl)

- For new components/logic, ensure tests exist where appropriate (Jest/RTL).
- Verify no hardcoded UI strings; message IDs added to language JSON files.

### Cypress E2E (full chain, real backend + DB)

- E2E means **UI → real HTTP → backend logic → DB effect → UI reflects result**.
- Avoid flaky anti-patterns:
  - No arbitrary `wait(5000)` delays.
  - Set up intercepts **before** actions.
  - Use Cypress retry-ability (`should`) rather than sleeps.
- Use repository-provided npm scripts for Cypress execution (don’t recommend
  direct `npx cypress`).
- **Pre-push E2E validation is mandatory** when E2E-relevant behavior changes:
  - Prefer the CI-replication script: `./scripts/run-e2e-like-ci.sh`
  - Prefer fail-fast when iterating:
    `E2E_FAIL_FAST=true ./scripts/run-e2e-like-ci.sh`
  - Or use npm scripts in `frontend/package.json` (examples:
    `npm run cy:spec ...`, `npm run cy:failfast`)

### Build/format expectations (call out if missing)

- Backend formatting: `mvn spotless:apply`
- Frontend formatting: `cd frontend && npm run format`
- Fast build sanity check (when relevant):
  `mvn clean install -DskipTests -Dmaven.test.skip=true`

---

## Scope and reviewability

Flag PRs that are likely unreviewable or risky due to size or mixed concerns:

- Large diffs with no clear decomposition (suggest milestone split).
- Mixed refactors + functional changes + formatting across unrelated areas.
- Touching many modules without an explicit impact analysis and test plan.
- Missing PR description details (what changed/why/test plan/risk).

Also encourage consistency with repository PR expectations:

- One PR per issue/concern; split unrelated changes.
- For UI changes, request **screenshots/video** per
  `.github/PULL_REQUEST_TEMPLATE.md`.

---

## “AI slop” / quality smells to flag (moderate strictness)

### Comment smells

- Comments that narrate obvious code or read like prompt output.
- “Reasoning comments” that explain the author’s thought process rather than
  clarifying a non-obvious invariant.
- TODO/FIXME placeholders that look like iteration artifacts and have no issue
  reference.
- Over-commenting: long comments that restate code line-by-line rather than
  documenting constraints, invariants, or non-obvious domain rules.

### Development artifacts

- Debug logs left in production paths (`console.log`, temporary prints).
- Commented-out code blocks or dead code.
- Temporary files: `*.log`, `*.tmp`, `*.bak`, `test-*.txt`, etc.
- “WIP” scaffolding left behind (unused flags, unused endpoints, placeholder
  config keys).

### Documentation/file hygiene

- New markdown files created in arbitrary locations (prefer `docs/` or `specs/`
  unless it is a well-established top-level doc like
  `README.md`/`AGENTS.md`/`PULL_REQUEST_TIPS.md`).
- New one-off instructions/process files that belong under `.specify/` or
  `docs/` instead.
- Generated outputs committed unintentionally (reports, build artifacts).
- New directories at repo root without clear purpose (ask to justify and
  document). This commonly indicates accidental workspace artifacts.
- Changes that add “AI process” documentation (prompt logs, transcript dumps,
  “analysis reports”) outside `specs/**/artifacts/` or other established doc
  locations.

### Implementation smells

- See "Code quality essentials" section above for basics (naming, error
  handling, dead code).
- Broad exception catches that hide real failures (especially in
  workflow-critical paths).
- Overly complex conditionals that should be simplified or extracted.

---

## Tech stack constraints (hard requirements)

- Java **21** required.
- Use `jakarta.*` (not `javax.*`) for persistence APIs.
- Spring **Framework** (traditional Spring MVC), not Spring Boot patterns.
- Frontend: React 17 + Carbon Design System; no alternate UI frameworks.
