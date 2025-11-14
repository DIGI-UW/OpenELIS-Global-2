# Cypress E2E Testing Amendment Plan

## Problem Statement

Current Cypress E2E tests have deficiencies:

1. Tests don't follow Cypress best practices (arbitrary waits, incorrect
   intercept timing)
2. Tests are messy and hard to maintain
3. Tests need to be run in smaller chunks individually
4. Browser console logging should be enabled for debugging
5. Video recording should be disabled by default (performance)
6. Post-run review of console logs and screenshots is required but not enforced

## Document Structure: What Goes Where

### 1. Constitution (`.specify/memory/constitution.md` - Section V.5)

**Scope**: Universal principles that apply to ALL features  
**Style**: Functional requirements, not technical implementation

**Additions**:

- **Test Execution Workflow**: Run tests individually during development (not
  full suite)
- **Browser Console Logging**: MUST be enabled and reviewed after each run
- **Video Recording**: MUST be disabled by default (performance)
- **Post-Run Review**: Mandatory review of console logs and screenshots
- **Anti-Patterns**: Arbitrary waits, missing element readiness checks, testing
  implementation details

### 2. Spec Document (`specs/001-sample-storage/spec.md`)

**Scope**: Feature-specific E2E test scenarios  
**Style**: What to test (user scenarios), not how to test

**Additions**:

- **E2E Test Scenarios**: Explicit test scenarios for each user story (P1, P2A,
  P2B)
- **Test Execution Requirements**: Reference to constitution requirements

### 3. Research Document (`specs/001-sample-storage/research.md`)

**Scope**: Technical examples and patterns  
**Style**: Code examples showing best practices

**Additions**:

- **Update Example Test Pattern**: Show intercept timing, retry-ability, element
  readiness
- **Update Run Commands**: Individual test execution examples
- **Add Best Practices Reference**: Link to constitution Section V.5

### 4. Plan Document (`specs/001-sample-storage/plan.md`)

**Scope**: Implementation details  
**Style**: Technical configuration and patterns

**Additions**:

- **Cypress Configuration**: Technical implementation details
- **Test Refactoring Patterns**: How to fix existing anti-patterns

### 5. Templates (`.specify/templates/`)

**Scope**: Reference guidelines, not duplicate them

**Additions**:

- **plan-template.md**: Reference Constitution V.5 in test coverage checklist
- **tasks-template.md**: Add E2E test task example with reference to V.5

## Amendment Content

### Constitution Section V.5 Enhancements

**New Subsection: Test Execution Workflow**

**MANDATE**: E2E tests MUST be executed individually in small, manageable chunks
during development. Full test suite runs are for CI/CD only.

**Requirements**:

- Run individual test files during development
- Maximum 5-10 test cases per execution during development
- Full suite runs only in CI/CD pipeline or pre-merge validation

**Rationale**: Running tests individually provides faster feedback, easier
debugging, and prevents cascading failures from masking root causes.

**Enhanced: Browser Console Logging**

**MANDATE**: Browser console logging MUST be enabled for all test executions.
Console logs provide critical debugging information and MUST be reviewed after
each test run.

**Requirements**:

- Browser console logging enabled by default (Cypress captures automatically)
- Review browser console logs in Cypress UI after each test run
- Check for JavaScript errors, API failures, and unexpected warnings

**Rationale**: Console logs reveal underlying issues (network failures,
JavaScript errors) that may not be visible in test output alone.

**New Subsection: Post-Run Review Requirements**

**MANDATE**: After each test execution (especially failures), developers MUST
review console logs and screenshots before marking tests as passing or filing
bug reports.

**Review Checklist**:

1. **Console Logs**: Review browser console in Cypress UI for errors, failed API
   requests, warnings
2. **Screenshots**: Review failure screenshots in `cypress/screenshots/` for UI
   state at failure point
3. **Test Output**: Review Cypress command log for execution order and timeouts

**Enhanced: Anti-Patterns Section**

Add the following anti-patterns (validated against Cypress documentation):

- ❌ **Arbitrary time delays** - Use Cypress's built-in waiting mechanisms
  instead
- ❌ **Missing element readiness checks** - Wait for elements to be
  visible/ready before interaction
- ❌ **Testing implementation details** - E2E tests should validate user
  workflows, not internal component logic
- ❌ **Not leveraging Cypress retry-ability** - Use `.should()` assertions that
  automatically retry
