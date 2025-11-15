# Plan: Enhance SpecKit Artifacts for Cypress Testing Framework

## Goal

Update SpecKit artifacts and documentation (primarily in `AGENTS.md`,
`.specify/` directory, and `templates/` directory) to provide comprehensive
guidance for Cypress E2E testing best practices, ensuring both AI agents and
human developers have clear roadmaps for writing, running, evaluating, and
iterating on Cypress tests.

## Sources

- **Cypress Official Documentation**:
  https://docs.cypress.io/app/end-to-end-testing/testing-your-app
- **Profy.dev React + Cypress Guide**: https://profy.dev/article/cypress-react
- **Carbon Design System**: Component testing patterns and ARIA roles

## Pain Points to Address

1. Tests don't use testids (selector strategy)
2. Don't search DOM effectively (scoped queries, viewport, table filtering)
3. Keep recreating test data or logging in (session management, API setup)
4. Starting new sessions unnecessarily (cy.session() with cacheAcrossSpecs)
5. Overly-complicated instead of validating high-level happy path user story
6. Tests don't follow Cypress best practices (arbitrary waits, incorrect
   intercept timing)
7. Tests are messy and hard to maintain
8. Tests need to be run in smaller chunks individually
9. Browser console logging should be enabled for debugging
10. Video recording should be disabled by default (performance)
11. Post-run review of console logs and screenshots is required but not enforced

---

## Document Structure: What Goes Where

### 1. Constitution (`.specify/memory/constitution.md` - Section V.5)

**Scope**: Universal principles that apply to ALL features  
**Style**: Functional requirements, not technical implementation  
**Action**: Enhance existing Section V.5

**Additions**:

- **Test Execution Workflow**: Run tests individually during development (not
  full suite)
- **Browser Console Logging**: MUST be enabled and reviewed after each run
- **Video Recording**: MUST be disabled by default (performance)
- **Post-Run Review**: Mandatory review of console logs and screenshots
- **Anti-Patterns**: Expand with all identified anti-patterns
- **Reference to Testing Roadmap**: Mandate adherence to Testing Roadmap for
  technical details

### 2. Testing Roadmap (`.specify/guides/testing-roadmap.md`)

**Scope**: Comprehensive technical guidance for both agents and humans  
**Style**: Detailed patterns, code examples, best practices  
**Action**: Significantly expand existing Cypress section

**Additions**:

- **Selector Strategy** (priority order with examples from Cypress docs +
  profy.dev)
- **Session Management** (cy.session() patterns with OpenELIS adaptation notes)
- **Test Data Management** (API-first approach, fixture patterns)
- **DOM Query Effectiveness** (scoped queries, viewport, table filtering from
  profy.dev)
- **Test Simplification** (happy path focus from profy.dev)
- **Carbon Component Patterns** (ComboBox, DataTable, Modal, OverflowMenu)
- **Debugging Techniques** (Chrome DevTools integration from profy.dev)
- **cy.intercept() Patterns** (official Cypress pattern with aliases)
- **Migration Strategy** (how to migrate existing tests)

### 3. Cypress Best Practices Reference (`.specify/guides/cypress-best-practices.md`)

**Scope**: Quick reference guide for common patterns  
**Style**: Cheat sheets, quick examples, anti-patterns checklist  
**Action**: Create new document

**Content**:

- Selector cheat sheet (priority order)
- Session management patterns
- Test data patterns (API setup, fixtures, cleanup)
- Common Carbon component queries
- Debugging techniques
- Performance optimization
- Anti-patterns checklist
- cy.intercept() quick reference

### 4. AGENTS.md

**Scope**: High-level testing strategy reference  
**Style**: Brief overview with links to detailed docs  
**Action**: Update Testing Strategy section

**Updates**:

- Reference Testing Roadmap for detailed Cypress guidance
- Reference Constitution V.5 for E2E requirements
- Update E2E test execution commands to show individual test runs
- Add reference to Cypress Best Practices guide

### 5. Templates (`.specify/templates/`)

**Scope**: Reference guidelines, not duplicate them  
**Action**: Update existing templates

**Files to Update**:

