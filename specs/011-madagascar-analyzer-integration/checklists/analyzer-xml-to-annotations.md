# Analyzer XML-to-Annotation Migration Checklist

**Feature**: 011-madagascar-analyzer-integration **Chore Branch**:
`chore/011-analyzer-xml-to-annotations` **Base Branch**:
`origin/demo/madagascar` **Plan**: fizzy-knitting-duckling.md **Effort**: 52-68
hours (7-9 developer days) **Constitution**: Principle IV (JPA annotations),
Principle V.4 (ORM validation)

---

## Pre-Migration Setup

- [ ] Feature 011 M18 completed and merged
- [ ] All analyzer E2E tests passing (`npm run cy:analyzer`)
- [ ] Database backup created
- [ ] 2-week sprint scheduled
- [ ] Branch created: `chore/011-analyzer-xml-to-annotations` from
      `origin/demo/madagascar`
- [ ] Rollback plan reviewed with team
- [ ] Constitution compliance checklist reviewed

---

## Phase 1: Analyzer Entity (6-8 hours)

**Risk**: LOW | **Dependencies**: None

### 1.1 Add JPA Annotations

- [ ] Open `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`
- [ ] Add `@Entity` annotation to class
- [ ] Add `@Table(name = "analyzer")` annotation
- [ ] Add `@Id` annotation to `id` field
- [ ] Add `@Column(name = "ID", precision = 10, scale = 0)` to `id`
- [ ] Add `@GeneratedValue` with `StringSequenceGenerator`:
  ```java
  @GeneratedValue(generator = "analyzer_seq_gen")
  @GenericGenerator(
      name = "analyzer_seq_gen",
      strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator",
      parameters = @Parameter(name = "sequence_name", value = "analyzer_seq")
  )
  ```
- [ ] Add `@Column(name = "script_id", length = 255)` to `script_id`
- [ ] Add `@Column(name = "name", length = 255)` to `name`
- [ ] Add `@Column(name = "machineId", length = 255)` to `machineId`
- [ ] Add `@Column(name = "analyzer_type", length = 30)` to `type`
- [ ] Add `@Column(name = "description", length = 512)` to `description`
- [ ] Add `@Column(name = "location", length = 255)` to `location`
- [ ] Add `@Column(name = "is_active")` to `active`
- [ ] Add `@Column(name = "has_setup_page")` to `hasSetupPage`
- [ ] Add required imports (javax.persistence._, org.hibernate.annotations._)

### 1.2 Update Configuration

- [ ] Open `src/main/resources/persistence/persistence.xml`
- [ ] Add `<class>org.openelisglobal.analyzer.valueholder.Analyzer</class>`
- [ ] Open `src/main/resources/hibernate/hibernate.cfg.xml`
- [ ] Comment out:
      `<!-- <mapping resource="hibernate/hbm/Analyzer.hbm.xml" /> -->`

### 1.3 Update Tests

- [ ] Open
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] Change `addResource("hibernate/hbm/Analyzer.hbm.xml")` to
      `addAnnotatedClass(Analyzer.class)`

### 1.4 Validation

- [ ] Run ORM validation: `mvn test -Dtest=HibernateMappingValidationTest`
  - [ ] Test passes in <5 seconds
  - [ ] No Hibernate mapping errors
- [ ] Run analyzer tests:
      `mvn test -Dtest=org.openelisglobal.analyzer.service.AnalyzerServiceTest`
  - [ ] All tests pass
- [ ] Application starts successfully
- [ ] Check logs for Hibernate initialization warnings

### 1.5 Manual Smoke Test

- [ ] Start application
- [ ] Navigate to Analyzer Configuration UI
- [ ] Create new analyzer
- [ ] Edit analyzer
- [ ] Verify data persists correctly

---

## Phase 2A: AnalyzerField Entity (8-10 hours)

**Risk**: MEDIUM | **Dependencies**: Phase 1 complete

### 2A.1 Add JPA Annotations

