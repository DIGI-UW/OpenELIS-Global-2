# Analyzer XML Entity Migration Scope Analysis

## Feature 011: Madagascar Analyzer Integration

**Analysis Date**: 2026-01-28 **Status**: Deferred (revisit post-M18)
**Related**:
[hibernate-xml-vs-annotation-analysis.md](hibernate-xml-vs-annotation-analysis.md)

---

## Executive Summary

Migration of the 5 Analyzer XML-mapped entities to JPA annotations requires
**52-68 hours** (7-9 developer days). The entities vary significantly in
complexity, with **Analyzer** being the simplest (standalone, no relationships)
and **AnalyzerFieldMapping** being the most complex (custom version management,
3 manual FKs, 2 enums).

**Recommendation**: Execute as dedicated 2-week sprint **after M18 completion**.

---

## Entity Overview

| Entity                   | Package                       | Properties       | Relationships  | Complexity | Effort |
| ------------------------ | ----------------------------- | ---------------- | -------------- | ---------- | ------ |
| **Analyzer**             | `analyzer.valueholder`        | 8                | 0              | LOW        | 6-8h   |
| **AnalyzerField**        | `analyzer.valueholder`        | 7 + 1 enum       | 2 `@ManyToOne` | MEDIUM     | 8-10h  |
| **AnalyzerFieldMapping** | `analyzer.valueholder`        | 10 + 2 enums     | 3 manual FKs   | HIGH       | 12-16h |
| **AnalyzerResults**      | `analyzerresults.valueholder` | 12               | 0 (manual FKs) | MEDIUM     | 8-10h  |
| **AnalyzerTestMapping**  | `analyzerimport.valueholder`  | 3 + composite PK | 1 manual FK    | MED-HIGH   | 10-12h |

---

## Entity 1: Analyzer (ROOT)

**Files**:

- `src/main/resources/hibernate/hbm/Analyzer.hbm.xml`
- `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`

**Current Structure**:

```
┌────────────────────────────────────────┐
│            Analyzer (XML)              │
├────────────────────────────────────────┤
│ id: String (LIMSStringNumberUserType)  │ ← Custom ID generator
│ name: String (20)                      │
│ machineId: String (20)                 │
│ type: String → analyzer_type (30)      │ ← Column rename
│ description: String (60)               │
│ location: String (60)                  │
│ active: Boolean → is_active            │ ← Column rename
│ hasSetupPage: Boolean → has_setup_page │ ← Column rename
├────────────────────────────────────────┤
│ RELATIONSHIPS: NONE (standalone)       │
└────────────────────────────────────────┘
```

**Migration Tasks**: | Task | Description | Effort |
|------|-------------|--------| | Add JPA annotations | `@Entity`, `@Table`,
`@Id`, `@Column` | 1h | | ID generator migration | Replace
`LIMSStringNumberUserType` with `@GenericGenerator` | 2h | | Column name mapping
| `@Column(name = "is_active")` for `active` field, etc. | 30min | | Update
hibernate.cfg.xml | Change mapping registration | 15min | | ORM validation test
| Add to HibernateMappingValidationTest | 1h | | Integration testing | Test all
110 referencing files | 2h |

**Target Annotation Structure**:

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

**Risk**: LOW - Standalone entity, no relationships **Effort**: 6-8 hours

---

## Entity 2: AnalyzerField

**Files**:

- `src/main/resources/hibernate/hbm/AnalyzerField.hbm.xml`
- `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerField.java`

**Current Structure**:

```
┌──────────────────────────────────────────┐
│          AnalyzerField (XML)             │
├──────────────────────────────────────────┤
│ id: String (UUID, assigned)              │
│ analyzer: Analyzer (@ManyToOne)          │ ← Relationship to XML entity
│ fieldName: String (255)                  │
│ astmRef: String (50)                     │
│ fieldType: FieldType (ENUM)              │ ← Inner enum
│ unit: String (50)                        │
│ customFieldType: CustomFieldType         │ ← @ManyToOne to annotation entity
│ isActive: Boolean                        │
├──────────────────────────────────────────┤
│ RELATIONSHIPS:                           │
│ • ManyToOne → Analyzer (becomes Ann→Ann) │
│ • ManyToOne → CustomFieldType (Ann)      │
└──────────────────────────────────────────┘
```