- ❌ **Setting up intercepts after actions** - Intercepts must be set up before
  actions that trigger them

**Note**: Technical implementation details (code examples) belong in plan.md and
research.md, not in the constitution.

### Spec Document Additions

**New Section: E2E Test Scenarios**

For each user story, add explicit E2E test scenarios (what to test, not how):

**User Story P1 - Basic Storage Assignment**:

- E2E Test: "should assign sample to location via cascading dropdowns"
- E2E Test: "should assign sample to location via type-ahead autocomplete"
- E2E Test: "should assign sample to location via barcode scan"
- E2E Test: "should create new location inline during assignment"
- E2E Test: "should display capacity warnings at 80%, 90%, 100%"

**User Story P2A - Sample Search and Retrieval**:

- E2E Test: "should search samples by accession number"
- E2E Test: "should filter samples by storage room"
- E2E Test: "should filter samples by multiple criteria"
- E2E Test: "should clear filters and show all samples"

**User Story P2B - Sample Movement**:

- E2E Test: "should move single sample between locations"
- E2E Test: "should prevent moving to occupied position"
- E2E Test: "should move multiple samples with auto-assigned positions"
- E2E Test: "should allow manual position editing during bulk move"

**Test Execution Requirements**:

- Tests MUST follow Constitution Section V.5 requirements (individual execution,
  console logging, post-run review)

### Plan Document Additions

**Enhanced Section: Cypress Configuration**

Technical implementation details (code examples, configuration):

```javascript
module.exports = defineConfig({
  video: false, // MUST be disabled by default
  screenshotOnRunFailure: true, // MUST be enabled
  defaultCommandTimeout: 30000,
  e2e: {
    setupNodeEvents(on, config) {
      // Browser console logging enabled by default
      return config;
    },
    baseUrl: "https://localhost",
  },
});
```

**New Section: Test Refactoring Patterns**

Technical patterns for fixing existing tests:

1. **Intercept Timing**: Set up `cy.intercept()` before actions that trigger API
   calls
2. **Retry-Ability**: Use `.should()` assertions instead of `.then()` callbacks
3. **Element Readiness**: Wait for elements to be visible before interaction
4. **State Verification**: Use proper assertions (contain.text, not.exist, etc.)

### Research Document Updates

**Update Example Test Pattern**: Show best practices with intercept timing,
retry-ability, element readiness (technical code examples)

**Update Run Commands**: Add individual test execution examples

**Add Best Practices Reference**: Link to Constitution Section V.5

### Template Updates

**plan-template.md**: Update Constitution Check to reference V.5 for E2E
requirements

**tasks-template.md**: Add E2E test task example with reference to Constitution
V.5

## Implementation Order

1. **Update Constitution** (Section V.5) - Universal principles (functional, not
   technical)
2. **Update Templates** - Reference guidelines
3. **Update Research Document** - Technical examples
4. **Update Spec Document** - Feature-specific scenarios (what to test)
5. **Update Plan Document** - Implementation details (how to test)
6. **Update cypress.config.js** - Apply configuration requirements

## Files to Update

1. `.specify/memory/constitution.md` - Section V.5 (functional principles)
2. `.specify/templates/plan-template.md` - Reference V.5
3. `.specify/templates/tasks-template.md` - Add E2E task example
4. `specs/001-sample-storage/research.md` - Technical examples
5. `specs/001-sample-storage/spec.md` - Test scenarios (what to test)
6. `specs/001-sample-storage/plan.md` - Implementation details (how to test)
7. `frontend/cypress.config.js` - Apply configuration

## Validation Against Cypress Documentation

Our proposed practices align with Cypress official best practices:

✅ **Test Independence**: Running tests individually aligns with Cypress
recommendation for independent tests  
✅ **Console Logging**: Cypress automatically captures browser console (built-in
feature)  
✅ **Video Recording**: Disabling video improves performance (Cypress
recommendation)  
✅ **Avoid Hardcoded Waits**: Using Cypress's built-in waiting mechanisms (our
anti-pattern)  
✅ **Post-Run Review**: Regular review maintains test suite health (Cypress
recommendation)  
✅ **Element Readiness**: Waiting for elements aligns with Cypress
retry-ability  
✅ **Intercept Timing**: Setting up intercepts before actions is Cypress best
practice
