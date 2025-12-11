# Quickstart Guide: Patient Merge Backend

**Feature**: Patient Merge Backend Implementation **Branch**:
`008-patient-merge-backend` **Date**: 2025-12-08 **Status**: Development Guide

---

## Purpose

This guide provides step-by-step instructions for implementing the patient merge
backend feature following the milestone-based development plan.

---

## Prerequisites

### Required Reading

**MUST READ FIRST** (in this order):

1. [constitution.md](../../.specify/memory/constitution.md) - OpenELIS Global
   3.0 governance
2. [AGENTS.md](../../AGENTS.md) - Agent onboarding
3. [spec.md](./spec.md) - Feature specification
4. [BLOCKERS.md](./BLOCKERS.md) - **CRITICAL**: Identifier duplication blocker
5. [plan.md](./plan.md) - Implementation plan with milestones
6. [research.md](./research.md) - Technical decisions
7. [data-model.md](./data-model.md) - Database and entity models

### Development Environment

- **Java**: 21 LTS (OpenJDK/Temurin)
- **Maven**: 3.8+
- **PostgreSQL**: 14+
- **IDE**: IntelliJ IDEA or Eclipse with Lombok plugin
- **Git**: 2.30+
- **Docker** (optional): For PostgreSQL via docker-compose

### Skills Required

- Java backend development (Spring Boot, Hibernate)
- Database design (PostgreSQL, Liquibase)
- REST API development
- Unit testing (JUnit 4, Mockito)
- FHIR R4 basics

---

## Getting Started

### 1. Clone and Setup

```bash
# Clone repository
git clone https://github.com/I-TECH-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2

# Checkout feature branch
git checkout develop
git pull origin develop
git checkout -b 008-patient-merge-backend

# Install dependencies
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### 2. Database Setup

**Option A: Using Docker Compose** (Recommended)

```bash
# Start PostgreSQL
docker-compose up -d database

# Verify connection
docker exec -it openelisglobal-database psql -U clinlims -d clinlims
```

**Option B: Local PostgreSQL**

```bash
# Create database
createdb -U postgres clinlims

# Update application properties if needed
# src/main/resources/application.properties
```

### 3. Run OpenELIS

```bash
# Backend only (for API development)
mvn spring-boot:run

# Or with frontend
cd frontend
npm install
npm start
```

**Verify**: Navigate to http://localhost:3000

---

## Development Workflow

### TDD Cycle (MANDATORY)

Follow **Red-Green-Refactor** for ALL development:

```
1. RED    → Write failing test
2. GREEN  → Write minimal code to pass
3. REFACTOR → Improve code quality
4. REPEAT
```

### Code Formatting (MANDATORY)

**BEFORE EVERY COMMIT**:

```bash
# Format backend code
mvn spotless:apply