**Migration Tasks**: | Task | Description | Effort |
|------|-------------|--------| | Add JPA annotations | `@Entity`, `@Table`,
`@ManyToOne`, `@Enumerated` | 2h | | Relationship migration | Convert XML
`<many-to-one>` to JPA annotations | 1h | | Enum annotation |
`@Enumerated(EnumType.STRING)` for FieldType | 30min | | Update
hibernate.cfg.xml | Remove XML, add annotated class | 15min | | ORM validation
test | Add to test suite | 1h | | Integration testing | Field mapping workflows
| 3h |

**Target Annotation Structure**:

```java
@Entity
@Table(name = "analyzer_field")
public class AnalyzerField extends BaseObject<String> {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @Column(name = "field_name", length = 255, nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type")
    private FieldType fieldType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_type_id")
    private CustomFieldType customFieldType;
}
```

**Dependency**: Requires Analyzer migration first **Risk**: MEDIUM **Effort**:
8-10 hours

---

## Entity 3: AnalyzerFieldMapping (MOST COMPLEX)

**Files**:

- `src/main/resources/hibernate/hbm/AnalyzerFieldMapping.hbm.xml`
- `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerFieldMapping.java`

**Current Structure**:

```
┌──────────────────────────────────────────────────────┐
│           AnalyzerFieldMapping (XML)                 │
├──────────────────────────────────────────────────────┤
│ id: String (UUID)                                    │
│ version: long (CUSTOM @Version - NOT BaseObject)     │ ← COMPLEXITY
│ lastupdated: Timestamp (mapped as property)          │ ← Override BaseObject
│ analyzerFieldId: String (manual FK)                  │ ← Manual FK #1
│ analyzerId: String (LIMSStringNumberUserType FK)     │ ← Manual FK #2
│ openelisFieldId: String (manual FK)                  │ ← Manual FK #3
│ openelisFieldType: OpenELISFieldType (ENUM)          │ ← Inner enum #1
│ mappingType: MappingType (ENUM)                      │ ← Inner enum #2
│ isRequired: Boolean                                  │
│ isActive: Boolean                                    │
│ specimenTypeConstraint: String (50)                  │
│ panelConstraint: String (50)                         │
├──────────────────────────────────────────────────────┤
│ COMPLEXITY FACTORS:                                  │
│ • Custom version column (not BaseObject's)           │
│ • 3 manual FKs (deliberate design choice)            │
│ • 2 inner enum types                                 │
│ • LIMSStringNumberUserType for analyzerId            │
└──────────────────────────────────────────────────────┘
```

**Migration Tasks**: | Task | Description | Effort |
|------|-------------|--------| | Add JPA annotations | `@Entity`, `@Table`, all
column mappings | 2h | | Custom version handling | Override BaseObject's
`@Version` | 2h | | Convert manual FKs to `@ManyToOne` | 3 relationships | 2-4h
| | Enum annotations | Both enums with `@Enumerated` | 1h | |
LIMSStringNumberUserType | Create converter or `@Type` | 1h | | ORM validation
test | Complex due to custom version | 2h | | Integration testing | Mapping
workflows | 3h |

**Key Decision**: Manual FKs vs `@ManyToOne`

| Option                  | Pros                 | Cons                      |
| ----------------------- | -------------------- | ------------------------- |
| Keep manual FKs         | Less refactoring     | Verbose, manual hydration |
| Convert to `@ManyToOne` | Clean JPA, type-safe | Service layer changes     |

**Recommendation**: Convert to `@ManyToOne` after Analyzer + AnalyzerField
migrated

**Dependency**: Requires Analyzer AND AnalyzerField migration first **Risk**:
MEDIUM-HIGH **Effort**: 12-16 hours

---

## Entity 4: AnalyzerResults

**Files**:

- `src/main/resources/hibernate/hbm/AnalyzerResults.hbm.xml`
- `src/main/java/org/openelisglobal/analyzerresults/valueholder/AnalyzerResults.java`

**Note**: Different package (`analyzerresults` not `analyzer`)

**Current Structure**:

```
┌──────────────────────────────────────────┐
│         AnalyzerResults (XML)            │
├──────────────────────────────────────────┤
│ id: String (LIMSStringNumberUserType)    │ ← Custom ID
│ lastupdated: Timestamp (@Version)        │
│ analyzerId: String (manual FK)           │ ← Manual FK
│ accessionNumber: String (20)             │
│ testName: String                         │
│ result: String                           │
│ units: String                            │
│ isControl: Boolean                       │
│ duplicateAnalyzerResultId: String (FK)   │ ← Self-reference
│ ReadOnly: Boolean                        │ ← Capitalized name
│ completeDate: Timestamp                  │
│ testId: String (LIMSStringNumberUserType)│ ← FK to Test
│ resultType: String (1)                   │
├──────────────────────────────────────────┤
│ RELATIONSHIPS: Manual FKs only           │
└──────────────────────────────────────────┘
```

**Migration Tasks**: | Task | Description | Effort |
|------|-------------|--------| | Add JPA annotations | All 12 columns | 2h | |
ID generator migration | `@GenericGenerator` with sequence | 1h | | Decision on
manual FKs | Keep or convert | 2h | | Property name fix | `ReadOnly` →
`readOnly` | 30min | | ORM validation test | Add to test suite | 1h | |
Integration testing | Results import, paging | 3h |

**Dependency**: Requires Analyzer migration first **Risk**: MEDIUM **Effort**:
8-10 hours

---

## Entity 5: AnalyzerTestMapping

**Files**:

- `src/main/resources/hibernate/hbm/AnalyzerTestMapping.hbm.xml`
- `src/main/java/org/openelisglobal/analyzerimport/valueholder/AnalyzerTestMapping.java`

**Note**: Different package (`analyzerimport` not `analyzer`)

**Current Structure**:

```
┌──────────────────────────────────────────────────────┐
│          AnalyzerTestMapping (XML)                   │
├──────────────────────────────────────────────────────┤
│ COMPOSITE PRIMARY KEY:                               │
│   analyzerId: String (LIMSStringNumberUserType)      │ ← Part of PK
│   analyzerTestName: String                           │ ← Part of PK
├──────────────────────────────────────────────────────┤
│ lastupdated: Timestamp (@Version)                    │
│ testId: String (LIMSStringNumberUserType)            │ ← FK to Test
├──────────────────────────────────────────────────────┤
│ COMPLEXITY: @EmbeddedId or @IdClass required         │
└──────────────────────────────────────────────────────┘
```

**Migration Tasks**: | Task | Description | Effort |
|------|-------------|--------| | Composite PK annotation | `@EmbeddedId` with
PK class | 3h | | Add JPA annotations | `@Embeddable` for PK class | 2h | |
LIMSStringNumberUserType | Handle in composite PK | 2h | | ORM validation test |
Composite PK testing | 1h | | Integration testing | Test name cache, mapping
service | 2h |

**Target Annotation Structure**:

```java
@Embeddable
public class AnalyzerTestMappingPK implements Serializable {
    @Column(name = "analyzer_id")
    @Type(LIMSStringNumberUserType.class)
    private String analyzerId;

    @Column(name = "analyzer_test_name")
    private String analyzerTestName;

    // equals(), hashCode() required
}

@Entity
@Table(name = "analyzer_test_map")
public class AnalyzerTestMapping extends BaseObject<AnalyzerTestMappingPK> {

    @EmbeddedId
    private AnalyzerTestMappingPK compoundId;

    @Version
    @Column(name = "LASTUPDATED")
    private Timestamp lastupdated;

    @Column(name = "test_id")
    @Type(LIMSStringNumberUserType.class)
    private String testId;
}
```

**Dependency**: Requires Analyzer migration first **Risk**: MEDIUM **Effort**:
10-12 hours

---