- [ ] Open
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerField.java`
- [ ] Add `@Entity` annotation
- [ ] Add `@Table(name = "analyzer_field")` annotation
- [ ] Add `@Id` and `@Column(name = "id", length = 36, nullable = false)` to
      `id`
- [ ] Add `@PrePersist` method for UUID generation:
  ```java
  @PrePersist
  protected void onCreate() {
      if (id == null) {
          id = UUID.randomUUID().toString();
      }
  }
  ```
- [ ] Add `@ManyToOne(fetch = FetchType.LAZY)` to `analyzer` field
- [ ] Add `@JoinColumn(name = "analyzer_id", nullable = false)` to `analyzer`
- [ ] Add `@ManyToOne(fetch = FetchType.LAZY)` to `customFieldType` field
- [ ] Add `@JoinColumn(name = "custom_field_type_id")` to `customFieldType`
- [ ] Add `@Enumerated(EnumType.STRING)` to `fieldType` field
- [ ] Add `@Column(name = "field_type")` to `fieldType`
- [ ] Add `@Column` annotations to remaining fields (name, position, sortOrder,
      isActive)

### 2A.2 Update Configuration

- [ ] Add to `persistence.xml`:
      `<class>org.openelisglobal.analyzer.valueholder.AnalyzerField</class>`
- [ ] Comment out in `hibernate.cfg.xml`:
      `<!-- <mapping resource="hibernate/hbm/AnalyzerField.hbm.xml" /> -->`

### 2A.3 Update Tests

- [ ] Update `HibernateMappingValidationTest`: Change to
      `addAnnotatedClass(AnalyzerField.class)`
- [ ] Verify enum persistence test exists
- [ ] Add relationship loading test

### 2A.4 Validation

- [ ] Run ORM validation: `mvn test -Dtest=HibernateMappingValidationTest`
- [ ] Run field tests:
      `mvn test -Dtest=org.openelisglobal.analyzer.service.AnalyzerFieldServiceTest`
- [ ] Test lazy loading within transaction
- [ ] Test UUID generation on create

### 2A.5 Manual Smoke Test

- [ ] Create analyzer field via UI
- [ ] Verify relationship to analyzer loads
- [ ] Verify custom field type relationship (if applicable)
- [ ] Check enum persistence

---

## Phase 2B: AnalyzerResults Entity (8-10 hours)

**Risk**: MEDIUM | **Dependencies**: Phase 1 complete

### 2B.1 Add JPA Annotations

- [ ] Open
      `src/main/java/org/openelisglobal/analyzerresults/valueholder/AnalyzerResults.java`
- [ ] Add `@Entity` annotation
- [ ] Add `@Table(name = "ANALYZER_RESULTS")` annotation (preserve uppercase)
- [ ] Add `@Id`, `@Column(name = "ID")`, `@GeneratedValue` with
      `analyzer_results_seq_gen`
- [ ] Add `@Column(name = "ANALYZER_ID", precision = 10, scale = 0)` to
      `analyzerId`
- [ ] Add `@Type(LIMSStringNumberUserType.class)` to `analyzerId`
- [ ] Add `@Column` annotations to all 12 properties (preserve UPPERCASE names)
- [ ] Add `@Column(name = "DUPLICATE_ANALYZER_RESULT_ID")` to self-referential
      FK
- [ ] Verify all column names match XML exactly

### 2B.2 Update Configuration

- [ ] Add to `persistence.xml`:
      `<class>org.openelisglobal.analyzerresults.valueholder.AnalyzerResults</class>`
- [ ] Comment out in `hibernate.cfg.xml`:
      `<!-- <mapping resource="hibernate/hbm/AnalyzerResults.hbm.xml" /> -->`

### 2B.3 Update Tests

- [ ] Update `HibernateMappingValidationTest`: Change to
      `addAnnotatedClass(AnalyzerResults.class)`
- [ ] Test `LIMSStringNumberUserType` works with annotations
- [ ] Test sequence generation

### 2B.4 Validation

- [ ] Run ORM validation: `mvn test -Dtest=HibernateMappingValidationTest`
- [ ] Run analyzer results tests
- [ ] Verify uppercase column names preserved
- [ ] Test manual FK persistence

---

## Phase 2C: AnalyzerTestMapping Entity (10-12 hours)

**Risk**: MEDIUM-HIGH | **Dependencies**: Phase 1 complete

### 2C.1 Create Composite Primary Key Class

- [ ] Create
      `src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMappingPK.java`
- [ ] Add `@Embeddable` annotation
- [ ] Add `implements Serializable`
- [ ] Add `@Column(name = "analyzer_id")` field
- [ ] Add `@Type(LIMSStringNumberUserType.class)` to `analyzerId`
- [ ] Add `@Column(name = "analyzer_test_name")` field
- [ ] Implement `equals()` method comparing both fields
- [ ] Implement `hashCode()` method using both fields
- [ ] Add default constructor and parameterized constructor

### 2C.2 Add JPA Annotations to Entity

- [ ] Open
      `src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMapping.java`
- [ ] Add `@Entity` annotation
- [ ] Add `@Table(name = "analyzer_test_map")` annotation
- [ ] Add `@EmbeddedId` to composite key field
- [ ] Change ID type: `extends BaseObject<AnalyzerTestMappingPK>`
- [ ] Add `@Column(name = "test_id")` to `testId`
- [ ] Add `@Type(LIMSStringNumberUserType.class)` to `testId`
- [ ] Update getId()/setId() to work with composite key

### 2C.3 Update Configuration

- [ ] Add to `persistence.xml`:
      `<class>org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping</class>`
- [ ] Comment out in `hibernate.cfg.xml`:
      `<!-- <mapping resource="hibernate/hbm/AnalyzerTestMapping.hbm.xml" /> -->`

### 2C.4 Update Tests

- [ ] Update `HibernateMappingValidationTest`: Change to
      `addAnnotatedClass(AnalyzerTestMapping.class)`
- [ ] Test composite key CRUD operations
- [ ] Test equals/hashCode in collections (Set, Map)
- [ ] Test querying by composite key components

### 2C.5 Validation

- [ ] Run ORM validation: `mvn test -Dtest=HibernateMappingValidationTest`
- [ ] Run analyzer import tests:
      `mvn test -Dtest=org.openelisglobal.analyzerimport.*`
- [ ] Test composite key uniqueness constraint

---

## Phase 3: AnalyzerFieldMapping Entity (12-16 hours)

**Risk**: HIGH | **Dependencies**: Phase 1 AND Phase 2A complete

### 3.1 Add JPA Annotations

- [ ] Open
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerFieldMapping.java`
- [ ] Add `@Entity` annotation
- [ ] Add `@Table(name = "analyzer_field_mapping")` annotation
- [ ] Add `@Id` and `@Column(name = "id", length = 36, nullable = false)` to
      `id`
