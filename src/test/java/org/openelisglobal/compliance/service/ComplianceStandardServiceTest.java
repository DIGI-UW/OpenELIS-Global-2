package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TDD Test Suite for ComplianceStandardService
 *
 * Tests follow constitutional requirements: - Test-Driven Development
 * (RED-GREEN-REFACTOR) - Inversion Test principle (V.6) - tests must fail if
 * implementation is replaced with hardcoded return - Service layer transaction
 * boundaries validation - FHIR UUID integration testing
 */
public class ComplianceStandardServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    private ComplianceStandard testStandard;

    @Before
    public void setUp() throws Exception {
        // Load test data following OpenELIS pattern
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");

        // Create test standard for individual tests
        testStandard = createTestStandard();
    }

    // RED PHASE: Tests that will fail initially

    @Test
    public void testGetAll_shouldReturnAllComplianceStandards() {
        List<ComplianceStandard> standards = complianceStandardService.getAll();

        assertNotNull("Standards list should not be null", standards);
        assertFalse("Standards list should not be empty", standards.isEmpty());

        assertTrue("Should have at least 3 test standards", standards.size() >= 3);

        // Verify FHIR UUID is present (constitutional requirement)
        for (ComplianceStandard standard : standards) {
            assertNotNull("FHIR UUID should not be null", standard.getFhirUuid());
        }
    }

    @Test
    public void testSaveComplianceStandard_shouldCreateNewStandard() {
        ComplianceStandard newStandard = createValidStandard("Test Standard", "Test Authority", "TS-2026-001", "1.0");

        ComplianceStandard savedStandard = complianceStandardService.save(newStandard);
        String savedId = savedStandard.getId();

        assertNotNull("Saved ID should not be null", savedId);
        assertEquals("Standard ID should match", newStandard.getId(), savedId);

        assertNotNull("FHIR UUID should be auto-generated", newStandard.getFhirUuid());

        // Verify audit fields are set (constitutional requirement)
        assertNotNull("Created date should be set", newStandard.getLastupdated());
        assertNotNull("Created user should be set", newStandard.getSysUserId());
    }

    @Test
    public void testUpdateStandard_shouldUpdateExistingStandard() {
        String standardId = "30001";
        ComplianceStandard standard = complianceStandardService.get(standardId);

        String originalName = standard.getName();
        String newName = "Updated Test Standard";
        standard.setName(newName);

        complianceStandardService.update(standard);

        ComplianceStandard updatedStandard = complianceStandardService.get(standardId);
        assertEquals("Name should be updated", newName, updatedStandard.getName());
        assertNotNull("Update timestamp should be set", updatedStandard.getLastupdated());

        assertFalse("Updated name should differ from original", originalName.equals(newName));
    }

    @Test
    public void testArchiveStandard_shouldPreventDeletionWithThresholds() {
        String standardId = "30001"; // Has linked parameter group + threshold in test data

        try {
            ComplianceStandard standardToDelete = complianceStandardService.get(standardId);
            complianceStandardService.delete(standardToDelete);
            fail("Should not be able to delete standard with linked thresholds");
        } catch (Exception e) {
            assertTrue("Should throw appropriate exception", e.getMessage().contains("has linked thresholds"));
        }

        // Archive should work instead
        complianceStandardService.archive(standardId);

        ComplianceStandard archivedStandard = complianceStandardService.get(standardId);
        assertEquals("Standard should be archived", ComplianceStandardStatus.ARCHIVED, archivedStandard.getStatus());
    }

    @Test
    public void testPreSeededStandardProtection_shouldPreventDeletionOfPreSeededRecords() {
        ComplianceStandard preSeededStandard = createPreSeededStandard();
        complianceStandardService.save(preSeededStandard);

        try {
            complianceStandardService.delete(preSeededStandard);
            fail("Should not be able to delete pre-seeded standard");
        } catch (Exception e) {
            assertTrue("Should throw pre-seeded protection exception",
                    e.getMessage().contains("Pre-seeded standards cannot be deleted"));
        }

        // Archive should still work
        complianceStandardService.archive(preSeededStandard.getId());
        ComplianceStandard archived = complianceStandardService.get(preSeededStandard.getId());
        assertEquals("Pre-seeded standard should be archivable", ComplianceStandardStatus.ARCHIVED,
                archived.getStatus());
    }

    @Test
    public void testUniquenessConstraint_shouldPreventDuplicateStandards() {
        ComplianceStandard duplicate = createValidStandard("PP No. 22/2021 - Water Quality", // Same as existing
                                                                                             // standard
                "Government of Indonesia", "PP 22/2021", "2021");

        try {
            complianceStandardService.save(duplicate);
            fail("Should not allow duplicate standard");
        } catch (Exception e) {
            assertTrue("Should throw uniqueness constraint violation", e.getMessage().contains("already exists"));
        }
    }

    // Helper Methods

    private ComplianceStandard createTestStandard() {
        return createValidStandard("Test Water Quality Standard", "Test Authority", "TEST-001", "1.0");
    }

    private ComplianceStandard createValidStandard(String name, String issuingBody, String regulationNumber,
            String version) {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setName(name);
        standard.setIssuingBody(issuingBody);
        standard.setRegulationNumber(regulationNumber);
        standard.setVersion(version);
        standard.setEffectiveDate(LocalDate.now());
        standard.setCountryRegion("Indonesia");
        standard.setApplicableSampleTypes("Water");
        standard.setStatus(ComplianceStandardStatus.DRAFT);
        standard.setIsPreSeeded(false);
        standard.setSysUserId("1");

        return standard;
    }

    private ComplianceStandard createPreSeededStandard() {
        ComplianceStandard preSeeded = createValidStandard("Pre-seeded Standard", "System", "PRE-001", "1.0");
        preSeeded.setIsPreSeeded(true);
        return preSeeded;
    }
}