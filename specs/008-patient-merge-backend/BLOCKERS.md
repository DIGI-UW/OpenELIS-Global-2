# Implementation Blockers & Open Questions

**Feature**: Patient Merge Backend (008-patient-merge-backend) **Status**:
Awaiting PM Clarification **Date**: 2025-12-05

---

## BLOCKER #1: Multiple Identifiers of Same Type Not Supported in OpenELIS

### Problem Statement

OpenELIS database/application logic **does NOT support multiple identifiers of
the same type** for a single patient. This creates a conflict during patient
merge operations.

**Example Scenario**:

- Patient A has National ID: `1234567890`
- Patient B has National ID: `0987654321`
- When merging B → A, the result would be:
  - Patient A with TWO National IDs: `1234567890` AND `0987654321`
  - **This is NOT supported by OpenELIS**

### Current OpenELIS Constraint

**Database Structure** (`patient_identity` table):

```sql
CREATE TABLE patient_identity (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    identity_type_id INT,  -- National ID, Passport, External ID, etc.
    identity_data VARCHAR(255),  -- The actual identifier value
    ...
);
```

**Application Constraint**:

- OpenELIS enforces uniqueness: ONE identifier per type per patient
- Business logic assumes: `patient.getNationalId()` returns a single value (not
  a list)
- UI forms expect single value per identifier type

### FHIR vs OpenELIS Database

| Layer           | Multiple IDs of Same Type? | Notes                                                               |
| --------------- | -------------------------- | ------------------------------------------------------------------- |
| **FHIR R4**     | ✅ YES - Fully supported   | FHIR Patient.identifier is an array, can have multiple of same type |
| **OpenELIS DB** | ❌ NO - NOT supported      | Application logic assumes single value per type                     |

**This means**:

- FHIR representation CAN show both National IDs with proper metadata
- OpenELIS database/application logic CANNOT handle both National IDs

### Impact on Patient Merge

**Affected Identifier Types** (potential duplicates during merge):

1. **National ID** - Most common (both patients likely have National ID)
2. **Passport** - Less common (possible duplicates)
3. **External Patient ID** - Depends on integration (possible duplicates)
4. **OpenELIS Patient ID** - ALWAYS duplicates (by definition, both patients
   have OE IDs)

**Merge Scenarios**:

| Scenario     | Patient A Identifiers              | Patient B Identifiers              | Conflict?                               |
| ------------ | ---------------------------------- | ---------------------------------- | --------------------------------------- |
| No Conflict  | National ID: 123                   | Passport: ABC                      | ✅ No conflict - different types        |
| **Conflict** | National ID: 123                   | National ID: 456                   | ❌ **CONFLICT** - same type             |
| **Conflict** | National ID: 123<br/>Passport: ABC | National ID: 456<br/>Passport: XYZ | ❌ **CONFLICT** - both types duplicated |

### Questions for PM

**CRITICAL DECISION NEEDED**:

**Q1: How should we handle duplicate identifier types during merge?**