- [ ] Add custom `@Version` field:
  ```java
  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;
  ```
- [ ] Override `lastupdated` as regular property:
  ```java
  @Column(name = "last_updated", nullable = false)
  private Timestamp lastupdated;
  ```
- [ ] Convert `analyzerFieldId` to relationship:
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "analyzer_field_id", nullable = false)
  private AnalyzerField analyzerField;
  ```
- [ ] Convert `analyzerId` to relationship:
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "analyzer_id", nullable = false)
  private Analyzer analyzer;
  ```
- [ ] Keep `openelisFieldId` as manual FK (polymorphic reference)
- [ ] Add `@Enumerated(EnumType.STRING)` to `openelisFieldType`
- [ ] Add `@Enumerated(EnumType.STRING)` to `mappingType`
- [ ] Override `getLastupdated()` and `setLastupdated()` methods
- [ ] Add convenience getters for backward compatibility:
  ```java
  public String getAnalyzerFieldId() {
      return analyzerField != null ? analyzerField.getId() : null;
  }
  public String getAnalyzerId() {
      return analyzer != null ? analyzer.getId() : null;
  }
  ```

### 3.2 Update Service Layer

- [ ] Open
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceImpl.java`
- [ ] Remove manual hydration code for `analyzer` and `analyzerField`
- [ ] Verify all methods have `@Transactional` annotation
- [ ] Ensure data compilation happens within transaction (Constitution
      requirement)
- [ ] Test for `LazyInitializationException` in all service methods

### 3.3 Update DAO Layer

- [ ] Open
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOImpl.java`
- [ ] Update queries to use relationship navigation:
  - [ ] Change `afm.analyzerId` to `afm.analyzer.id`
  - [ ] Change `afm.analyzerFieldId` to `afm.analyzerField.id`
