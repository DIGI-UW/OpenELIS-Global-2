# Technical Research: Patient Merge Backend

**Feature**: Patient Merge Backend Implementation **Branch**:
`008-patient-merge-backend` **Date**: 2025-12-05 **Status**: Phase 0 Complete
(with 1 blocker tracked)

## Purpose

This document captures technical research, decisions, and patterns for
implementing the patient merge backend functionality. All decisions follow the
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md) and
leverage existing patterns from the codebase.

---

## 1. Transaction Management Strategy

### Decision: Use Spring `@Transactional` with READ_COMMITTED Isolation

**Research Context**:

- Patient merge involves updates to 10+ related tables (sample_human,
  patient_identity, patient_contact, etc.)
- Merge operation must be atomic - all changes succeed or all fail (no partial
  merges)
- Need to prevent concurrent merge attempts on same patients

**Options Evaluated**:

1. **Manual transaction management** (EntityManager.getTransaction())

   - ❌ More error-prone, verbose code
   - ❌ Doesn't integrate well with Spring's declarative approach

2. **@Transactional with default isolation** (READ_COMMITTED)

   - ✅ Declarative, clean code
   - ✅ Spring handles rollback automatically on exceptions
   - ✅ READ_COMMITTED prevents dirty reads while allowing concurrent
     transactions

3. **@Transactional(isolation = SERIALIZABLE)**
   - ❌ Overkill - too restrictive, reduces concurrency
   - ❌ Performance impact for high-load scenarios

**Decision**: Use `@Transactional` annotation on service methods with default
READ_COMMITTED isolation.

**Implementation Pattern**:

```java
@Service
public class PatientMergeServiceImpl implements PatientMergeService {

    @Transactional  // Default: propagation=REQUIRED, isolation=READ_COMMITTED
    @Override
    public PatientMergeAudit executeMerge(PatientMergeRequest request) {
        // All database operations within single transaction
        // Automatic rollback on any RuntimeException
    }
}
```

**Rationale**:

- Constitution Principle IV mandates `@Transactional` in service layer only (NOT
  controllers)
- Existing OpenELIS patterns use `@Transactional` extensively (sample storage,
  result entry)
- Spring's rollback mechanism handles failure scenarios automatically

---

## 2. FHIR R4 Patient Link Relationships

### Decision: Use FHIR `link` with type "replaces" and "replaced-by"

**Research Context**:

- FHIR R4 Patient resource provides `link` array for patient record
  relationships
- Need to represent "Patient A replaces Patient B" relationship
- Must maintain historical reference to merged patient for audit purposes