# Format frontend code (if applicable)
cd frontend && npm run format && cd ..
```

---

## Milestone Implementation

### Milestone M1: Database & DAO Layer

**Branch**: `008-patient-merge-backend-m1-database-dao`

**Duration**: ~1 week **Tasks**: 18-22 tasks

#### Step 1: Create Liquibase Migrations

**Location**: `src/main/resources/liquibase/3.3.x.x/`

1. **Create `patient-merge-audit.xml`**:

```bash
touch src/main/resources/liquibase/3.3.x.x/patient-merge-audit.xml
```

Reference:
[data-model.md Section 1.1](./data-model.md#11-new-table-patient_merge_audit)

2. **Create `patient-merge-tracking.xml`**:

```bash
touch src/main/resources/liquibase/3.3.x.x/patient-merge-tracking.xml
```

Reference: [data-model.md Section 1.2](./data-model.md#12-updated-table-patient)

3. **Update master changelog**:

```bash
# Edit src/main/resources/liquibase/3.3.x.x/base-changelog.xml
# Add includes for new changesets
```

#### Step 2: Create Valueholder (JPA Entity)

**Location**: `src/main/java/org/openelisglobal/patient/merge/valueholder/`

1. **Create `PatientMergeAudit.java`**:

```bash
mkdir -p src/main/java/org/openelisglobal/patient/merge/valueholder
touch src/main/java/org/openelisglobal/patient/merge/valueholder/PatientMergeAudit.java
```

Reference:
[data-model.md Section 2.1](./data-model.md#21-new-entity-patientmergeaudit)

**TDD**: Write ORM validation test FIRST:

```bash
touch src/test/java/org/openelisglobal/patient/merge/valueholder/PatientMergeAuditORMTest.java
```

2. **Update `Patient.java`**:

Reference:
[data-model.md Section 2.2](./data-model.md#22-updated-entity-patient)

**TDD**: Write ORM validation test for new fields

#### Step 3: Create DAO Layer

**Location**: `src/main/java/org/openelisglobal/patient/merge/dao/`

1. **Create `PatientMergeAuditDAO.java`** (interface):

```bash
mkdir -p src/main/java/org/openelisglobal/patient/merge/dao
touch src/main/java/org/openelisglobal/patient/merge/dao/PatientMergeAuditDAO.java
```

2. **Create `PatientMergeAuditDAOImpl.java`**:

```bash
touch src/main/java/org/openelisglobal/patient/merge/dao/PatientMergeAuditDAOImpl.java
```

**TDD**: Write DAO test FIRST:

```bash
mkdir -p src/test/java/org/openelisglobal/patient/merge/dao
touch src/test/java/org/openelisglobal/patient/merge/dao/PatientMergeAuditDAOTest.java
```

Use `@DataJpaTest` for DAO testing

#### Step 4: Verify Migrations

```bash
# Run migrations
mvn liquibase:update

# Verify tables exist
psql -U clinlims -d clinlims -c "\dt patient_merge_audit"
psql -U clinlims -d clinlims -c "\d patient"

# Test rollback
mvn liquibase:rollback -Dliquibase.rollbackCount=2
mvn liquibase:update
```

#### Milestone M1 Checkpoint

**Verify**:

- [ ] All Liquibase migrations execute without errors
- [ ] Rollback scripts work correctly
- [ ] ORM validation tests pass
- [ ] DAO tests pass (>80% coverage)
- [ ] Code formatted with `mvn spotless:apply`

**Commit**:

```bash
mvn spotless:apply
git add .
git commit -m "$(cat <<'EOF'
feat: Add database schema and DAO layer for patient merge

- Create patient_merge_audit table with JSONB data_summary
- Add merge tracking fields to patient table (merged_into_patient_id, is_merged, merge_date)
- Implement PatientMergeAudit valueholder with JPA annotations
- Update Patient entity with merge tracking fields
- Create PatientMergeAuditDAO with CRUD operations
- Add ORM validation and DAO integration tests

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

---

### Milestone M2: Service Logic

**Branch**: `008-patient-merge-backend-m2-service-logic`

**Duration**: ~1.5 weeks **Tasks**: 22-28 tasks

**BLOCKER**: See [BLOCKERS.md](./BLOCKERS.md) - Awaiting PM decision on
duplicate identifier handling

#### Step 1: Create DTOs

**Location**: `src/main/java/org/openelisglobal/patient/merge/dto/`

