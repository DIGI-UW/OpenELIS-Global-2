# Analyzer XML-to-Annotation Migration Guide

**Status**: Deferred (post-M18) | **Effort**: 52-68 hours (7-9 developer days)
**Date**: 2026-01-28

---

## Problem

OpenELIS has **129 legacy XML-mapped** entities coexisting with **~75
annotation-based** entities. The Analyzer domain has **5 XML-mapped** and **7
annotation-based** entities. Bidirectional JPA relationships between the two
mapping systems **break at runtime** with `MappingException`.

**Key rule**: Annotation → XML (unidirectional) works. XML ↔ Annotation
(bidirectional) **BREAKS**. Per Constitution Principle IV, new entities MUST use
annotations; legacy XML may be extended but must document why.

---

## Integration Patterns

### Pattern 1: Unidirectional JPA (Annotation → XML) — SAFE

Used by AnalyzerConfiguration, AnalyzerError.

```java
@Entity
public class AnalyzerConfiguration extends BaseObject<String> {
    @OneToOne
    @JoinColumn(name = "analyzer_id", nullable = false, unique = true)
    private Analyzer analyzer;  // Unidirectional: annotation → XML
}
```

### Pattern 2: Manual FK (NO JPA object) — RECOMMENDED for M2-M18

Used by SerialPortConfiguration, FileImportConfiguration.

```java
@Entity
public class SerialPortConfiguration extends BaseObject<String> {
    @Column(name = "analyzer_id", nullable = false, unique = true)
    private Integer analyzerId;  // FK as primitive, NO @ManyToOne
}
```

Service layer manually hydrates the Analyzer when needed:

```java
@Transactional(readOnly = true)
public SerialPortConfigurationDTO getByAnalyzerId(Integer analyzerId) {
    var config = configDAO.findByAnalyzerId(analyzerId).orElse(null);
    var analyzer = analyzerDAO.get(String.valueOf(analyzerId)).orElseThrow();
    return new SerialPortConfigurationDTO(config, analyzer);  // Compile in txn
}
```

### Pattern 5: Bidirectional (XML ↔ Annotation) — NEVER USE

Adding `@OneToMany` to Analyzer.java causes Hibernate `MappingException` because
both XML and annotation metadata exist for the same entity.

---

## 5 Entities to Migrate

| Entity                   | Complexity | Effort | Dependencies     | Key Challenge                            |
| ------------------------ | ---------- | ------ | ---------------- | ---------------------------------------- |
| **Analyzer**             | LOW        | 6-8h   | None (root)      | `LIMSStringNumberUserType` ID            |
| **AnalyzerField**        | MEDIUM     | 8-10h  | Analyzer         | 2 `@ManyToOne` relationships             |
| **AnalyzerFieldMapping** | HIGH       | 12-16h | Analyzer + Field | Custom `@Version`, 3 manual FKs, 2 enums |
| **AnalyzerResults**      | MEDIUM     | 8-10h  | Analyzer         | 12 properties, self-reference FK         |
| **AnalyzerTestMapping**  | MED-HIGH   | 10-12h | Analyzer         | Composite `@EmbeddedId` PK               |

---

## Migration Dependency Graph

```
Phase 1 (root, no deps):     Analyzer
                                 │
Phase 2 (parallel):    AnalyzerField  AnalyzerResults  AnalyzerTestMapping
                           │
Phase 3 (last):        AnalyzerFieldMapping
```

---

## Effort Summary

| Phase   | Entity                | Effort | Cumulative |
| ------- | --------------------- | ------ | ---------- |
| 1       | Analyzer              | 6-8h   | 6-8h       |
| 2a      | AnalyzerField         | 8-10h  | 14-18h     |
| 2b      | AnalyzerResults       | 8-10h  | 22-28h     |
| 2c      | AnalyzerTestMapping   | 10-12h | 32-40h     |
| 3       | AnalyzerFieldMapping  | 12-16h | 44-56h     |
| Testing | Integration + plugins | 8-12h  | **52-68h** |

**Staffing**: 2 developers parallel = 1-2 weeks. 1 developer = 2-3 weeks.

---

## Pre-Migration Checklist

- [ ] M18 completed and merged to develop
- [ ] All analyzer E2E tests passing
- [ ] 11 analyzer plugins verified working
- [ ] Dedicated 2-week sprint scheduled
- [ ] Database backup created
- [ ] Rollback plan documented

---

## Phase 1 Example: Analyzer Entity

```java
@Entity
@Table(name = "analyzer")
public class Analyzer extends BaseObject<String> {
    @Id
    @Column(name = "ID", precision = 10)
    @GenericGenerator(name = "analyzer_seq_gen",
        strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator",
        parameters = @Parameter(name = "sequence_name", value = "analyzer_seq"))
    @GeneratedValue(generator = "analyzer_seq_gen")
    private String id;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "analyzer_type", length = 30)
    private String type;

    @Column(name = "is_active")
    private boolean active;
    // ... rest of fields
}
```

After migration: update `hibernate.cfg.xml` (remove XML mapping), add to
`persistence.xml`, update `HibernateMappingValidationTest`.

---

## Key Files

- **Constitution**: `.specify/memory/constitution.md` (Principle IV, V.4)
- **Pattern documentation**: `AnalyzerFieldMapping.hbm.xml` lines 30-33
- **Existing patterns**: `AnalyzerConfiguration.java` (Pattern 1),
  `FileImportConfiguration.java` (Pattern 2)
- **ORM test**: `HibernateMappingValidationTest.java`

---

## Codebase-Wide Context

129 total XML entities. Full codebase migration estimated at 1,700-2,100 hours
(2-3 years incremental). Analyzer module (5 entities) is the natural first
target. See `demo/madagascar` branch for full analysis documents.
