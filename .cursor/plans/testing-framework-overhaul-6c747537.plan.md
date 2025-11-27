<!-- 6c747537-6494-49ed-80b7-a2cda54945c8 552ea546-0cdc-4050-a2a3-3970b7ae1f1f -->

# Plan: Enhance Backend Testing Framework

## Goal

Establish a comprehensive, best practice-based framework for backend Java/Spring
Boot testing (JUnit 4 + Mockito + Spring Test) that ensures consistency, aligns
with Spring Framework official documentation and best practices, and integrates
seamlessly with SDD and TDD workflows.

## Current State Analysis

### Strengths

- Tests exist (100+ test files across modules)
- Uses JUnit 4 (consistent with codebase standard)
- Uses Mockito for unit tests (`@RunWith(MockitoJUnitRunner.class)`)
- Uses `BaseWebContextSensitiveTest` for integration tests
- Uses Testcontainers with PostgreSQL for database testing
- Uses DBUnit for test data management
- Uses MockMvc for REST controller testing
- Base test classes: `BaseTestConfig`, `BaseWebContextSensitiveTest`
- Templates exist: `JUnit4ServiceTest.java.template`,
  `WebMvcTestController.java.template`

### Issues Identified

1. **Unclear test slicing strategy** - No clear guidance on when to use
   `@WebMvcTest` vs `@SpringBootTest` vs `BaseWebContextSensitiveTest`
2. **Missing @DataJpaTest guidance** - No clear patterns for DAO/repository
   layer testing
3. **Inconsistent test data management** - Some tests use builders, others use
   hardcoded values or JdbcTemplate
4. **Transaction management unclear** - Mixed use of `@Transactional`, manual
   cleanup, DBUnit
5. **No quick reference guide** - Missing quick reference for common patterns
6. **Template gaps** - Templates don't reference Testing Roadmap or Constitution
7. **Test organization unclear** - No clear file naming, package structure
   guidance
8. **Missing TDD workflow integration** - No clear guidance on test-first
   development

## Sources

- **Spring Framework Official Docs**:
  https://docs.spring.io/spring-boot/reference/testing/spring-applications.html
- Test slicing strategies (@WebMvcTest, @DataJpaTest, @SpringBootTest)
- MockMvc patterns
- @MockBean vs @Mock usage
- **Spring Boot Testing Best Practices** (web search results):
- Use test slicing when possible (faster execution)
- Use @MockBean for Spring context mocking
- Ensure test independence
- Use builders/factories for test data
- **Current Codebase**: `src/test/java/org/openelisglobal/**/*Test.java` (100+
  files)
- **Testing Roadmap**: `.specify/guides/testing-roadmap.md` (existing backend
  guidance)
- **Base Classes**: `BaseTestConfig.java`, `BaseWebContextSensitiveTest.java`

## Document Structure: What Goes Where

### 1. Testing Roadmap (`.specify/guides/testing-roadmap.md`)

**Scope**: Comprehensive technical guidance **Action**: Enhance existing Backend
Testing section

**Additions**:

- **Test Slicing Strategy Decision Tree**: When to use @WebMvcTest vs
  @DataJpaTest vs @SpringBootTest vs BaseWebContextSensitiveTest
- **@DataJpaTest Patterns**: DAO/repository layer testing with TestEntityManager
- **Transaction Management**: @Transactional patterns, rollback strategies, when
  to use manual cleanup
- **Test Data Management**: Builders/factories patterns, DBUnit usage,
  Testcontainers patterns
- **Test Organization**: File naming, package structure, test grouping
- **MockMvc Patterns**: Request/response testing, JSONPath assertions, error
  handling
- **@MockBean vs @Mock**: When to use each (Spring context vs isolated unit
  tests)
- **TDD Workflow Integration**: Red-Green-Refactor for backend
- **SDD Checkpoint Integration**: When tests must pass

### 2. Backend Testing Best Practices Quick Reference (`.specify/guides/backend-testing-best-practices.md`)

**Scope**: Quick reference guide **Action**: Create new document

**Content**:

- Test slicing decision tree
- Annotation cheat sheet (@WebMvcTest, @DataJpaTest, @SpringBootTest, @MockBean,
  @Transactional)
- MockMvc quick patterns
- Test data management quick reference
- Transaction management patterns
- Anti-patterns checklist
- TDD workflow quick reference

### 3. Backend Test Templates

**Scope**: Copy-paste ready examples **Action**: Enhance existing templates

**Files to Update**:

- `.specify/templates/testing/JUnit4ServiceTest.java.template` - Unit tests
- `.specify/templates/testing/WebMvcTestController.java.template` - Controller
  tests
- `.specify/templates/testing/DataJpaTestDao.java.template` - DAO tests (enhance
  if exists)

**Enhancements**:

- Add TDD workflow comments
- Add SDD checkpoint references
- Add test data builder examples
- Add transaction management examples
- Reference Testing Roadmap and Best Practices
- Add anti-pattern warnings

### 4. AGENTS.md

**Scope**: High-level reference **Action**: Enhance Backend Testing section

