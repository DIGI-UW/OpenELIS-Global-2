# ID Type Analysis: Legacy vs Modern Entity Patterns

**Date**: 2025-11-18  
**Feature**: 004-astm-analyzer-mapping  
**Status**: Analysis Complete

## Executive Summary

OpenELIS Global uses a **hybrid ID system** due to legacy architecture:

- **Legacy entities** (e.g., `Analyzer`): Database stores `INTEGER`, Java uses
  `String` (via custom Hibernate UserType)
- **Modern entities** (e.g., `AnalyzerError`, `AnalyzerField`): Database stores
  `VARCHAR(36)` (UUID), Java uses `String`

This creates a **type mismatch** when modern entities reference legacy entities
via foreign keys. This document analyzes the pattern and provides implementation
guidelines.

---

## 1. Legacy Entity Pattern: Analyzer

### Database Schema

```sql
CREATE TABLE analyzer (
    id NUMERIC(10,0) PRIMARY KEY,  -- INTEGER in database
    name VARCHAR(20),
    ...
);
```

### Java Entity

```java
public class Analyzer extends BaseObject<String> {
    private String id;  // String in Java, but INTEGER in database
    ...
}
```

### Hibernate Mapping (XML)

```xml
<hibernate-mapping>
    <class name="org.openelisglobal.analyzer.valueholder.Analyzer" table="analyzer">
        <id name="id" type="org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType">
            <column name="ID" precision="10" scale="0" />  <!-- NUMERIC in DB -->
            <generator class="org.openelisglobal.hibernate.resources.StringSequenceGenerator">
                <param name="sequence_name">analyzer_seq</param>
            </generator>
        </id>
        ...
    </class>
</hibernate-mapping>
```

### Key Components

#### `LIMSStringNumberUserType`

- **Purpose**: Converts between Java `String` and database `NUMERIC(10,0)`
  (INTEGER)
- **Java → DB**: `Integer.parseInt((String) value)` → `st.setInt(index, ...)`
- **DB → Java**: `rs.getInt(...)` → `String.valueOf(value)`
- **Why**: Legacy compatibility - Oracle allowed String→NUMBER, PostgreSQL does
  not

#### `StringSequenceGenerator`

- **Purpose**: Generates sequence values as `Long`, converts to `String`
- **Process**: `Long id = super.generate(...)` → `String.format("%d", id)`
- **Result**: Java sees `"1"`, `"2"`, `"3"`, but database stores `1`, `2`, `3`

### ID Format

- **Database**: `1`, `2`, `3` (INTEGER)
- **Java**: `"1"`, `"2"`, `"3"` (String representation of integers)
- **Test Data**: Uses string IDs like `"1"`, `"2"`, `"3"` in XML fixtures

---

## 2. Modern Entity Pattern: AnalyzerError, AnalyzerField, etc.

### Database Schema

```sql
CREATE TABLE analyzer_error (
    id VARCHAR(36) PRIMARY KEY,  -- UUID string
    analyzer_id NUMERIC(10,0) NOT NULL,  -- References legacy analyzer.id
    ...
    FOREIGN KEY (analyzer_id) REFERENCES analyzer(id)
);
```

### Java Entity

```java
@Entity
@Table(name = "analyzer_error")
public class AnalyzerError extends BaseObject<String> {
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;  // UUID format: "550e8400-e29b-41d4-a716-446655440000"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false, referencedColumnName = "id")
    private Analyzer analyzer;  // References legacy Analyzer
    ...

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
```

### ID Format

- **Database**: `"550e8400-e29b-41d4-a716-446655440000"` (VARCHAR UUID)
- **Java**: `"550e8400-e29b-41d4-a716-446655440000"` (String UUID)
- **Generation**: `UUID.randomUUID().toString()` in `@PrePersist`

---

## 3. The Type Mismatch Problem

### Problem Statement

When a **modern entity** (JPA-annotated) references a **legacy entity**
(XML-mapped) via `@ManyToOne`:

1. **Foreign Key Column**: `analyzer_id NUMERIC(10,0)` (INTEGER in database)
2. **Java Relationship**: `@ManyToOne Analyzer` where `Analyzer.id` is `String`
   in Java
3. **HQL Query**: `WHERE analyzer.id = :analyzerId` expects `String` parameter
4. **Database Comparison**: Must compare `NUMERIC(10,0)` column with `String`
   parameter

### Example: AnalyzerErrorDAO.findByAnalyzerId()

**Current Implementation (INCORRECT)**:

```java
public List<AnalyzerError> findByAnalyzerId(String analyzerId) {
    String hql = "FROM AnalyzerError WHERE analyzer.id = :analyzerId";
    Query<AnalyzerError> query = ...;
    query.setParameter("analyzerId", analyzerId);  // String "1"
    return query.list();
}
```

**Problem**: Hibernate tries to compare:

- Database: `analyzer_id = 1` (INTEGER)
- Parameter: `"1"` (String)

Hibernate's type conversion may fail or produce incorrect results.

---

## 4. Existing Solutions in Codebase

### Pattern 1: Convert String to Integer in DAO (RECOMMENDED)

**Used in**: `AnalyzerFieldDAOImpl.findByAnalyzerId()`,
`AnalyzerConfigurationDAOImpl.findByAnalyzerId()`

```java
public List<AnalyzerField> findByAnalyzerId(String analyzerId) {
    try {
        Integer analyzerIdInt;
        try {
            analyzerIdInt = Integer.parseInt(analyzerId);
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
        }

        String hql = "SELECT af FROM AnalyzerField af JOIN af.analyzer a WHERE a.id = :analyzerId";
        Query<AnalyzerField> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerField.class);
        query.setParameter("analyzerId", analyzerIdInt);  // Pass Integer, not String
        return query.list();
    } catch (Exception e) {
        throw new LIMSRuntimeException("Error finding AnalyzerField by analyzer ID", e);
    }
}
```

**Why This Works**:

- Hibernate's `LIMSStringNumberUserType` handles conversion when loading
  `Analyzer`
- But for HQL parameter binding, we must pass the **database type** (Integer)
- Hibernate compares `NUMERIC(10,0) = INTEGER` correctly

### Pattern 2: Use Native SQL (NOT RECOMMENDED)

```java
// ❌ ANTI-PATTERN: Avoid native SQL
String sql = "SELECT * FROM analyzer_error WHERE analyzer_id = ?";
// Requires manual type conversion, breaks HQL abstraction
```

---

## 5. Implementation Guidelines

### Rule 1: DAO Methods Accepting Analyzer ID

**For DAOs that query by `analyzer_id` foreign key**:

1. **Method signature**: Accept `String analyzerId` (matches Java type)
2. **Parameter conversion**: Convert `String` → `Integer` before HQL query
3. **HQL parameter**: Pass `Integer` to `setParameter()`
4. **Error handling**: Catch `NumberFormatException` and throw
   `LIMSRuntimeException`

**Template**:

```java
@Override
@Transactional(readOnly = true)
public List<YourEntity> findByAnalyzerId(String analyzerId) {
    try {
        Integer analyzerIdInt;
        try {
            analyzerIdInt = Integer.parseInt(analyzerId);
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
        }

        String hql = "FROM YourEntity WHERE analyzer.id = :analyzerId";
        Query<YourEntity> query = entityManager.unwrap(Session.class).createQuery(hql, YourEntity.class);
        query.setParameter("analyzerId", analyzerIdInt);  // Integer, not String
        return query.list();
    } catch (Exception e) {
        throw new LIMSRuntimeException("Error finding YourEntity by analyzer ID", e);
    }
}
```

### Rule 2: Service Layer Methods

**Service methods should accept `String analyzerId`** (matches Java entity
type):

```java
public interface YourService {
    List<YourEntity> findByAnalyzerId(String analyzerId);  // String, not Integer
}
```

**Service implementation delegates to DAO** (which handles conversion
internally):

```java
@Override
public List<YourEntity> findByAnalyzerId(String analyzerId) {
    return yourDAO.findByAnalyzerId(analyzerId);  // Pass String, DAO converts
}
```

### Rule 3: Controller/REST Layer

**Controllers accept `String analyzerId`** (matches REST API convention):

```java
@GetMapping("/entities")
public ResponseEntity<?> getEntities(@RequestParam String analyzerId) {
    List<YourEntity> entities = yourService.findByAnalyzerId(analyzerId);
    return ResponseEntity.ok(entities);
}
```

### Rule 4: Test Data

**Use string IDs in test data** (matches Java entity type):

```java
Analyzer analyzer = new Analyzer();
analyzer.setId("1");  // String, not Integer
analyzer.setName("Test Analyzer");
```

**In test XML fixtures**:

```xml
<analyzer id="1" name="Test Analyzer" ... />  <!-- String "1" in Java -->
```

---

## 6. Entities Requiring Fix

### ✅ Already Correct

- `AnalyzerFieldDAOImpl.findByAnalyzerId()` - Converts String → Integer
- `AnalyzerConfigurationDAOImpl.findByAnalyzerId()` - Converts String → Integer
- `AnalyzerFieldMappingDAOImpl.findByAnalyzerIdWithFields()` - Converts String →
  Integer