**FHIR R4 Specification** (https://hl7.org/fhir/R4/patient.html#link):

- `link.other`: Reference to related Patient resource
- `link.type`: Relationship type (replaces, replaced-by, refer, seealso)
- `replaces`: This patient record replaces the linked record
- `replaced-by`: This patient record has been replaced by the linked record

**Implementation Pattern**:

**Primary Patient** (the one that remains active):

```json
{
  "resourceType": "Patient",
  "id": "PAT-12345",
  "active": true,
  "link": [
    {
      "other": { "reference": "Patient/PAT-67890" },
      "type": "replaces"
    }
  ]
}
```

**Merged Patient** (marked inactive):

```json
{
  "resourceType": "Patient",
  "id": "PAT-67890",
  "active": false,
  "link": [
    {
      "other": { "reference": "Patient/PAT-12345" },
      "type": "replaced-by"
    }
  ]
}
```

**Decision**: Implement bidirectional links (primary has "replaces", merged has
"replaced-by").

**Rationale**:

- FHIR R4 standard pattern for patient merging
- Bidirectional links enable traversal in both directions
- Constitution Principle III mandates FHIR R4 compliance
- Existing OpenELIS FHIR integration uses HAPI FHIR library (6.6.2)

**Existing Services to Leverage**:

- `FhirPersistanceService` - Update/create FHIR resources
- `FhirTransformService` - Transform domain objects to FHIR resources
- Pattern from: `src/main/java/org/openelisglobal/fhir/service/`

---

## 3. Concurrency Control and Locking

### Decision: Use PostgreSQL row-level locking with SELECT FOR UPDATE

**Research Context**:

- Prevent two users from merging same patient simultaneously
- Need to lock both patients being merged
- Must avoid deadlocks

**Options Evaluated**:

1. **Optimistic locking** (@Version column)

   - ❌ Doesn't prevent concurrent merge attempts
   - ❌ Requires retry logic after failure

2. **Pessimistic locking** (SELECT FOR UPDATE)
   - ✅ Prevents concurrent access at database level
   - ✅ First transaction acquires lock, second waits or fails
   - ✅ PostgreSQL handles deadlock detection automatically

**Decision**: Use `SELECT FOR UPDATE` in DAO layer when fetching patients for
merge.

**Implementation Pattern**:

```java
@Query("SELECT p FROM Patient p WHERE p.id = :patientId FOR UPDATE")
Patient findByIdForUpdate(@Param("patientId") String patientId);

// In service:
Patient primary = patientDAO.findByIdForUpdate(primaryPatientId);  // Locks row
Patient merged = patientDAO.findByIdForUpdate(mergedPatientId);    // Locks row
// Proceed with merge - other transactions will wait or timeout
```

**Rationale**:

- PostgreSQL default: Wait for lock release or timeout (30 seconds default)
- Prevents duplicate processing
- Constitution-compliant (no custom complexity)

---

## 4. Batch UPDATE Performance Optimization

### Decision: Use HQL (Hibernate Query Language) bulk UPDATE statements for foreign key updates

**Research Context**:

- Need to update patient_id in 10+ related tables (sample_human,
  patient_identity, etc.)
- Patient with 500 results = 500+ rows to update
- Performance requirement: Complete within 30 seconds

**Options Evaluated**:

1. **Individual entity updates** (for each loop with save())

   - ❌ N queries for N entities (very slow)
   - ❌ 500 results = 500 UPDATE statements

2. **Native SQL batch UPDATE**

   - ❌ Bypasses Hibernate second-level cache
   - ❌ Doesn't trigger entity lifecycle events

3. **HQL bulk UPDATE** (Hibernate's native query language)
   - ✅ Single UPDATE statement per table
   - ✅ Hibernate handles batching efficiently
   - ✅ Remains ORM-compliant
   - ✅ Native to Hibernate (OpenELIS uses Hibernate as JPA provider)

**Decision**: Use HQL bulk UPDATE statements for foreign key updates.

**Implementation Pattern**:

```java
@Modifying
@Query("UPDATE SampleHuman s SET s.patient.id = :primaryPatientId WHERE s.patient.id = :mergedPatientId")
int updateSampleHumanPatientId(@Param("primaryPatientId") String primaryPatientId,
                                @Param("mergedPatientId") String mergedPatientId);

// In service:
int updatedSamples = sampleHumanDAO.updateSampleHumanPatientId(primaryId, mergedId);
int updatedIdentities = patientIdentityDAO.updatePatientId(primaryId, mergedId);
// Continue for all related tables
```

**Rationale**:

- Single UPDATE per table (10 UPDATEs total vs 500+)
- OpenELIS uses Hibernate as JPA provider - HQL is native query language
- OpenELIS already uses @Modifying @Query pattern extensively
- Constitution Principle IV explicitly mentions "JPA/Hibernate" - Hibernate is
  acceptable
- Meets 30-second performance requirement easily

---

## 5. Audit Trail Design

### Decision: JSONB column for data summary, separate audit table for merge operations

**Research Context**:

- Need to track: who performed merge, when, which patients, why, what data was
  affected
- Data summary includes counts: orders, results, samples, identifiers
- Must link to OpenELIS audit infrastructure

**PostgreSQL JSONB Benefits**:

- Flexible structure (can add fields without migration)
- Queryable (can search within JSON)
- Efficient storage and indexing

**Decision**: Create `patient_merge_audit` table with JSONB `data_summary`
column.

**Schema Design**:

```sql
CREATE TABLE patient_merge_audit (
    id BIGSERIAL PRIMARY KEY,
    primary_patient_id BIGINT NOT NULL REFERENCES patient(id),
    merged_patient_id BIGINT NOT NULL REFERENCES patient(id),
    merge_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by_user_id BIGINT NOT NULL REFERENCES system_user(id),
    reason TEXT NOT NULL,
    data_summary JSONB,  -- {orders: {total: 23, active: 2}, results: {total: 47}, ...}
    CONSTRAINT fk_primary_patient FOREIGN KEY (primary_patient_id) REFERENCES patient(id),
    CONSTRAINT fk_merged_patient FOREIGN KEY (merged_patient_id) REFERENCES patient(id),
    CONSTRAINT fk_performed_by FOREIGN KEY (performed_by_user_id) REFERENCES system_user(id)
);

CREATE INDEX idx_patient_merge_audit_primary ON patient_merge_audit(primary_patient_id);
CREATE INDEX idx_patient_merge_audit_merged ON patient_merge_audit(merged_patient_id);
CREATE INDEX idx_patient_merge_audit_date ON patient_merge_audit(merge_date);
CREATE INDEX idx_patient_merge_audit_data_summary ON patient_merge_audit USING gin(data_summary);
```

**Rationale**:

- JSONB allows flexible data summary without rigid schema
- GIN index enables fast queries on JSON fields
- Follows OpenELIS pattern (other audit tables exist)
- Constitution Principle VIII mandates comprehensive audit trail

---

## 6. Permission Enforcement Strategy

### Decision: Dual-layer permission checks (service + controller)

**Research Context**:

- Only Global Administrators should merge patients
- Need defense in depth (multiple layers of security)
- Must prevent unauthorized API access

**Decision**: Check Global Administrator permission at both service and
controller layers.

**Implementation Pattern**:

```java
// Controller layer (HTTP boundary)
@RestController
@RequestMapping("/api/patient/merge")
@PreAuthorize("hasRole('ROLE_GLOBAL_ADMIN')")  // Spring Security annotation
public class PatientMergeRestController {

    @PostMapping("/execute")
    public ResponseEntity<PatientMergeResult> executeMerge(@RequestBody PatientMergeForm form) {
        // Spring Security already validated permission
        return patientMergeService.executeMerge(form);
    }
}

// Service layer (business logic boundary)
@Service
public class PatientMergeServiceImpl implements PatientMergeService {

    @Transactional
    public PatientMergeAudit executeMerge(PatientMergeRequest request) {
        // Double-check permission (defense in depth)
        if (!hasGlobalAdminPermission(getCurrentUser())) {
            throw new PermissionException("Global Administrator permission required");
        }
        // Proceed with merge
    }
}
```

**Rationale**:

- Constitution Principle VIII mandates RBAC enforcement
- Defense in depth: controller prevents HTTP access, service prevents
  programmatic bypass
- OpenELIS uses Spring Security extensively (existing pattern)
- Existing pattern from `@PreAuthorize` usage in other controllers

---

## 7. Identifier Preservation Strategy

### 🔴 BLOCKER: OpenELIS Constraint on Duplicate Identifier Types

**CRITICAL ISSUE**: OpenELIS does **NOT support multiple identifiers of the same
type** for a single patient. This creates a conflict during patient merge
operations.

**See**: [`BLOCKERS.md`](./BLOCKERS.md) for full details and PM clarification
questions.

**Summary of Issue**:

- If Patient A has National ID "123" and Patient B has National ID "456"
- Merging B → A would result in A having TWO National IDs
- **OpenELIS application logic cannot handle this** (expects single value per
  type)
- FHIR R4 CAN handle this (identifier array supports multiple of same type)

**Pending PM Decision**: Which approach to take:

- **Option A**: Primary patient wins (discard merged patient's duplicate
  identifiers)
- **Option B**: User selects which identifier to keep during merge
- **Option C**: Store both with "active" flag (requires schema changes)
- **Option D**: Block merge if duplicate identifier types exist

**Implementation BLOCKED** until PM clarifies approach.

**Temporary Documentation** (assuming Option A for now):

### Layer 1: PostgreSQL Database (patient_identity table)

**Objective**: Consolidate identifiers while avoiding duplicates of same type.

**Implementation Pattern** (Option A - Primary Wins):

```java
@Transactional
public void consolidateIdentifiers(Patient primary, Patient merged) {
    for (PatientIdentity mergedIdentity : merged.getIdentities()) {
        // Check if primary already has this identity type
        if (primary.hasIdentityOfType(mergedIdentity.getIdentityType())) {
            // CONFLICT: Primary patient wins, log discarded value
            log.warn("Identifier conflict: Keeping primary patient's {}, discarding merged patient's {}",
                primary.getIdentityValue(mergedIdentity.getIdentityType()),
                mergedIdentity.getIdentityData());
            recordDiscardedIdentifier(primary, merged, mergedIdentity);
        } else {
            // No conflict: Add to primary
            int updated = patientIdentityDAO.updatePatientId(
                primary.getId(), merged.getId(), mergedIdentity.getIdentityType());
        }
    }
}
```

### Layer 2: FHIR Patient Resource

**FHIR Representation** (showing preserved identifiers only):

```json
{
  "resourceType": "Patient",
  "id": "PAT-12345",
  "active": true,
  "identifier": [
    {
      "system": "http://openelis-global.org/patient-id",
      "value": "PAT-12345",
      "use": "official"
    },
    {
      "system": "http://openelis-global.org/patient-id",
      "value": "PAT-67890",
      "use": "old",
      "period": { "end": "2024-11-19T10:30:00Z" }
    },
    {
      "system": "http://country.gov/national-id",
      "value": "1234567890",
      "use": "official"
    }
    // Note: Merged patient's National ID NOT included if duplicate type
  ]
}
```

**Status**: ⏸️ **Awaiting PM clarification** before finalizing implementation.

---

## 8. Related Table Updates

### Decision: Update all related tables in dependency order

**Tables to Update**:

1. `patient_identity` - Patient identifiers
2. `patient_contact` - Contact information
3. `external_patient_id` - External system identifiers
4. `patient_relations` - Family/related patient relationships
5. `sample_human` - Sample records
6. `electronic_order` - Electronic orders

**Implementation Pattern**:

```java
@Transactional
public PatientMergeAudit executeMerge(PatientMergeRequest request) {
    // 1. Lock patients
    Patient primary = patientDAO.findByIdForUpdate(request.getPrimaryPatientId());
    Patient merged = patientDAO.findByIdForUpdate(request.getMergedPatientId());

    // 2. Update related tables (dependency order)
    consolidateIdentifiers(primary, merged);  // Handle duplicates per PM decision
    int contacts = patientContactDAO.updatePatientId(primary.getId(), merged.getId());
    int externalIds = externalPatientIdDAO.updatePatientId(primary.getId(), merged.getId());
    int relations = patientRelationsDAO.updatePatientId(primary.getId(), merged.getId());
    int samples = sampleHumanDAO.updatePatientId(primary.getId(), merged.getId());
    int orders = electronicOrderDAO.updatePatientId(primary.getId(), merged.getId());

    // 3. Mark merged patient
    merged.setIsMerged(true);
    merged.setMergedIntoPatientId(primary.getId());
    merged.setMergeDate(new Date());
    patientDAO.save(merged);

    // 4. Update FHIR
    fhirPatientLinkService.createMergeLinks(primary.getId(), merged.getId());

    // 5. Create audit
    return createMergeAudit(primary, merged, request, dataSummary);
}
```

---

## 9. Error Handling and Validation

### Decision: Fail-fast validation before transaction

**Implementation Pattern**:

```java
public PatientMergeValidationResult validateMerge(PatientMergeRequest request) {
    List<String> errors = new ArrayList<>();

    // Fast pre-checks
    if (!hasGlobalAdminPermission(getCurrentUser())) {
        errors.add("Global Administrator permission required");
    }

    if (request.getPatient1Id().equals(request.getPatient2Id())) {
        errors.add("Cannot merge patient with itself");
    }

    if (errors.isEmpty()) {
        return validateWithDatabase(request);  // Database validation
    }

    return PatientMergeValidationResult.failed(errors);
}
```

---

## 10. FHIR Resource Updates Beyond Patient

### Decision: Update all FHIR resources referencing merged patient

**Resources to Update**:

1. ServiceRequest - Lab orders
2. Specimen - Sample specimens
3. Observation - Lab results
4. DiagnosticReport - Result reports

**Implementation Pattern**:

```java
public void updateFhirResourceReferences(String primaryPatientId, String mergedPatientId) {
    // Update ServiceRequest resources
    Bundle orders = fhirClient.search()
        .forResource(ServiceRequest.class)
        .where(ServiceRequest.SUBJECT.hasId("Patient/" + mergedPatientId))
        .returnBundle(Bundle.class)
        .execute();

    for (Bundle.BundleEntryComponent entry : orders.getEntry()) {
        ServiceRequest order = (ServiceRequest) entry.getResource();
        order.setSubject(new Reference("Patient/" + primaryPatientId));
        fhirPersistanceService.updateFhirResource(order);
    }
    // Repeat for Specimen, Observation, DiagnosticReport
}
```

---

## Summary of Key Decisions

| Decision Area           | Choice                                    | Status         |
| ----------------------- | ----------------------------------------- | -------------- |
| Transaction Management  | @Transactional with READ_COMMITTED        | ✅ Decided     |
| FHIR Links              | Bidirectional (replaces/replaced-by)      | ✅ Decided     |
| Concurrency Control     | SELECT FOR UPDATE row locking             | ✅ Decided     |
| Batch Updates           | HQL bulk UPDATE statements                | ✅ Decided     |
| Audit Trail             | JSONB data_summary column                 | ✅ Decided     |
| Permission Enforcement  | Dual-layer (service + controller)         | ✅ Decided     |
| Identifier Preservation | **PENDING PM DECISION**                   | 🔴 **BLOCKED** |
| Table Update Order      | Dependency-ordered updates                | ✅ Decided     |
| Error Handling          | Fail-fast validation + exception rollback | ✅ Decided     |
| FHIR Resource Updates   | Update all resource types                 | ✅ Decided     |

---

## References

- **FHIR R4 Patient**: https://hl7.org/fhir/R4/patient.html
- **FHIR R4 Patient.link**:
  https://hl7.org/fhir/R4/patient-definitions.html#Patient.link
- **Spring @Transactional**:
  https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
- **PostgreSQL Locking**:
  https://www.postgresql.org/docs/current/explicit-locking.html
- **HAPI FHIR Documentation**: https://hapifhir.io/hapi-fhir/docs/
- **OpenELIS Constitution**:
  [../../.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- **OpenELIS FHIR Integration**: `src/main/java/org/openelisglobal/fhir/`

---

**Research Status**: ✅ Complete (with 1 blocker tracked in BLOCKERS.md) **Next
Phase**: Design (data-model.md, contracts, quickstart.md)