**Updates**:

- Add test slicing strategy guidance
- Reference Testing Roadmap for detailed patterns
- Reference Backend Testing Best Practices for quick reference
- Update examples to show @WebMvcTest, @DataJpaTest patterns
- Add TDD workflow reference

### 5. Plan Template (`.specify/templates/plan-template.md`)

**Scope**: SDD integration **Action**: Enhance Testing Strategy section

**Updates**:

- Add backend test slicing strategy requirements
- Reference Testing Roadmap for backend patterns
- Add @DataJpaTest requirements
- Add transaction management requirements

### 6. Tasks Template (`.specify/templates/tasks-template.md`)

**Scope**: TDD workflow integration **Action**: Enhance backend test task
examples

**Updates**:

- Add @DataJpaTest task example
- Show test slicing strategy in task descriptions
- Reference Testing Roadmap and Backend Testing Best Practices
- Add TDD workflow reminders

## Detailed Update Specifications

### Update 1: Enhance Testing Roadmap Backend Section

**File**: `.specify/guides/testing-roadmap.md` **Section**: Backend Testing
(expand existing)

**Additions**:

1. **Test Slicing Strategy Decision Tree**:

- **@WebMvcTest**: REST controllers in isolation (mocked services)
- **@DataJpaTest**: DAO/repository layer (TestEntityManager, in-memory DB)
- **@SpringBootTest**: Full integration (full application context)
- **BaseWebContextSensitiveTest**: Legacy integration tests (Testcontainers,
  DBUnit)
- Decision tree: When to use each

2. **@DataJpaTest Patterns** (NEW - currently missing):

- DAO testing with TestEntityManager
- In-memory database configuration
- Transaction rollback patterns
- Examples for OpenELIS DAO patterns

3. **Transaction Management**:

- **@Transactional**: Automatic rollback (preferred for @SpringBootTest)
- **Manual cleanup**: When @Transactional doesn't work (DBUnit, Testcontainers)
- **@Rollback(false)**: When you need to verify database state
- **Propagation.NOT_SUPPORTED**: For BaseWebContextSensitiveTest pattern

4. **Test Data Management** (enhanced):

- **Builders/Factories**: Preferred pattern (per Testing Roadmap)
- **DBUnit**: For complex test data (legacy pattern in
  BaseWebContextSensitiveTest)
- **JdbcTemplate**: For direct database operations (when needed)
- **Testcontainers**: For database integration tests
- Migration strategy: Move from hardcoded to builders

5. **Test Organization**:

- File naming: `{ServiceName}Test.java`, `{ControllerName}Test.java`,
  `{DAO}Test.java`
- Package structure: Mirror main package structure
- Test grouping: Group by layer (service, controller, dao)

6. **MockMvc Patterns** (enhanced):

- Request building patterns
- Response assertion patterns (JSONPath)
- Error response testing
- Authentication/authorization testing

7. **@MockBean vs @Mock**:

- **@MockBean**: Use in Spring context tests (@WebMvcTest, @SpringBootTest)
- **@Mock**: Use in isolated unit tests (@RunWith(MockitoJUnitRunner.class))
- Decision tree

8. **TDD Workflow Integration**:

- Red-Green-Refactor cycle for backend
- Test-first development process
- SDD checkpoint requirements

### Update 2: Create Backend Testing Best Practices Quick Reference

**File**: `.specify/guides/backend-testing-best-practices.md` (NEW)

**Structure**:

- **Test Slicing Decision Tree**: When to use each annotation
- **Annotation Cheat Sheet**: @WebMvcTest, @DataJpaTest, @SpringBootTest,
  @MockBean, @Transactional
- **MockMvc Quick Patterns**: Request/response testing
- **Test Data Management**: Builders, DBUnit, JdbcTemplate
- **Transaction Management**: Rollback patterns
- **Anti-Patterns Checklist**: What NOT to do
- **TDD Workflow**: Red-Green-Refactor quick reference

### Update 3: Enhance Backend Test Templates

**Files**:

- `.specify/templates/testing/JUnit4ServiceTest.java.template` (update existing)
- `.specify/templates/testing/WebMvcTestController.java.template` (update
  existing)
- `.specify/templates/testing/DataJpaTestDao.java.template` (enhance if exists,
  create if missing)

**Enhancements**:

1. **Header Comments**:

- Reference Testing Roadmap
- Reference Backend Testing Best Practices
- TDD workflow reminder
- SDD checkpoint reference

2. **Test Data Examples**:

- Builder pattern examples
- Factory pattern examples
- Edge case data examples

3. **Transaction Management Examples**:

- @Transactional usage
- Manual cleanup patterns
- Rollback strategies

4. **Anti-Pattern Warnings**:

- Inline comments showing what NOT to do
- Examples of anti-patterns with corrections

### Update 4: Update AGENTS.md

**File**: `AGENTS.md` **Section**: Testing Strategy > Backend Testing (enhance
existing)

**Updates**:

1. **Test Slicing Strategy** subsection:

- When to use @WebMvcTest (controller isolation)
- When to use @DataJpaTest (DAO testing)
- When to use @SpringBootTest (full integration)
- When to use BaseWebContextSensitiveTest (legacy integration)
- Reference Testing Roadmap for detailed patterns

2. **Testing Resources** (enhance existing):

- Add Backend Testing Best Practices guide link
- Update Testing Roadmap reference

### Update 5: Update Plan Template

**File**: `.specify/templates/plan-template.md` **Section**: Testing Strategy
(enhance existing)

**Updates**:

1. **Backend Unit Tests** checkbox:

- Reference Testing Roadmap for backend patterns
- Reference Backend Testing Best Practices for quick reference
- Add test slicing strategy requirements
- Add @DataJpaTest requirements

2. **Test Data Management**:

- Add backend builder/factory patterns
- Add transaction management patterns

### Update 6: Update Tasks Template

**File**: `.specify/templates/tasks-template.md` **Section**: Test Tasks
(enhance existing)

**Updates**:

1. **@DataJpaTest Task Example** (add if missing):

- Show DAO testing pattern
- Reference Testing Roadmap
- Reference Backend Testing Best Practices

2. **Test Task Structure** (enhance existing):

- Add test slicing strategy notes
- Add references to Testing Roadmap and Backend Testing Best Practices
- Add note about using templates

## Migration Strategy

**Priority Order** for improving existing tests (incremental - don't break
existing tests):

1. **Standardize test data management** - Move to builders/factories
2. **Clarify test slicing strategy** - Document when to use each annotation
3. **Add @DataJpaTest patterns** - For DAO testing (currently missing)
4. **Standardize transaction management** - Use @Transactional where possible
5. **Add TDD workflow comments** - Document test-first approach
6. **Update test organization** - Consistent naming and structure

**Migration Checklist**:

- [ ] Test data uses builders/factories (not hardcoded values)
- [ ] Test slicing strategy is clear (@WebMvcTest vs @SpringBootTest vs
      BaseWebContextSensitiveTest)
- [ ] DAO tests use @DataJpaTest when appropriate
- [ ] Transaction management is clear (@Transactional vs manual cleanup)
- [ ] Tests focus on behavior, not implementation
- [ ] Test names follow convention:
      `test{MethodName}_{Scenario}_{ExpectedResult}`
- [ ] Tests grouped logically by layer
- [ ] TDD workflow documented in test comments

## Files to Create/Update

### Files to Update

1. `.specify/guides/testing-roadmap.md` - Enhanced Backend Testing section
2. `AGENTS.md` - Backend Testing section with test slicing strategy
3. `.specify/templates/testing/JUnit4ServiceTest.java.template` - Comprehensive
   examples
4. `.specify/templates/testing/WebMvcTestController.java.template` - Enhanced
   patterns
5. `.specify/templates/testing/DataJpaTestDao.java.template` - Create or enhance
6. `.specify/templates/plan-template.md` - Backend test requirements
7. `.specify/templates/tasks-template.md` - Backend test task examples

### Files to Create

1. `.specify/guides/backend-testing-best-practices.md` - NEW: Quick reference
   guide

## Implementation Order

1. **Enhance Testing Roadmap** - Comprehensive Backend Testing section with all
   best practices
2. **Create Backend Testing Best Practices Reference** - Quick reference guide
3. **Enhance/Update Test Templates** - Comprehensive examples with all patterns
4. **Update AGENTS.md** - Backend Testing section with references
5. **Update Plan Template** - Backend test requirements
6. **Update Tasks Template** - Backend test task examples

## Success Criteria

- [ ] Testing Roadmap provides comprehensive backend guidance with all best
      practices
- [ ] Backend Testing Best Practices quick reference guide created
- [ ] Test templates include all patterns with examples
- [ ] AGENTS.md references Testing Roadmap and Backend Testing Best Practices
- [ ] Plan template includes backend test requirements
- [ ] Tasks template includes backend test task examples
- [ ] Test slicing strategy clearly documented
- [ ] @DataJpaTest patterns documented
- [ ] Transaction management patterns clear
- [ ] TDD workflow integrated
- [ ] SDD checkpoint integration documented
- [ ] Migration strategy documented for existing tests

## Notes for Implementation

1. **Testing Roadmap**: Include comprehensive code examples. This is the primary
   technical reference.
2. **Template Updates**: Show examples, don't just describe. Make it copy-paste
   ready.
3. **Migration Strategy**: Focus on incremental approach - don't break existing
   tests.
4. **Test Slicing**: Document decision tree clearly - this is a common source of
   confusion.
5. **@DataJpaTest**: This is currently missing from guidance - add comprehensive
   patterns.
6. **Transaction Management**: Clarify when to use @Transactional vs manual
   cleanup.
7. **JUnit 4**: Maintain JUnit 4 focus (not JUnit 5) - this is the codebase
   standard.
8. **BaseWebContextSensitiveTest**: Document this legacy pattern and when to use
   it vs @SpringBootTest.

### To-dos

- [ ] Significantly expand Testing Roadmap Cypress section with all best
      practices
- [ ] Update Cypress template with comprehensive examples