- **`.specify/templates/testing/CypressE2E.cy.js.template`**: Comprehensive
  examples with all best practices
- **`.specify/templates/plan-template.md`**: Reference Constitution V.5 and
  Testing Roadmap in test coverage section
- **`.specify/templates/tasks-template.md`**: Add E2E test task example with
  references

---

## Detailed Update Specifications

### Update 1: Constitution Section V.5 Enhancements

**File**: `.specify/memory/constitution.md`  
**Section**: V.5 Cypress E2E Testing Best Practices

**Additions**:

1. **Test Execution Workflow** (new subsection):

   - MANDATE: Run tests individually during development
   - Maximum 5-10 test cases per execution
   - Full suite runs only in CI/CD
   - Command examples showing individual vs full suite

2. **Browser Console Logging** (enhance existing):

   - MANDATE: MUST be enabled and reviewed
   - Review checklist items
   - Rationale for debugging

3. **Post-Run Review Requirements** (new subsection):

   - MANDATE: Review console logs, screenshots, test output
   - Review checklist (3 items)
   - When to review (especially failures)

4. **Enhanced Anti-Patterns** (expand existing):

   - Add all 9 anti-patterns from review
   - Keep functional, not technical (reference Testing Roadmap for technical
     details)

5. **Reference to Testing Roadmap** (new subsection):
   - MANDATE: All practices must adhere to Testing Roadmap
   - List what Testing Roadmap provides
   - Link to `.specify/guides/testing-roadmap.md`

### Update 2: Testing Roadmap Cypress Section

**File**: `.specify/guides/testing-roadmap.md`  
**Section**: Cypress E2E Testing (expand existing section)

**Additions** (all with code examples):

1. **Selector Strategy (MANDATORY Priority)**:

   - STRICT priority order: data-testid → ARIA → Semantic → CSS
   - Examples for each priority level
   - Format guidelines for data-testid
   - Migration note: "For existing tests, gradually migrate. For new tests,
     data-testid is mandatory."

2. **Session Management (cy.session())**:

   - Official Cypress pattern with code example
   - OpenELIS adaptation note: "Adapt authentication pattern to match OpenELIS
     implementation (may use cookies, tokens, or session storage)"
   - Benefits (10-20x faster)
   - testIsolation clarification: "With cy.session(), typically keep
     testIsolation: true (cy.session handles caching)"

3. **Test Data Management (API-First)**:

   - cy.request() pattern for fast setup
   - Anti-pattern: UI-based setup (show comparison)
   - Fixture pattern with cy.intercept():
     `cy.intercept('GET', '/rest/storage/rooms', { fixture: 'rooms.json' }).as('getRooms')`
   - Smart fixture management (reference existing implementation or document
     pattern)

4. **DOM Query Effectiveness**:

   - Scoped queries pattern (profy.dev: `cy.get("main").find("li")`)
   - Table row filtering (profy.dev: use `tbody`)
   - Text-based queries with context
   - Viewport management (set before visit)
   - Chaining with .should() for retry-ability
   - Anti-patterns with examples

5. **Test Simplification (Happy Path Focus)**:

   - User workflow focus (profy.dev philosophy)
   - Good test structure example
   - What NOT to test (implementation details)
   - One happy path per feature

6. **Carbon Design System Specific Patterns**:

   - ComboBox: Explicit selection required, portal rendering
   - DataTable: Use tbody, profy.dev table pattern
   - Modal/Dialog: Portal pattern, role="dialog"
   - OverflowMenu: Portal pattern, role="menu"
   - Why Carbon needs special handling: "Carbon components use React portals,
     rendering outside normal DOM hierarchy"

7. **cy.intercept() Patterns** (new subsection):

   - Official Cypress pattern with aliases:
     ```javascript
     cy.intercept("POST", "/rest/storage/rooms").as("createRoom");
     cy.get('[data-testid="save-button"]').click();
     cy.wait("@createRoom").its("response.statusCode").should("eq", 201);
     ```
   - Timing: Set up BEFORE actions
   - Fixture usage:
     `cy.intercept('GET', '/rest/rooms', { fixture: 'rooms.json' })`