Create all DTOs per
[data-model.md Section 5](./data-model.md#5-data-transfer-objects-dtos):

- `PatientMergeRequestDTO.java`
- `PatientMergeValidationResultDTO.java`
- `PatientMergeDataSummaryDTO.java`
- `PatientMergeDetailsDTO.java`
- `PatientMergeExecutionResultDTO.java`

**Note**: Use Lombok `@Data` annotation

#### Step 2: Create Service Interface

**Location**: `src/main/java/org/openelisglobal/patient/merge/service/`

1. **Create `PatientMergeService.java`**:

```java
public interface PatientMergeService {
    PatientMergeValidationResultDTO validateMerge(String patient1Id, String patient2Id);
    PatientMergeDetailsDTO getMergeDetails(String patientId);
    PatientMergeAudit executeMerge(PatientMergeRequestDTO request);
}
```

#### Step 3: Create Service Implementation

**TDD Approach**:

1. Write unit test FIRST (mock all dependencies)
2. Implement method to pass test
3. Refactor

**Example TDD Cycle for `validateMerge()`**:

```bash
# 1. RED: Write failing test
touch src/test/java/org/openelisglobal/patient/merge/service/PatientMergeServiceImplTest.java
```

```java
@Test
public void validateMerge_whenSamePatientId_shouldReturnInvalid() {
    // Arrange
    String patientId = "123";

    // Act
    PatientMergeValidationResultDTO result = service.validateMerge(patientId, patientId);

    // Assert
    assertFalse(result.isValid());
    assertTrue(result.getErrors().contains("Cannot merge patient with itself"));
}
```

```bash
# 2. GREEN: Implement minimal code
# Edit PatientMergeServiceImpl.java to make test pass

# 3. REFACTOR: Improve code quality

# 4. REPEAT for next test
```

**Key Methods**:

- `validateMerge()` - Validation logic (see
  [research.md Section 9](./research.md#9-error-handling-and-validation))
- `executeMerge()` - Main merge logic (see
  [data-model.md Section 6.1](./data-model.md#61-merge-execution-data-flow))
- `consolidateIdentifiers()` - **BLOCKED** until PM decision

#### Step 4: Implement Bulk UPDATE DAO Methods

Reference:
[research.md Section 4](./research.md#4-batch-update-performance-optimization)

**Example**:

```java
// In SampleHumanDAO.java
@Modifying
@Query("UPDATE SampleHuman s SET s.patientId = :primaryPatientId WHERE s.patientId = :mergedPatientId")
int updateSampleHumanPatientId(@Param("primaryPatientId") String primaryPatientId,
                                @Param("mergedPatientId") String mergedPatientId);
```

**TDD**: Write integration test FIRST using `@DataJpaTest`

#### Step 5: Implement FHIR Integration

Reference: [data-model.md Section 4](./data-model.md#4-fhir-resource-mapping)

1. **Create `FhirPatientLinkService.java`**:

```bash
mkdir -p src/main/java/org/openelisglobal/patient/merge/service
touch src/main/java/org/openelisglobal/patient/merge/service/FhirPatientLinkService.java
```

2. **Implement bidirectional links** (replaces/replaced-by)

**TDD**: Write FHIR transformation test FIRST

#### Milestone M2 Checkpoint

**Verify**:

- [ ] All unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] Transaction rollback tests pass
- [ ] FHIR link tests pass
- [ ] Permission enforcement tests pass
- [ ] Code formatted with `mvn spotless:apply`

**Run Tests**:

```bash
# Unit tests only
mvn test

# Integration tests
mvn verify

# Coverage report
mvn jacoco:report
# View: target/site/jacoco/index.html
```

**Commit**:

```bash
mvn spotless:apply
git add .
git commit -m "$(cat <<'EOF'
feat: Add service layer for patient merge

- Implement PatientMergeService with validation and execution logic
- Add HQL bulk UPDATE methods for efficient data consolidation
- Implement FHIR Patient link relationships (replaces/replaced-by)
- Add transaction management with automatic rollback
- Implement permission enforcement (Global Administrator required)
- Create comprehensive audit trail with JSONB data summary
- Add unit tests (>80% coverage) and integration tests

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

---

### Milestone M3: REST Controller

**Branch**: `008-patient-merge-backend-m3-rest-controller`

**Duration**: ~1 week **Tasks**: 16-20 tasks

#### Step 1: Create REST Controller

**Location**: `src/main/java/org/openelisglobal/patient/merge/controller/rest/`

1. **Create `PatientMergeRestController.java`**:

```bash
mkdir -p src/main/java/org/openelisglobal/patient/merge/controller/rest
touch src/main/java/org/openelisglobal/patient/merge/controller/rest/PatientMergeRestController.java
```

Reference:
[contracts/patient-merge-api.yaml](./contracts/patient-merge-api.yaml)

**Example Structure**:

```java
@RestController
@RequestMapping("/api/patient/merge")
@PreAuthorize("hasRole('ROLE_GLOBAL_ADMIN')")
public class PatientMergeRestController {

    @Autowired
    private PatientMergeService patientMergeService;

    @GetMapping("/merge-details/{patientId}")
    public ResponseEntity<PatientMergeDetailsDTO> getMergeDetails(
            @PathVariable String patientId) {
        // Implementation
    }

    @PostMapping("/validate")
    public ResponseEntity<PatientMergeValidationResultDTO> validateMerge(
            @Valid @RequestBody Map<String, String> request) {
        // Implementation
    }

    @PostMapping("/execute")
    public ResponseEntity<PatientMergeExecutionResultDTO> executeMerge(
            @Valid @RequestBody PatientMergeRequestDTO request) {
        // Implementation
    }
}
```

#### Step 2: Controller Testing

**TDD**: Write controller test FIRST using `@WebMvcTest`

```bash
mkdir -p src/test/java/org/openelisglobal/patient/merge/controller/rest
touch src/test/java/org/openelisglobal/patient/merge/controller/rest/PatientMergeRestControllerTest.java
```

**Example Test**:

```java
@WebMvcTest(PatientMergeRestController.class)
public class PatientMergeRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientMergeService patientMergeService;

    @Test
    @WithMockUser(roles = "GLOBAL_ADMIN")
    public void getMergeDetails_whenValidId_shouldReturn200() throws Exception {
        // Arrange
        PatientMergeDetailsDTO dto = new PatientMergeDetailsDTO();
        dto.setPatientId("123");
        when(patientMergeService.getMergeDetails("123")).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(get("/api/patient/merge/merge-details/123"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.patientId").value("123"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getMergeDetails_whenNotGlobalAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/patient/merge/merge-details/123"))
               .andExpect(status().isForbidden());
    }
}
```

#### Step 3: API Integration Tests

Use RestAssured or Spring Boot Test for end-to-end API testing:

```bash
touch src/test/java/org/openelisglobal/patient/merge/integration/PatientMergeApiIntegrationTest.java
```

**Example**:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public class PatientMergeApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    public void fullMergeWorkflow_shouldSucceed() {
        // 1. Get merge details for patient 1
        // 2. Get merge details for patient 2
        // 3. Validate merge
        // 4. Execute merge
        // 5. Verify merged patient is inactive
        // 6. Verify primary patient has consolidated data
    }
}
```

#### Milestone M3 Checkpoint

**Verify**:

- [ ] All controller tests pass (@WebMvcTest)
- [ ] API integration tests pass
- [ ] Security tests pass (403 for non-admin)
- [ ] Error response tests pass (400, 404, 500)
- [ ] API contract matches OpenAPI spec
- [ ] Code formatted with `mvn spotless:apply`

**Test API Manually** (optional):

```bash
# Using curl or Postman
curl -X GET http://localhost:8080/api/patient/merge/merge-details/123 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Commit**:

```bash
mvn spotless:apply
git add .
git commit -m "$(cat <<'EOF'
feat: Add REST API endpoints for patient merge

- Implement GET /api/patient/merge-details/{patientId}
- Implement POST /api/patient/merge/validate
- Implement POST /api/patient/merge/execute
- Add Global Administrator permission enforcement (@PreAuthorize)
- Implement error handling with proper HTTP status codes
- Add controller tests (@WebMvcTest) and API integration tests
- Update OpenAPI documentation

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

---

## Testing Strategy

### Unit Tests (75% of tests)

**Location**: `src/test/java/.../` **Framework**: JUnit 4 + Mockito **Coverage
Target**: >80%

**Run**:

```bash
mvn test
```

**Focus Areas**:

- Service validation logic
- Permission checks
- Business rule enforcement
- Edge cases (same patient, already merged, circular references)

### Integration Tests (15% of tests)

**Framework**: Spring Boot Test + @DataJpaTest **Database**: H2 in-memory or
Testcontainers PostgreSQL

**Run**:

```bash
mvn verify
```

**Focus Areas**:

- Full merge workflow
- Transaction rollback scenarios
- FHIR synchronization
- Database constraints

### DAO Tests (5% of tests)

**Framework**: @DataJpaTest

**Focus Areas**:

- HQL bulk UPDATE queries
- Foreign key updates
- Data integrity

### ORM Validation Tests (5% of tests)

**Framework**: Hibernate Validator Test

**Focus Areas**:

- Entity mappings
- Constraints (NOT NULL, UNIQUE, FK)
- JSONB serialization

---

## Common Issues & Troubleshooting

### Issue 1: Liquibase Migration Fails

**Symptom**: `liquibase.exception.LiquibaseException`

**Solution**:

```bash
# Check current version
mvn liquibase:status

# Rollback and retry
mvn liquibase:rollback -Dliquibase.rollbackCount=1
mvn liquibase:update

# Clear checksums if needed
mvn liquibase:clearCheckSums
```

### Issue 2: LazyInitializationException

**Symptom**:
`org.hibernate.LazyInitializationException: could not initialize proxy`

**Solution**: Ensure service compiles all data within `@Transactional` scope

```java
@Transactional
public PatientMergeDetailsDTO getMergeDetails(String patientId) {
    Patient patient = patientDAO.findById(patientId);

    // MUST fetch lazy collections within transaction
    patient.getIdentities().size(); // Force initialization
    patient.getContacts().size();

    return buildDTO(patient);
}
```

### Issue 3: Tests Fail with "Permission Denied"

**Symptom**: `@PreAuthorize` fails in tests

**Solution**: Use `@WithMockUser(roles = "GLOBAL_ADMIN")` in tests

```java
@Test
@WithMockUser(roles = "GLOBAL_ADMIN")
public void testMerge() {
    // Test code
}
```

### Issue 4: Spotless Formatting Fails

**Symptom**: `mvn spotless:check` fails

**Solution**:

```bash
# Auto-fix formatting
mvn spotless:apply

# Then commit
git add .
git commit
```

### Issue 5: FHIR Sync Fails

**Symptom**: `FhirPersistanceException`

**Solution**: Check FHIR server is running and accessible

```bash
# Verify FHIR server
curl http://localhost:8081/fhir/Patient/123
```

---

## Code Quality Checklist

Before committing:

- [ ] All tests pass (`mvn verify`)
- [ ] Code coverage >80% (`mvn jacoco:report`)
- [ ] Code formatted (`mvn spotless:apply`)
- [ ] No TODOs or FIXMEs in committed code
- [ ] Javadoc added for public methods
- [ ] Constitution compliance verified
- [ ] BLOCKERS.md checked for open issues

---

## Resources

### Documentation

- [OpenAPI Specification](./contracts/patient-merge-api.yaml)
- [Data Model](./data-model.md)
- [Technical Research](./research.md)
- [Implementation Plan](./plan.md)

### External References

- [FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html)
- [Spring @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Liquibase Documentation](https://docs.liquibase.com/)
- [JUnit 4 Guide](https://junit.org/junit4/)

### OpenELIS Patterns

- [Sample Storage Feature](../001-sample-storage/) - Reference implementation
- [Constitution](../../.specify/memory/constitution.md) - Architecture patterns

---

## Getting Help

### Blockers

- Check [BLOCKERS.md](./BLOCKERS.md) for known issues
- Raise questions with PM if blocked

### Code Review

- Create draft PR after each milestone
- Request review from team lead
- Address feedback before merging

### Community

- OpenELIS Slack: #development channel
- GitHub Discussions: https://github.com/I-TECH-UW/OpenELIS-Global-2/discussions

---

**Last Updated**: 2025-12-08 **Next Review**: After PM resolves identifier
duplication blocker