- [ ] Add `JOIN FETCH` for eager loading where needed:
  ```java
  "FROM AnalyzerFieldMapping afm " +
  "LEFT JOIN FETCH afm.analyzer " +
  "LEFT JOIN FETCH afm.analyzerField " +
  "WHERE afm.analyzer.id = :analyzerId"
  ```
- [ ] Test all DAO queries return correct results

### 3.4 Update Configuration

- [ ] Add to `persistence.xml`:
      `<class>org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping</class>`
- [ ] Comment out in `hibernate.cfg.xml`:
      `<!-- <mapping resource="hibernate/hbm/AnalyzerFieldMapping.hbm.xml" /> -->`

### 3.5 Update Tests

- [ ] Update `HibernateMappingValidationTest`: Change to
      `addAnnotatedClass(AnalyzerFieldMapping.class)`
- [ ] Test custom version field increments on update
- [ ] Test optimistic locking behavior
- [ ] Test relationships load within transaction
- [ ] Test convenience getters work correctly
- [ ] Test backward compatibility of API

### 3.6 Validation

- [ ] Run ORM validation: `mvn test -Dtest=HibernateMappingValidationTest`
- [ ] Run all field mapping tests:
      `mvn test -Dtest=org.openelisglobal.analyzer.service.AnalyzerFieldMappingServiceTest`
- [ ] Test no `LazyInitializationException` occurs
- [ ] Test version field works correctly
- [ ] Test all CRUD operations work

### 3.7 Manual Smoke Test

- [ ] Create analyzer field mapping
- [ ] Update analyzer field mapping (verify version increments)
- [ ] Delete analyzer field mapping
- [ ] Test concurrent updates (optimistic locking)
- [ ] Verify relationships load correctly

---

## Phase 4: Cleanup & Documentation (4-6 hours)

**Risk**: LOW | **Dependencies**: All previous phases complete, all tests
passing

### 4.1 Delete XML Mapping Files

- [ ] Verify ALL tests pass before deletion
- [ ] Delete `src/main/resources/hibernate/hbm/Analyzer.hbm.xml`
- [ ] Delete `src/main/resources/hibernate/hbm/AnalyzerField.hbm.xml`
- [ ] Delete `src/main/resources/hibernate/hbm/AnalyzerFieldMapping.hbm.xml`
- [ ] Delete `src/main/resources/hibernate/hbm/AnalyzerResults.hbm.xml`
- [ ] Delete `src/main/resources/hibernate/hbm/AnalyzerTestMapping.hbm.xml`

### 4.2 Clean Configuration

- [ ] Open `hibernate.cfg.xml`
- [ ] Remove all commented-out analyzer mapping lines
- [ ] Verify no other references to deleted XML files
- [ ] Open `persistence.xml`
- [ ] Verify all 5 annotation classes present
- [ ] Format configuration files

### 4.3 Full Test Suite

- [ ] Run full backend test suite: `mvn clean test`
  - [ ] All 2800+ tests pass
  - [ ] No failures
  - [ ] No errors
- [ ] Run analyzer E2E tests: `cd frontend && npm run cy:analyzer`
  - [ ] All analyzer workflows pass
  - [ ] Configuration UI works
  - [ ] Field mapping works
  - [ ] Import functionality works