## Migration Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│               MIGRATION DEPENDENCY GRAPH                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Phase 1 (FOUNDATION - No dependencies)                    │
│   ════════════════════════════════════                      │
│        ╔═══════════════╗                                    │
│        ║   Analyzer    ║  ← ROOT ENTITY (migrate first)     │
│        ╚═══════╤═══════╝                                    │
│                │                                             │
│   Phase 2 (DEPENDS ON ANALYZER - can parallelize)           │
│   ═══════════════════════════════════════════               │
│                ↓                                             │
│        ┌───────┼───────┐                                    │
│        ↓       ↓       ↓                                    │
│  ┌───────────┐ ┌────────────────┐ ┌──────────────────┐     │
│  │Analyzer   │ │AnalyzerResults │ │AnalyzerTestMap   │     │
│  │Field      │ │(different pkg) │ │(composite PK)    │     │
│  └─────┬─────┘ └────────────────┘ └──────────────────┘     │
│        │                                                     │
│   Phase 3 (DEPENDS ON ANALYZERFIELD)                        │
│   ══════════════════════════════════                        │
│        ↓                                                     │
│  ┌──────────────────────┐                                   │
│  │AnalyzerFieldMapping  │  ← MOST COMPLEX (migrate last)    │
│  └──────────────────────┘                                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Impact Analysis

### Files Affected

| Category                | Count | Changes Required                 |
| ----------------------- | ----- | -------------------------------- |
| Entity classes          | 5     | Add annotations                  |
| XML mapping files       | 5     | Delete                           |
| hibernate.cfg.xml       | 1     | Update registrations             |
| DAO implementations     | ~5    | Update HQL if FK patterns change |
| Service implementations | ~10   | Update if converting manual FKs  |
| ORM validation test     | 1     | Add 5 entity validations         |
| Integration tests       | ~15   | Add/update                       |

### External Dependencies

| Dependency                | Count | Impact                         |
| ------------------------- | ----- | ------------------------------ |
| Files importing Analyzer  | 110   | API unchanged, no code changes |
| Analyzer plugins          | 11    | Regression testing required    |
| AnalyzerResults workflows | ~20   | Integration testing            |

---

## Total Effort Summary

| Phase    | Entity                | Effort | Cumulative |
| -------- | --------------------- | ------ | ---------- |
| Phase 1  | Analyzer              | 6-8h   | 6-8h       |
| Phase 2a | AnalyzerField         | 8-10h  | 14-18h     |
| Phase 2b | AnalyzerResults       | 8-10h  | 22-28h     |
| Phase 2c | AnalyzerTestMapping   | 10-12h | 32-40h     |
| Phase 3  | AnalyzerFieldMapping  | 12-16h | 44-56h     |
| Testing  | Integration + plugins | 8-12h  | **52-68h** |

**Total**: **52-68 hours** (7-9 developer days)

---

## Resource Options

| Option                  | Duration  | Risk   | Notes                               |
| ----------------------- | --------- | ------ | ----------------------------------- |
| 1 senior developer      | 2-3 weeks | Medium | Knowledge concentration risk        |
| 2 developers (parallel) | 1-2 weeks | Low    | Recommended - enables code review   |
| Alongside M2-M18        | N/A       | High   | NOT recommended - context switching |

---

## Recommendation

**Execute as dedicated 2-week sprint after M18 completion**

**Rationale**:

1. Feature 011 (M2-M18) uses manual FK pattern successfully
2. Migration during active development creates instability risk
3. Post-M18, team has full context on analyzer domain
4. 2-week focused sprint minimizes context switching

**Pre-Migration Checklist**:

- [ ] M18 completed and merged to develop
- [ ] All analyzer E2E tests passing
- [ ] 11 analyzer plugins verified working
- [ ] Dedicated 2-week sprint scheduled
- [ ] 2 developers assigned (1 lead, 1 support)
- [ ] Rollback plan documented

---

## References

- [hibernate-xml-vs-annotation-analysis.md](hibernate-xml-vs-annotation-analysis.md) -
  Full technical deep dive
- [specification-analysis-report.md](specification-analysis-report.md) -
  CRITICAL D1 and HIGH D2 issues
- [constitution.md](../../../.specify/memory/constitution.md) - Principle IV
  legacy extension exception
- [AnalyzerFieldMapping.hbm.xml](../../../src/main/resources/hibernate/hbm/AnalyzerFieldMapping.hbm.xml) -
  Manual FK pattern documentation (lines 30-33)

---

**Analysis Complete**: 2026-01-28 **Next Review**: Post-M18 completion