### ❌ Needs Fix

- `AnalyzerErrorDAOImpl.findByAnalyzerId()` - **Currently passes String
  directly**

---

## 7. Fix Implementation

### Fix: AnalyzerErrorDAOImpl.findByAnalyzerId()

**Before**:

```java
@Override
@Transactional(readOnly = true)
public List<AnalyzerError> findByAnalyzerId(String analyzerId) {
    try {
        String hql = "FROM AnalyzerError WHERE analyzer.id = :analyzerId ORDER BY lastupdated DESC";
        Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
        query.setParameter("analyzerId", analyzerId);  // ❌ WRONG: String passed directly
        return query.list();
    } catch (Exception e) {
        throw new LIMSRuntimeException("Error finding AnalyzerError by analyzer ID", e);
    }
}
```

**After**:

```java
@Override
@Transactional(readOnly = true)
public List<AnalyzerError> findByAnalyzerId(String analyzerId) {
    try {
        Integer analyzerIdInt;
        try {
            analyzerIdInt = Integer.parseInt(analyzerId);
        } catch (NumberFormatException e) {
            throw new LIMSRuntimeException("Invalid analyzer ID format: " + analyzerId, e);
        }

        String hql = "FROM AnalyzerError WHERE analyzer.id = :analyzerId ORDER BY lastupdated DESC";
        Query<AnalyzerError> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerError.class);
        query.setParameter("analyzerId", analyzerIdInt);  // ✅ CORRECT: Integer passed
        return query.list();
    } catch (Exception e) {
        throw new LIMSRuntimeException("Error finding AnalyzerError by analyzer ID", e);
    }
}
```

---

## 8. Testing Considerations

### Unit Tests (Mockito)

**Mock the conversion**:

```java
@Test
public void testFindByAnalyzerId_WithValidId_ReturnsErrors() {
    // Arrange: Mock HQL query
    when(entityManager.unwrap(Session.class)).thenReturn(session);
    when(session.createQuery(anyString(), eq(AnalyzerError.class))).thenReturn(query);
    when(query.setParameter(eq("analyzerId"), eq(1))).thenReturn(query);  // Integer, not String

    List<AnalyzerError> expectedResults = new ArrayList<>();
    expectedResults.add(testError);
    when(query.list()).thenReturn(expectedResults);

    // Act
    List<AnalyzerError> results = analyzerErrorDAO.findByAnalyzerId("1");  // String input

    // Assert
    assertEquals(1, results.size());
}
```

### Integration Tests (BaseWebContextSensitiveTest)

**Use existing analyzers from test data**:

```java
@Before
public void setUp() throws Exception {
    super.setUp();

    // Get existing analyzer from test data (IDs: "1", "2", "3")
    List<Analyzer> analyzers = analyzerService.getAll();
    if (!analyzers.isEmpty()) {
        testAnalyzer = analyzers.get(0);  // Uses String ID internally
    }
}
```

---

## 9. Summary

### Key Takeaways

1. **Legacy entities** (`Analyzer`): Database `INTEGER` → Java `String` (via
   `LIMSStringNumberUserType`)
2. **Modern entities**: Database `VARCHAR(36)` → Java `String` (UUID)
3. **Foreign keys to legacy entities**: Database `NUMERIC(10,0)`, but Java
   relationship uses `String`
4. **Solution**: Convert `String` → `Integer` in DAO before HQL parameter
   binding
5. **Pattern**: All DAOs querying by `analyzer_id` should follow
   `AnalyzerFieldDAOImpl` pattern

### Implementation Checklist

- [x] Analyze ID type patterns
- [ ] Fix `AnalyzerErrorDAOImpl.findByAnalyzerId()` to convert String → Integer
- [ ] Update `AnalyzerErrorService` to handle String IDs correctly
- [ ] Update `AnalyzerErrorRestController` to accept String IDs
- [ ] Update tests to use String IDs consistently
- [ ] Verify integration tests pass with real database

---

## 10. References

- `src/main/resources/hibernate/hbm/Analyzer.hbm.xml` - Legacy XML mapping
- `src/main/java/org/openelisglobal/hibernate/resources/usertype/LIMSStringNumberUserType.java` -
  Type converter
- `src/main/java/org/openelisglobal/hibernate/resources/StringSequenceGenerator.java` -
  ID generator
- `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOImpl.java` -
  Correct pattern
- `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAOImpl.java` -
  Needs fix
- `src/main/resources/liquibase/analyzer/004-006-create-analyzer-error-table.xml` -
  Schema definition