### 4.4 Performance Validation

- [ ] Benchmark key queries (before/after comparison if possible)
- [ ] Check for N+1 query issues
- [ ] Verify application startup time (<2 minutes)
- [ ] Profile analyzer workflows for performance

### 4.5 Documentation

- [ ] Create migration summary document
- [ ] Update constitution compliance notes
- [ ] Document any breaking changes (should be none)
- [ ] Document backward compatibility measures
- [ ] Update analyzer developer documentation

### 4.6 Code Formatting

- [ ] Run Spotless: `mvn spotless:apply`
- [ ] Verify no formatting issues

---

## Commit & Pull Request

### Commit Steps

- [ ] Stage all changes: Review each file carefully
- [ ] Run final test suite
- [ ] Create commit with message:

  ```
  chore(011): Migrate analyzer entities from XML to JPA annotations

  Migrate 5 Analyzer entities to JPA annotations per Constitution Principle IV:
  - Analyzer (Phase 1)
  - AnalyzerField (Phase 2A)
  - AnalyzerResults (Phase 2B)
  - AnalyzerTestMapping (Phase 2C)
  - AnalyzerFieldMapping (Phase 3)

  Constitution Compliance:
  - Principle IV: JPA annotations mandatory
  - Principle V.4: ORM validation tests (<5s)

  Backward Compatibility: MAINTAINED
  - All public APIs unchanged
  - Column names preserved exactly
  - Convenience getters added for FK fields

  Test Coverage:
  - ORM validation: 5 entities
  - Unit tests: All passing
  - Integration tests: All passing
  - E2E tests: Analyzer workflows verified

  Breaking Changes: NONE

  Migration Plan: specs/011-madagascar-analyzer-integration/fizzy-knitting-duckling.md

  Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
  ```

### Pull Request

- [ ] Push branch to origin
- [ ] Create PR to `demo/madagascar` (or target branch)
- [ ] Use PR template from plan (fizzy-knitting-duckling.md lines 370-403)
- [ ] Link to migration plan
- [ ] Request review from architecture lead
- [ ] Address review comments
- [ ] Verify CI passes
- [ ] Merge after approval

---

## Post-Merge Validation

- [ ] Pull merged changes
- [ ] Run smoke tests on merged branch
- [ ] Verify production deployment (if applicable)
- [ ] Monitor application logs for issues
- [ ] Watch for performance regressions

---

## Constitution Compliance Verification

### ✅ Principle IV: Layered Architecture (Valueholders)

- [ ] All 5 entities use `@Entity`, `@Table`, `@Column` annotations
- [ ] All XML mapping files deleted
- [ ] All entities extend `BaseObject<T>`
- [ ] Uses `@GenericGenerator` with sequence names
- [ ] `@PrePersist` hooks where applicable

### ✅ Principle V.4: ORM Validation Tests

- [ ] `HibernateMappingValidationTest` updated for all 5 entities
- [ ] Tests build `SessionFactory` without database
- [ ] Tests execute in <5 seconds
- [ ] Tests catch mapping errors before deployment

### ✅ Principle V.5: Transactional Service Layer

- [ ] All service methods have `@Transactional`
- [ ] Services compile data within transaction
- [ ] No `LazyInitializationException` in service layer

---

## Success Criteria

- [ ] ✅ Zero test failures (2800+ tests pass)
- [ ] ✅ No Hibernate mapping exceptions in logs
- [ ] ✅ Application starts in normal time (<2 minutes)
- [ ] ✅ All analyzer workflows functional
- [ ] ✅ Performance metrics within 10% of baseline
- [ ] ✅ Constitution compliance verified
- [ ] ✅ PR merged successfully
- [ ] ✅ Post-merge smoke tests pass

---

**Last Updated**: 2026-01-29 **Plan Reference**: fizzy-knitting-duckling.md
(copied to ~/.claude/plans/) **Estimated Completion**: 7-9 developer days (52-68
hours)