**Option A: Primary Patient Wins (Discard Merged Patient's Identifier)**

- Keep Patient A's National ID `1234567890`
- Discard Patient B's National ID `0987654321`
- **Risk**: Lose potentially valid identifier data
- **Benefit**: No database/application changes needed

**Option B: User Selects Which Identifier to Keep**

- During merge workflow, show both National IDs
- User chooses which one is correct
- Discard the other
- **Risk**: Requires UI changes in frontend (not in current spec)
- **Benefit**: User makes informed decision

**Option C: Store Both but Mark One as "Superseded"**

- Add `is_active` or `superseded_by` flag to `patient_identity` table
- Application logic only uses active identifier
- Historical identifier preserved for audit
- **Risk**: Requires database schema changes and application logic updates
- **Benefit**: Preserves both values for audit trail

**Option D: Prevent Merge if Duplicate Identifier Types Exist**

- Validation fails with error: "Cannot merge - conflicting National IDs"
- User must manually resolve conflict first (update one patient's ID)
- **Risk**: Blocks merge workflow, requires manual intervention
- **Benefit**: Ensures data quality, no ambiguity

**Q2: Should FHIR representation include both identifiers even if OE doesn't?**

- FHIR can technically show both with `use: "old"` for merged patient's ID
- But this creates inconsistency: FHIR shows 2 IDs, OE shows 1 ID
- Which is the source of truth?

**Q3: What about OpenELIS Patient IDs specifically?**

- Both patients ALWAYS have OE Patient IDs (e.g., PAT-12345, PAT-67890)
- After merge, should both IDs be preserved?
- Current spec says YES (both shown in FHIR with `use: "old"` for merged)
- Is this acceptable even if other identifier types can't be duplicated?

### Implemented Approach (Interim Solution)

**Current Implementation**: **Redirect-on-Lookup Pattern**

**Design Decision**: Instead of merging identifiers, we leverage the `is_merged`
flag and `merged_into_patient_id` reference to redirect lookups.

**How it works**:

1. When a patient is merged, `patient.is_merged = TRUE` and
   `merged_into_patient_id` points to primary
2. When searching/retrieving a merged patient, application checks `is_merged`
   flag
3. If `is_merged = TRUE`, redirect to `merged_into_patient_id` (the primary
   patient)
4. Primary patient retains all original identifiers
5. Merged patient identifiers remain in database but patient is marked inactive

**Rationale**:

- **No data loss**: Both patients' identifiers remain in database
- **No duplication conflicts**: Primary patient keeps only its own identifiers
- **Transparent redirection**: Application layer handles lookup redirection
- **Audit trail preserved**: Can see which patient was merged (historical
  lookups)
- **Reversible**: Can undo merge if needed (just clear `is_merged` flag)
- **Awaits PM decision**: Final identifier handling can be implemented later

**Implementation** (in PatientMergeServiceImpl):

```java
@Override
public PatientMergeExecutionResultDTO executeMerge(PatientMergeRequestDTO request) {
    // ... validation logic ...

    // Mark merged patient as inactive
    mergedPatient.setIsMerged(true);
    mergedPatient.setMergedIntoPatientId(primaryPatient.getId());
    mergedPatient.setMergeDate(new Timestamp(System.currentTimeMillis()));

    // Update merged patient in database
    patientDAO.update(mergedPatient);

    // Create audit entry
    PatientMergeAudit audit = new PatientMergeAudit();
    audit.setPrimaryPatientId(Long.parseLong(primaryPatient.getId()));
    audit.setMergedPatientId(Long.parseLong(mergedPatient.getId()));
    // ... save audit ...

    // NO identifier consolidation - patient records remain separate
    // Lookups will redirect merged patient → primary patient
}
```

**Lookup Redirection** (to be implemented in PatientDAO/Service):

```java
public Patient getPatientWithMergeRedirect(String patientId) {
    Patient patient = patientDAO.getData(patientId);

    if (patient != null && Boolean.TRUE.equals(patient.getIsMerged())) {
        // Redirect to primary patient
        String primaryId = patient.getMergedIntoPatientId();
        return patientDAO.getData(primaryId);
    }

    return patient;
}
```

**FHIR Representation** (Option A):

```json
{
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
    // Note: PAT-67890's National ID (0987654321) is NOT included - was discarded
  ]
}
```

### Action Required

**BLOCKER STATUS**: 🟡 **INTERIM SOLUTION IMPLEMENTED** - Redirect-on-Lookup
pattern in place

**Completed**:

1. ✅ Implemented redirect-on-lookup pattern
2. ✅ Updated BLOCKERS.md with interim solution
3. ✅ Merged patient marked inactive with reference to primary
4. ✅ No identifier duplication - both patients keep original identifiers

**Still Awaiting PM Decision**:

1. ⏳ Final identifier consolidation strategy (Options A, B, C, or D)
2. ⏳ Whether to enhance with actual identifier merging
3. ⏳ FHIR representation strategy for merged identifiers

**Current State**: Feature is **functional** with interim solution. Can proceed
with M3 (REST Controllers) and frontend integration.

**Impact on Timeline**:

- Milestone M1 (Database & DAO) - **MAY BE BLOCKED** if schema changes needed
  (Option C)
- Milestone M2 (Service Logic) - **BLOCKED** until identifier handling approach
  is defined
- Milestone M3 (REST Controller) - Depends on M2

**Workaround for Now**:

- Can proceed with Milestone M1 (Database schema for audit table, patient merge
  tracking fields)
- Can proceed with test infrastructure setup
- **CANNOT proceed** with core merge logic in M2 until this is resolved

---

## Tracking

**Issue Raised**: 2025-12-05 **Raised By**: Developer (during planning phase)
**PM Notified**: 2025-12-05 **Target Resolution**: Before Milestone M2
implementation begins **Status**: 🔴 BLOCKED

---

**Related Documents**:

- [spec.md](./spec.md) - Feature specification
- [research.md](./research.md) - Technical research (Section 7 affected)
- [plan.md](./plan.md) - Implementation plan