8. **Debugging Techniques**:

   - Chrome DevTools integration (profy.dev guide)
   - Common debugging scenarios
   - Reference to Constitution V.5 for post-run review (don't duplicate)

9. **Migration Strategy** (new subsection):
   - Priority order for migrating existing tests
   - Incremental approach (don't break existing tests)
   - Checklist for migration:
     1. Convert login to cy.session()
     2. Convert UI-based setup to API-based
     3. Replace CSS selectors with data-testid
     4. Add viewport management
     5. Fix intercept timing
     6. Add element readiness checks
     7. Replace arbitrary waits with .should()

### Update 3: Create Cypress Best Practices Quick Reference

**File**: `.specify/guides/cypress-best-practices.md` (NEW)

**Structure**:

- **Selector Cheat Sheet**: Priority order with quick examples
- **Session Management**: cy.session() quick pattern
- **Test Data**: API setup, fixtures, cleanup patterns
- **Carbon Components**: Quick queries for common components
- **cy.intercept() Quick Reference**: Common patterns with aliases
- **Debugging**: Chrome DevTools quick guide
- **Performance**: Session reuse, fixture caching tips
- **Anti-Patterns Checklist**: What NOT to do (quick reference)

### Update 4: AGENTS.md Testing Strategy Section

**File**: `AGENTS.md`  
**Section**: Testing Strategy (update existing)

**Updates**:

1. **E2E Tests (Cypress)** subsection:

   - Update execution commands to emphasize individual test runs
   - Add reference: "For comprehensive Cypress guidance, see
     [Testing Roadmap](.specify/guides/testing-roadmap.md)"
   - Add reference: "For quick reference, see
     [Cypress Best Practices](.specify/guides/cypress-best-practices.md)"
   - Update configuration example to show video: false, screenshotOnRunFailure:
     true
   - Add note about post-run review (reference Constitution V.5)

2. **Testing Resources** (new subsection at end of Testing Strategy):
   - Link to Testing Roadmap
   - Link to Cypress Best Practices guide
   - Link to Constitution V.5

### Update 5: Cypress Template

**File**: `.specify/templates/testing/CypressE2E.cy.js.template` (update
existing)

**Enhancements**:

1. **Header Comments**:

   - Reference to Testing Roadmap
   - Reference to Constitution V.5
   - Constitution V.5 compliance checklist

2. **Session Management Example**:

   - cy.session() pattern in before() hook
   - Note about adapting to OpenELIS authentication

3. **Test Data Setup**:

   - API-based setup example (cy.request())
   - Fixture example (cy.intercept() with fixture)
   - Cleanup in after() hook

4. **Selector Examples**:

   - data-testid (PREFERRED) with examples
   - ARIA roles (SECOND CHOICE) with examples
   - Semantic with context (USE CAREFULLY) with examples
   - CSS selectors (LAST RESORT) with warning

5. **Scoped DOM Queries**:

   - Container → find children pattern
   - Table tbody filtering pattern

6. **Viewport Management**:

   - Set viewport before visit example

7. **cy.intercept() Pattern**:

   - Set up BEFORE action
   - Use aliases
   - cy.wait('@alias') pattern

8. **Carbon Component Examples**:

   - ComboBox selection
   - DataTable interaction
   - Modal/Dialog interaction
   - OverflowMenu interaction

9. **Happy Path Focus**:

   - User workflow example
   - Comment about NOT testing implementation details

10. **Post-Run Review Checklist**:

    - Console logs review
    - Screenshots review
    - Test output review

11. **Anti-Pattern Warnings**:
    - Inline comments showing what NOT to do
    - Examples of anti-patterns with corrections

### Update 6: Plan Template

**File**: `.specify/templates/plan-template.md` (update existing)

**Updates**:

1. **Testing Strategy Section** (if exists, enhance; if not, add):

   - Reference Constitution V.5 for E2E requirements
   - Reference Testing Roadmap for detailed Cypress patterns
   - Include Cypress configuration requirements
   - Mention post-run review requirement

2. **Constitution Check Section** (enhance existing):
   - Add V.5 E2E testing compliance check
   - Reference Testing Roadmap for technical implementation

### Update 7: Tasks Template

**File**: `.specify/templates/tasks-template.md` (update existing)

**Updates**:

1. **E2E Test Task Example** (add new example task):

   - Show TDD workflow (test before implementation)
   - Reference Constitution V.5
   - Reference Testing Roadmap for patterns
   - Include post-run review as subtask
   - Show individual test execution command

2. **Test Task Structure** (enhance existing examples):
   - Add references to Testing Roadmap
   - Add note about using Cypress template

---

## Implementation Order

1. **Update Constitution** (Section V.5) - Universal principles (functional, not
   technical)
2. **Enhance Testing Roadmap** - Comprehensive Cypress section with all best
   practices
3. **Create Cypress Best Practices Reference** - Quick reference guide
4. **Update AGENTS.md** - Testing Strategy section with references
5. **Update Cypress Template** - Comprehensive examples with all patterns
6. **Update Plan Template** - Reference Constitution V.5 and Testing Roadmap
7. **Update Tasks Template** - Add E2E test task example

---

## Files to Create/Update

### Files to Update

1. `.specify/memory/constitution.md` - Section V.5 (functional principles)
2. `.specify/guides/testing-roadmap.md` - Enhanced Cypress section (technical
   guidance)
3. `AGENTS.md` - Testing Strategy section (high-level reference)
4. `.specify/templates/testing/CypressE2E.cy.js.template` - Comprehensive
   examples
5. `.specify/templates/plan-template.md` - Reference V.5 and Testing Roadmap
6. `.specify/templates/tasks-template.md` - Add E2E task example

### Files to Create

1. `.specify/guides/cypress-best-practices.md` - NEW: Quick reference guide

---

## Key Improvements from Review

### Structural Fixes

1. **Removed Duplication**: Post-run review only in Constitution V.5, referenced
   (not duplicated) in Testing Roadmap
2. **Clear Section Organization**: Each section has clear scope and style
3. **Consistent References**: All documents reference each other appropriately

### Missing Elements Added

1. **Migration Strategy**: Added to Testing Roadmap with checklist
2. **cy.intercept() Patterns**: Added official Cypress pattern with aliases
3. **Fixture Patterns**: Added profy.dev fixture pattern with cy.intercept()
4. **OpenELIS Adaptation Notes**: Added notes for cy.session() and
   authentication
5. **testIsolation Clarification**: Clarified when to use false vs true with
   cy.session()

### Clarity Improvements

1. **Carbon Component Context**: Added explanation of why Carbon needs special
   handling (portals)
2. **Selector Migration**: Added note about gradual migration for existing tests
3. **Performance Metrics**: Added expected improvements (10-20x faster with
   cy.session())

---

## Validation Against Sources

✅ **Profy.dev Patterns**: All key patterns included (scoped queries, viewport,
table filtering, Chrome DevTools, API setup, happy path)  
✅ **Cypress Official Docs**: All recommendations included (cy.session(),
data-testid, retry-ability, cy.intercept() with aliases)  
✅ **Carbon Design System**: Component-specific patterns included (portals, ARIA
roles, explicit selection)

---

## Success Criteria

- [ ] Constitution V.5 updated with all functional requirements
- [ ] Testing Roadmap provides comprehensive technical guidance with all best
      practices
- [ ] Cypress Best Practices quick reference guide created
- [ ] AGENTS.md references Testing Roadmap and Constitution V.5
- [ ] Cypress template includes all patterns with examples
- [ ] Plan template references Constitution V.5 and Testing Roadmap
- [ ] Tasks template includes E2E test task example
- [ ] All pain points addressed with documented solutions
- [ ] All documents cross-reference appropriately
- [ ] Migration strategy documented for existing tests
- [ ] Validation confirms alignment with Cypress official documentation

---

## Notes for Implementation

1. **Constitution Updates**: Keep functional, not technical. Reference Testing
   Roadmap for technical details.
2. **Testing Roadmap**: Include comprehensive code examples. This is the primary
   technical reference.
3. **Template Updates**: Show examples, don't just describe. Make it copy-paste
   ready.
4. **Cross-References**: Ensure all documents link to each other appropriately.
5. **Migration Strategy**: Focus on incremental approach - don't break existing
   tests.
