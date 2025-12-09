# TODO Markers for Identifier Duplication Blocker

**Blocker Reference**: [BLOCKERS.md](./BLOCKERS.md) - Blocker #1 **Status**:
Awaiting PM decision **Impact**: Milestone M2 (Service Logic) -
`consolidateIdentifiers()` method

---

## Where to Place TODOs

### 1. Service Layer - Identifier Consolidation

**File**:
`src/main/java/org/openelisglobal/patient/merge/service/PatientMergeServiceImpl.java`

**Location**: `consolidateIdentifiers()` method

```java
/**
 * Consolidates patient identifiers from merged patient to primary patient.
 *
 * TODO(BLOCKER-001): Implement identifier consolidation logic once PM clarifies approach
 *   - See: specs/008-patient-merge-backend/BLOCKERS.md
 *   - Options: A (Primary wins), B (User selects), C (Store both with flag), D (Block merge)
 *   - Current assumption: Option A (Primary patient wins, discard duplicate types)
 *
 * @param primary Primary patient (remains active)
 * @param merged Merged patient (will be marked inactive)
 * @param summary Data summary to update with identifier counts
 */
@Transactional
private void consolidateIdentifiers(Patient primary, Patient merged,
                                    PatientMergeDataSummaryDTO summary) {
    // TODO(BLOCKER-001): Replace placeholder with actual implementation

    // TEMPORARY PLACEHOLDER: Just log the issue for now
    log.warn("TODO(BLOCKER-001): Identifier consolidation not yet implemented. " +
             "Awaiting PM decision on handling duplicate identifier types.");

    // For now, do NOT update patient_identity foreign keys
    // This will be implemented once PM clarifies the approach

    summary.setTotalIdentities(0);
    summary.setPreservedIdentities(0);
    summary.setDiscardedIdentities(0);
    summary.setConflictingIdentityTypes(new ArrayList<>());
}
```

---

### 2. DAO Layer - Identifier Updates

**File**:
`src/main/java/org/openelisglobal/patientidentity/dao/PatientIdentityDAO.java`

**Location**: Bulk UPDATE method (may not be needed depending on PM decision)

```java
/**
 * Updates patient_id for all identities from merged patient to primary patient.
 *
 * TODO(BLOCKER-001): This method may need modification based on PM decision
 *   - Option A: Use as-is (simple UPDATE)
 *   - Option C: Add is_active flag logic
 *   - Option D: May not be needed (block merge if duplicates exist)
 */
@Modifying
@Query("UPDATE PatientIdentity pi SET pi.patientId = :primaryPatientId " +
       "WHERE pi.patientId = :mergedPatientId AND pi.identityTypeId = :identityTypeId")
int updatePatientIdForIdentityType(@Param("primaryPatientId") String primaryPatientId,
                                   @Param("mergedPatientId") String mergedPatientId,
                                   @Param("identityTypeId") String identityTypeId);
```

---

### 3. Validation Logic - Duplicate Identifier Check

**File**:
`src/main/java/org/openelisglobal/patient/merge/service/PatientMergeServiceImpl.java`

**Location**: `validateMerge()` method

```java
/**
 * Validates whether two patients can be merged.
 */
public PatientMergeValidationResultDTO validateMerge(String patient1Id, String patient2Id) {
    PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();

    // ... other validation checks ...

    // TODO(BLOCKER-001): Add duplicate identifier type validation once PM clarifies approach
    // If Option D selected: Fail validation if duplicate identifier types exist
    // If Option A/B/C selected: Add warnings but allow merge

    // TEMPORARY: Just add a warning for now
    List<String> duplicateTypes = checkForDuplicateIdentifierTypes(patient1, patient2);
    if (!duplicateTypes.isEmpty()) {
        result.addWarning("TODO(BLOCKER-001): Duplicate identifier types detected: " +
                         String.join(", ", duplicateTypes) +
                         ". Handling not yet implemented - awaiting PM decision.");
    }

    return result;
}
```

---

### 4. Integration Tests - Identifier Consolidation

**File**:
`src/test/java/org/openelisglobal/patient/merge/service/PatientMergeServiceIntegrationTest.java`

**Location**: Test for identifier consolidation

```java
@Test
@Ignore("TODO(BLOCKER-001): Enable this test once PM clarifies identifier handling approach")
public void executeMerge_whenDuplicateIdentifierTypes_shouldHandleCorrectly() {
    // Arrange
    Patient patient1 = createPatientWithNationalId("123", "NAT-001");
    Patient patient2 = createPatientWithNationalId("456", "NAT-002");

    PatientMergeRequestDTO request = new PatientMergeRequestDTO();
    request.setPatient1Id("123");
    request.setPatient2Id("456");
    request.setPrimaryPatientId("123");
    request.setReason("Test duplicate identifier handling");
    request.setConfirmed(true);

    // Act
    PatientMergeAudit audit = service.executeMerge(request);

    // Assert
    // TODO(BLOCKER-001): Add assertions based on PM decision
    // - Option A: Only NAT-001 remains
    // - Option B: UI allows selection (backend receives choice)
    // - Option C: Both remain, one marked inactive
    // - Option D: Merge should have failed in validation
}
```

---

### 5. Data Model Documentation

**File**: `specs/008-patient-merge-backend/data-model.md`

**Location**: Section 7.1 - Already marked

✅ Already documented with status: **⏸️ Implementation blocked until PM
clarifies approach**

---

## TODO Format Convention

Use this consistent format for all TODOs:

```java
// TODO(BLOCKER-001): Brief description
//   - Reference: specs/008-patient-merge-backend/BLOCKERS.md
//   - Context: Additional context if needed
//   - Decision needed: What specifically needs clarification
```

**Why this format?**

- `TODO(BLOCKER-001)`: Searchable, links to specific blocker
- Multi-line comment: Provides full context for future developer
- Reference to BLOCKERS.md: Clear documentation trail

---

## Searching for TODOs Later

When PM provides clarification:

```bash
# Find all blocker-related TODOs
grep -r "TODO(BLOCKER-001)" src/

# Or using IDE
# IntelliJ: Ctrl+Shift+F → Search "TODO(BLOCKER-001)"
# VSCode: Ctrl+Shift+F → Search "TODO(BLOCKER-001)"
```

---

## Implementation Priority (With TODOs)

### ✅ Can Implement Now (No Blockers)

- **Milestone M1**: Database schema, entities, DAOs (100% complete)
- **Milestone M2**:
  - Service validation logic (except duplicate identifier check)
  - Permission enforcement
  - Transaction management
  - FHIR Patient link creation
  - Audit trail creation
  - Related table updates (patient_contact, sample_human, etc.)
- **Milestone M3**: REST controllers (100% complete)

### ⏸️ Implement with TODOs (Affected by Blocker)

- **Milestone M2**:
  - `consolidateIdentifiers()` method → **Add TODO(BLOCKER-001)**
  - Duplicate identifier validation → **Add TODO(BLOCKER-001)**
  - Identifier consolidation tests → **@Ignore with TODO(BLOCKER-001)**

---

## When PM Clarifies

1. **Search for TODOs**: `grep -r "TODO(BLOCKER-001)" src/`
2. **Update BLOCKERS.md**: Document the decision
3. **Implement solution**: Replace TODOs with actual implementation
4. **Enable tests**: Remove `@Ignore` from identifier tests
5. **Update documentation**: Update data-model.md with final approach

---

**Status**: Ready to proceed with implementation **Blockers**: 1 blocker
documented with TODOs **Coverage**: ~90% of functionality can be implemented now

---

**Last Updated**: 2025-12-08 **Next Review**: When PM provides decision on
Blocker #1
