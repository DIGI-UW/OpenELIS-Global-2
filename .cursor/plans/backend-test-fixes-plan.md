# Backend Test Failures Remediation Plan

## Summary

This plan addresses 5 backend test failures identified in CI and replicated
locally.

## Test Failures Identified

### 1. HibernateMappingValidationTest.buildSessionFactory

**Error**:
`@OneToOne or @ManyToOne on org.openelisglobal.storage.valueholder.SampleStorageAssignment.sampleItem references an unknown entity: org.openelisglobal.sampleitem.valueholder.SampleItem`

**Root Cause**: The test configuration doesn't include the SampleItem entity
mapping. SampleItem uses XML mapping (`SampleItem.hbm.xml`), not annotations.

**Fix**: Add `SampleItem.hbm.xml` resource to the Hibernate configuration in the
test.

### 2. LabelManagementRestControllerTest.testPutShortCodeEndpoint_LocationNotFound_Returns404

**Error**: Expected 404 but got 200

**Root Cause**: The `getLocationById` method may be returning a default/fallback
value instead of null when location doesn't exist, OR the exception handling is
catching the error and returning 200.

**Fix**: Verify `getLocationById` returns null for non-existent IDs, and ensure
the controller properly handles null return values.

### 3. BarcodeValidationServiceTest.testUnknownBarcodeType

**Error**: Expected 'unknown' but got 'sample'

**Root Cause**: The `detectBarcodeType` method is incorrectly identifying the
test barcode as a 'sample' type instead of 'unknown'.

**Fix**: Review `detectBarcodeType` logic to ensure it correctly identifies
unknown barcodes vs sample barcodes.

### 4. BarcodeValidationServiceTest.testErrorMessageFormatWhenParsingFails

**Error**: Error message should include format error

**Root Cause**: The error message format when parsing fails may not be including
the expected format error text.

**Fix**: Review `formatErrorMessage` method to ensure it includes the parsing
error message when parsing fails.

### 5. StorageDashboardServiceImplTest.testGetRacks_IncludesRoomColumn

**Error**: Rack should have roomId key

**Root Cause**: The test expects `roomId` key, but the implementation uses
`parentRoomId` (as seen in line 290 of StorageDashboardServiceImpl.java).

**Fix**: Either update the test to expect `parentRoomId` (consistent with other
parent-prefixed keys) OR update the implementation to also include `roomId` for
backward compatibility.

## Implementation Plan

### Phase 1: Fix HibernateMappingValidationTest (Critical - Blocks Other Tests)

1. Update `HibernateMappingValidationTest.buildSessionFactory()` to include
   `SampleItem.hbm.xml`
2. Verify test passes locally
3. Run full test suite to ensure no regressions

### Phase 2: Fix LabelManagementRestControllerTest

1. Review `getLocationById` method in `LabelManagementRestController`
2. Add debug logging or verify it returns null for non-existent IDs
3. Check if any exception handling is swallowing errors
4. Update controller logic if needed to properly return 404
5. Verify test passes

### Phase 3: Fix BarcodeValidationServiceTest Issues

1. Review `detectBarcodeType` method in `BarcodeValidationServiceImpl`
2. Fix logic to correctly identify 'unknown' vs 'sample' barcodes
3. Review `formatErrorMessage` method
4. Ensure error message includes parsing error when parsing fails
5. Verify both tests pass

### Phase 4: Fix StorageDashboardServiceImplTest

1. Review test expectation vs implementation
2. Decision: Use `parentRoomId` (consistent) or add `roomId` (backward compat)
3. Update test or implementation accordingly
4. Verify test passes

### Phase 5: Verification

1. Run all affected tests locally
2. Run full test suite
3. Verify CI passes

## Files to Modify

1. `src/test/java/org/openelisglobal/storage/HibernateMappingValidationTest.java`
2. `src/main/java/org/openelisglobal/storage/controller/LabelManagementRestController.java`
   (if needed)
3. `src/main/java/org/openelisglobal/storage/service/BarcodeValidationServiceImpl.java`
4. `src/test/java/org/openelisglobal/storage/service/StorageDashboardServiceImplTest.java`
   OR
   `src/main/java/org/openelisglobal/storage/service/StorageDashboardServiceImpl.java`

## Testing Strategy

- Run each test individually after fix
- Run full test suite after all fixes
- Verify no regressions in related tests
