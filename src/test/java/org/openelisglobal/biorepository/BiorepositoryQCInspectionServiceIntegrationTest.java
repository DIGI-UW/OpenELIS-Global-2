package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.BiorepositoryQCInspectionService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.DiscrepancyType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for BiorepositoryQCInspectionService.
 *
 * Tests verify QC inspection workflow for stored biorepository samples: -
 * Creating individual and bulk inspections - Querying inspections by various
 * criteria - QC result calculation based on checklist - Discrepancy tracking
 */
public class BiorepositoryQCInspectionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BiorepositoryQCInspectionService qcInspectionService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser testUser;
    private TypeOfSample serumType;

    @Before
    public void setUp() {
        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_qc_user");
            testUser.setFirstName("Test");
            testUser.setLastName("QC User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        // Get serum sample type
        List<TypeOfSample> allTypes = typeOfSampleService.getAll();
        serumType = allTypes.stream().filter(t -> "Serum".equalsIgnoreCase(t.getDescription())).findFirst()
                .orElse(allTypes.get(0));
    }

    // ========== CREATE SINGLE INSPECTION TESTS ==========

    @Test
    public void testCreateInspection_AllChecksPassed_ResultVerified() {
        // Arrange
        BioSample bioSample = createTestBioSample("QC-PASS-" + System.currentTimeMillis());
        Timestamp inspectionDate = new Timestamp(System.currentTimeMillis());

        // Act - All checks passed
        BiorepositoryQCInspection inspection = qcInspectionService.createInspection(bioSample.getId(), "John Inspector",
                inspectionDate, true, // samplePresent
                true, // labelIntegrity
                true, // containerIntegrity
                true, // volumeAppearanceAcceptable
                true, // correctPosition
                null, // discrepancyType (none)
                null, // correctiveAction (none)
                "All checks passed", // remarks
                testUser.getId().toString());

        // Assert
        assertNotNull("Inspection ID should be generated", inspection.getId());
        assertEquals("BioSample should match", bioSample.getId(), inspection.getBioSample().getId());
        assertEquals("Inspector name should match", "John Inspector", inspection.getInspectorName());
        assertEquals("QC result should be VERIFIED", QCResult.VERIFIED, inspection.getQcResult());
        assertNull("Discrepancy type should be null for passed QC", inspection.getDiscrepancyType());
        assertEquals("All 5 checks should pass", 5, inspection.getPassedCheckCount());
        assertTrue("isAllChecksPassed should return true", inspection.isAllChecksPassed());

        // CRITICAL: Verify individual checklist items are actually stored (prevent
        // false positive)
        assertTrue("samplePresent should be true", inspection.isSamplePresent());
        assertTrue("labelIntegrity should be true", inspection.isLabelIntegrity());
        assertTrue("containerIntegrity should be true", inspection.isContainerIntegrity());
        assertTrue("volumeAppearanceAcceptable should be true", inspection.isVolumeAppearanceAcceptable());
        assertTrue("correctPosition should be true", inspection.isCorrectPosition());
    }

    @Test
    public void testCreateInspection_OneCheckFailed_ResultDiscrepancy() {
        // Arrange
        BioSample bioSample = createTestBioSample("QC-FAIL-" + System.currentTimeMillis());
        Timestamp inspectionDate = new Timestamp(System.currentTimeMillis());

        // Act - One check failed (samplePresent = false)
        BiorepositoryQCInspection inspection = qcInspectionService.createInspection(bioSample.getId(), "Jane Inspector",
                inspectionDate, false, // samplePresent = FALSE
                true, // labelIntegrity
                true, // containerIntegrity
                true, // volumeAppearanceAcceptable
                true, // correctPosition
                "MISSING_SAMPLE", // discrepancyType
                "Initiate sample location investigation", // correctiveAction
                "Sample not found at recorded position", // remarks
                testUser.getId().toString());

        // Assert
        assertNotNull("Inspection ID should be generated", inspection.getId());
        assertEquals("QC result should be DISCREPANCY_FOUND", QCResult.DISCREPANCY_FOUND, inspection.getQcResult());
        assertEquals("Discrepancy type should be MISSING_SAMPLE", DiscrepancyType.MISSING_SAMPLE,
                inspection.getDiscrepancyType());
        assertEquals("Corrective action should be set", "Initiate sample location investigation",
                inspection.getCorrectiveAction());
        assertEquals("4 checks should pass", 4, inspection.getPassedCheckCount());
        assertFalse("isAllChecksPassed should return false", inspection.isAllChecksPassed());

        // CRITICAL: Verify individual checklist items match input values (prevent false
        // positive)
        assertFalse("samplePresent should be FALSE", inspection.isSamplePresent());
        assertTrue("labelIntegrity should be true", inspection.isLabelIntegrity());
        assertTrue("containerIntegrity should be true", inspection.isContainerIntegrity());
        assertTrue("volumeAppearanceAcceptable should be true", inspection.isVolumeAppearanceAcceptable());
        assertTrue("correctPosition should be true", inspection.isCorrectPosition());
    }

    @Test
    public void testCreateInspection_MultipleChecksFailed_ResultDiscrepancy() {
        // Arrange
        BioSample bioSample = createTestBioSample("QC-MULTI-FAIL-" + System.currentTimeMillis());
        Timestamp inspectionDate = new Timestamp(System.currentTimeMillis());

        // Act - Multiple checks failed
        BiorepositoryQCInspection inspection = qcInspectionService.createInspection(bioSample.getId(),
                "Inspector Smith", inspectionDate, true, // samplePresent
                false, // labelIntegrity = FALSE
                false, // containerIntegrity = FALSE
                true, // volumeAppearanceAcceptable
                true, // correctPosition
                "CONTAINER_DAMAGE", // discrepancyType
                "Replace container and re-label", // correctiveAction
                "Container cracked, label damaged", // remarks
                testUser.getId().toString());

        // Assert
        assertEquals("QC result should be DISCREPANCY_FOUND", QCResult.DISCREPANCY_FOUND, inspection.getQcResult());
        assertEquals("3 checks should pass", 3, inspection.getPassedCheckCount());
        assertEquals("Discrepancy type should be CONTAINER_DAMAGE", DiscrepancyType.CONTAINER_DAMAGE,
                inspection.getDiscrepancyType());
    }

    @Test(expected = Exception.class)
    public void testCreateInspection_NonExistentBioSample_ThrowsException() {
        // Act - should throw (ObjectNotFoundException or IllegalArgumentException)
        qcInspectionService.createInspection(999999, // non-existent ID
                "Inspector", new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null, null,
                testUser.getId().toString());
    }

    // ========== BULK CREATE INSPECTIONS TESTS ==========

    @Test
    public void testCreateBulkInspections_Success() {
        // Arrange - Create 3 test biosamples
        BioSample sample1 = createTestBioSample("BULK-1-" + System.currentTimeMillis());
        BioSample sample2 = createTestBioSample("BULK-2-" + System.currentTimeMillis());
        BioSample sample3 = createTestBioSample("BULK-3-" + System.currentTimeMillis());

        List<Integer> bioSampleIds = Arrays.asList(sample1.getId(), sample2.getId(), sample3.getId());
        Timestamp inspectionDate = new Timestamp(System.currentTimeMillis());

        // Act - Bulk create with all checks passed
        List<BiorepositoryQCInspection> inspections = qcInspectionService.createBulkInspections(bioSampleIds,
                "Bulk Inspector", inspectionDate, true, true, true, true, true, null, null, "Bulk inspection test",
                testUser.getId().toString());

        // Assert
        assertEquals("Should create 3 inspections", 3, inspections.size());

        for (BiorepositoryQCInspection inspection : inspections) {
            assertNotNull("Inspection should have ID", inspection.getId());
            assertEquals("Inspector name should match", "Bulk Inspector", inspection.getInspectorName());
            assertEquals("QC result should be VERIFIED", QCResult.VERIFIED, inspection.getQcResult());
            assertTrue("All checks should pass", inspection.isAllChecksPassed());
        }
    }

    // ========== QUERY TESTS ==========

    @Test
    public void testGetByBioSampleId_Found() {
        // Arrange - Create inspection
        BioSample bioSample = createTestBioSample("GET-BY-SAMPLE-" + System.currentTimeMillis());
        BiorepositoryQCInspection created = qcInspectionService.createInspection(bioSample.getId(), "Inspector",
                new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null, null,
                testUser.getId().toString());

        // Act
        List<BiorepositoryQCInspection> found = qcInspectionService.getByBioSampleId(bioSample.getId());

        // Assert
        assertFalse("Should find inspections", found.isEmpty());
        assertEquals("Should find 1 inspection", 1, found.size());
        assertEquals("Inspection ID should match", created.getId(), found.get(0).getId());
    }

    @Test
    public void testGetByBioSampleId_MultipleInspections_OrderedByDateDesc() {
        // Arrange - Create multiple inspections for same sample at different times
        BioSample bioSample = createTestBioSample("MULTI-INSPECT-" + System.currentTimeMillis());

        Timestamp firstTime = new Timestamp(System.currentTimeMillis() - 10000);
        Timestamp secondTime = new Timestamp(System.currentTimeMillis());

        BiorepositoryQCInspection first = qcInspectionService.createInspection(bioSample.getId(), "Inspector 1",
                firstTime, true, true, true, true, true, null, null, "First inspection", testUser.getId().toString());

        BiorepositoryQCInspection second = qcInspectionService.createInspection(bioSample.getId(), "Inspector 2",
                secondTime, true, false, true, true, true, "DAMAGED_LABEL", "Re-label", "Second inspection",
                testUser.getId().toString());

        // Act
        List<BiorepositoryQCInspection> found = qcInspectionService.getByBioSampleId(bioSample.getId());

        // Assert
        assertEquals("Should find 2 inspections", 2, found.size());
        // Should be ordered by date descending (most recent first)
        assertEquals("First result should be most recent", second.getId(), found.get(0).getId());
        assertEquals("Second result should be older", first.getId(), found.get(1).getId());
    }

    @Test
    public void testGetMostRecentByBioSampleId_Found() {
        // Arrange
        BioSample bioSample = createTestBioSample("RECENT-" + System.currentTimeMillis());

        // Create older inspection
        qcInspectionService.createInspection(bioSample.getId(), "Inspector 1",
                new Timestamp(System.currentTimeMillis() - 10000), true, true, true, true, true, null, null,
                "Older inspection", testUser.getId().toString());

        // Create newer inspection
        BiorepositoryQCInspection newer = qcInspectionService.createInspection(bioSample.getId(), "Inspector 2",
                new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null, "Newer inspection",
                testUser.getId().toString());

        // Act
        BiorepositoryQCInspection found = qcInspectionService.getMostRecentByBioSampleId(bioSample.getId());

        // Assert
        assertNotNull("Should find most recent inspection", found);
        assertEquals("Should return newer inspection", newer.getId(), found.getId());
    }

    @Test
    public void testGetMostRecentByBioSampleId_NotFound() {
        // Act
        BiorepositoryQCInspection found = qcInspectionService.getMostRecentByBioSampleId(999999);

        // Assert
        assertNull("Should return null for non-existent biosample", found);
    }

    @Test
    public void testGetByQCResult_FiltersByResult() {
        // Arrange - Create samples with different QC results
        BioSample passedSample = createTestBioSample("FILTER-PASS-" + System.currentTimeMillis());
        BioSample failedSample = createTestBioSample("FILTER-FAIL-" + System.currentTimeMillis());

        BiorepositoryQCInspection passedInspection = qcInspectionService.createInspection(passedSample.getId(),
                "Inspector", new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null,
                "Passed", testUser.getId().toString());

        BiorepositoryQCInspection failedInspection = qcInspectionService.createInspection(failedSample.getId(),
                "Inspector", new Timestamp(System.currentTimeMillis()), false, true, true, true, true, "MISSING_SAMPLE",
                "Investigate", "Failed", testUser.getId().toString());

        // Act
        List<BiorepositoryQCInspection> verified = qcInspectionService.getByQCResult(QCResult.VERIFIED);
        List<BiorepositoryQCInspection> discrepancies = qcInspectionService.getByQCResult(QCResult.DISCREPANCY_FOUND);

        // Assert - Verify correct filtering (not just presence, but also absence in
        // wrong list)
        // 1. Passed sample SHOULD be in verified list
        assertTrue("Passed sample SHOULD be in verified list",
                verified.stream().anyMatch(i -> i.getId().equals(passedInspection.getId())));

        // 2. Passed sample should NOT be in discrepancies list (CRITICAL: prevents
        // false positive)
        assertFalse("Passed sample should NOT be in discrepancies list",
                discrepancies.stream().anyMatch(i -> i.getId().equals(passedInspection.getId())));

        // 3. Failed sample SHOULD be in discrepancies list
        assertTrue("Failed sample SHOULD be in discrepancies list",
                discrepancies.stream().anyMatch(i -> i.getId().equals(failedInspection.getId())));

        // 4. Failed sample should NOT be in verified list (CRITICAL: prevents false
        // positive)
        assertFalse("Failed sample should NOT be in verified list",
                verified.stream().anyMatch(i -> i.getId().equals(failedInspection.getId())));

        // 5. Verify ALL records in verified list have VERIFIED status
        assertTrue("All records in verified list must have VERIFIED status",
                verified.stream().allMatch(i -> QCResult.VERIFIED.equals(i.getQcResult())));

        // 6. Verify ALL records in discrepancies list have DISCREPANCY_FOUND status
        assertTrue("All records in discrepancies list must have DISCREPANCY_FOUND status",
                discrepancies.stream().allMatch(i -> QCResult.DISCREPANCY_FOUND.equals(i.getQcResult())));
    }

    @Test
    public void testGetByInspectorName_Found() {
        // Arrange
        String inspectorName = "Jane Doe " + System.currentTimeMillis();
        BioSample bioSample = createTestBioSample("INSPECTOR-" + System.currentTimeMillis());

        BiorepositoryQCInspection created = qcInspectionService.createInspection(bioSample.getId(), inspectorName,
                new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null, null,
                testUser.getId().toString());

        // Act
        List<BiorepositoryQCInspection> found = qcInspectionService.getByInspectorName(inspectorName);

        // Assert
        assertFalse("Should find inspections", found.isEmpty());
        assertTrue("Should include created inspection",
                found.stream().anyMatch(i -> i.getId().equals(created.getId())));
    }

    @Test
    public void testExistsByBioSampleId() {
        // Arrange
        BioSample bioSample = createTestBioSample("EXISTS-" + System.currentTimeMillis());

        // Assert - before creating inspection
        assertFalse("Should not exist before creation", qcInspectionService.existsByBioSampleId(bioSample.getId()));

        // Create inspection
        qcInspectionService.createInspection(bioSample.getId(), "Inspector", new Timestamp(System.currentTimeMillis()),
                true, true, true, true, true, null, null, null, testUser.getId().toString());

        // Assert - after creating inspection
        assertTrue("Should exist after creation", qcInspectionService.existsByBioSampleId(bioSample.getId()));
    }

    @Test
    public void testCountByQCResult() {
        // Arrange - Create samples with known results
        String uniqueTag = "COUNT-" + System.currentTimeMillis();
        BioSample pass1 = createTestBioSample(uniqueTag + "-P1");
        BioSample pass2 = createTestBioSample(uniqueTag + "-P2");
        BioSample fail1 = createTestBioSample(uniqueTag + "-F1");

        long initialVerifiedCount = qcInspectionService.countByQCResult(QCResult.VERIFIED);
        long initialDiscrepancyCount = qcInspectionService.countByQCResult(QCResult.DISCREPANCY_FOUND);

        // Create inspections
        qcInspectionService.createInspection(pass1.getId(), "Inspector", new Timestamp(System.currentTimeMillis()),
                true, true, true, true, true, null, null, null, testUser.getId().toString());

        qcInspectionService.createInspection(pass2.getId(), "Inspector", new Timestamp(System.currentTimeMillis()),
                true, true, true, true, true, null, null, null, testUser.getId().toString());

        qcInspectionService.createInspection(fail1.getId(), "Inspector", new Timestamp(System.currentTimeMillis()),
                false, true, true, true, true, "MISSING_SAMPLE", "Investigate", null, testUser.getId().toString());

        // Act
        long finalVerifiedCount = qcInspectionService.countByQCResult(QCResult.VERIFIED);
        long finalDiscrepancyCount = qcInspectionService.countByQCResult(QCResult.DISCREPANCY_FOUND);

        // Assert
        assertEquals("Verified count should increase by 2", initialVerifiedCount + 2, finalVerifiedCount);
        assertEquals("Discrepancy count should increase by 1", initialDiscrepancyCount + 1, finalDiscrepancyCount);
    }

    // ========== QC RESULT CALCULATION TESTS ==========

    @Test
    public void testQCResultCalculation_AllTrue_ReturnsVerified() {
        // Arrange
        BioSample bioSample = createTestBioSample("CALC-VERIFY-" + System.currentTimeMillis());

        // Act
        BiorepositoryQCInspection inspection = qcInspectionService.createInspection(bioSample.getId(), "Inspector",
                new Timestamp(System.currentTimeMillis()), true, true, true, true, true, null, null, null,
                testUser.getId().toString());

        // Assert
        assertEquals("QC result should be VERIFIED when all checks pass", QCResult.VERIFIED, inspection.getQcResult());
    }

    @Test
    public void testQCResultCalculation_OneFalse_ReturnsDiscrepancy() {
        // Arrange
        BioSample bioSample = createTestBioSample("CALC-DISC-" + System.currentTimeMillis());

        // Act - Only one check fails
        BiorepositoryQCInspection inspection = qcInspectionService.createInspection(bioSample.getId(), "Inspector",
                new Timestamp(System.currentTimeMillis()), true, true, true, false, // volumeAppearanceAcceptable =
                                                                                    // FALSE
                true, "VOLUME_DISCREPANCY", "Investigate volume loss", null, testUser.getId().toString());

        // Assert
        assertEquals("QC result should be DISCREPANCY_FOUND when any check fails", QCResult.DISCREPANCY_FOUND,
                inspection.getQcResult());
    }

    // ========== DISCREPANCY TYPE TESTS ==========

    @Test
    public void testDiscrepancyType_AllEnumValues_ParseCorrectly() {
        // Test that all enum values can be parsed
        for (DiscrepancyType type : DiscrepancyType.values()) {
            DiscrepancyType parsed = DiscrepancyType.fromString(type.name());
            assertEquals("Should parse " + type.name() + " correctly", type, parsed);
        }
    }

    @Test
    public void testDiscrepancyType_InvalidString_ReturnsNull() {
        // Act
        DiscrepancyType parsed = DiscrepancyType.fromString("INVALID_TYPE");

        // Assert
        assertNull("Should return null for invalid discrepancy type", parsed);
    }

    @Test
    public void testDiscrepancyType_NullString_ReturnsNull() {
        // Act
        DiscrepancyType parsed = DiscrepancyType.fromString(null);

        // Assert
        assertNull("Should return null for null input", parsed);
    }

    // ========== HELPER METHODS ==========

    /**
     * Create a test BioSample with STORED workflow status.
     */
    private BioSample createTestBioSample(String externalId) {
        // Create Sample
        Sample sample = new Sample();
        // Use shorter accession number to fit VARCHAR(20) constraint
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000); // Last 8 digits
        sample.setAccessionNumber("QC" + timestamp); // e.g., "QC10150114" = 10 chars
        sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
        sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        sample.setDomain("H");
        sample.setSysUserId(testUser.getId().toString());
        sample = sampleService.save(sample);

        // Create SampleItem
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setTypeOfSample(serumType);
        sampleItem.setStatusId("1"); // ACTIVE status
        sampleItem.setSortOrder("1");
        sampleItem.setExternalId(externalId);
        sampleItem.setCollectionDate(new Timestamp(System.currentTimeMillis()));
        sampleItem.setSysUserId(testUser.getId().toString());
        sampleItem = sampleItemService.save(sampleItem);

        // Create BioSample with STORED status
        BioSample bioSample = new BioSample();
        bioSample.setSampleItem(sampleItem);
        bioSample.setBiosafetyLevel(BiosafetyLevel.BSL_1);
        bioSample.setWorkflowStatus(WorkflowStatus.STORED); // IMPORTANT: Set to STORED for QC inspection
        bioSample.setPreservationMedium("EDTA");
        bioSample.setSysUserId(testUser.getId().toString());

        return bioSampleService.save(bioSample);
    }
}
