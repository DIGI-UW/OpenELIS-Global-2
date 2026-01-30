package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for TBSampleCreationService verifying: 1. Service is
 * properly wired and injectable 2. Invalid specimen types are properly rejected
 * 3. Row creation returns appropriate results
 *
 * Note: Tests that require AccessionNumberUtil are skipped because the static
 * initializer requires AccessionNumberValidatorFactory bean which is not
 * available during test context initialization. The accession number generation
 * is tested via the full integration tests where Spring context is fully
 * initialized.
 */
public class TBSampleCreationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TBSampleCreationService tbSampleCreationService;

    @Before
    public void setUp() throws Exception {
        // Use existing test data with type_of_sample entries
        executeDataSetWithStateManagement("testdata/typeofsample.xml");
    }

    @Test
    public void testServiceIsWiredCorrectly() {
        assertNotNull("TBSampleCreationService should be autowired", tbSampleCreationService);
    }

    @Test
    public void testCreateSamplesForRow_InvalidSpecimenType() {
        // Create a row with invalid specimen type
        TBManifestRow testRow = createTestRow(2, "InvalidSpecimenType", "PAT-001", 1);

        TBSampleCreationService.RowCreationResult result = tbSampleCreationService.createSamplesForRow(1, testRow, "1");

        assertFalse("Row with invalid specimen type should fail", result.success());
        assertTrue("Error message should mention specimen type",
                result.errorMessage().contains("Unknown specimen type"));
        assertTrue("No samples should be created", result.createdSamples().isEmpty());
    }

    @Test
    public void testCreateSamplesForRow_NullSpecimenType() {
        // Create a row with null specimen type
        TBManifestRow testRow = createTestRow(2, null, "PAT-001", 1);

        TBSampleCreationService.RowCreationResult result = tbSampleCreationService.createSamplesForRow(1, testRow, "1");

        assertFalse("Row with null specimen type should fail", result.success());
        assertTrue("Error message should indicate specimen type is required",
                result.errorMessage().contains("Specimen type is required"));
        assertTrue("No samples should be created", result.createdSamples().isEmpty());
    }

    @Test
    public void testCreateSamplesForRow_EmptySpecimenType() {
        // Create a row with empty specimen type
        TBManifestRow testRow = createTestRow(2, "", "PAT-001", 1);

        TBSampleCreationService.RowCreationResult result = tbSampleCreationService.createSamplesForRow(1, testRow, "1");

        assertFalse("Row with empty specimen type should fail", result.success());
        assertTrue("Error message should indicate specimen type is required",
                result.errorMessage().contains("Specimen type is required"));
        assertTrue("No samples should be created", result.createdSamples().isEmpty());
    }

    @Test
    public void testRowCreationResultRecord() {
        // Test the record structure works correctly
        TBSampleCreationService.RowCreationResult successResult = new TBSampleCreationService.RowCreationResult(true,
                java.util.List.of(), java.util.List.of("ACC-001"), null);

        assertTrue("Success flag should be true", successResult.success());
        assertNull("Error message should be null for success", successResult.errorMessage());
        assertEquals("Should have one accession number", 1, successResult.accessionNumbers().size());

        TBSampleCreationService.RowCreationResult failResult = new TBSampleCreationService.RowCreationResult(false,
                java.util.List.of(), java.util.List.of(), "Test error");

        assertFalse("Success flag should be false", failResult.success());
        assertEquals("Error message should match", "Test error", failResult.errorMessage());
        assertTrue("Accession numbers should be empty", failResult.accessionNumbers().isEmpty());
    }

    /**
     * Helper method to create a test TBManifestRow
     */
    private TBManifestRow createTestRow(int rowNumber, String specimenType, String patientId, int numSamples) {
        return new TBManifestRow(rowNumber, // rowNumber
                "TB-TEST-" + rowNumber, // sampleId
                specimenType, // specimenType
                "Good", // specimenQuality
                "DOC-2025-TEST", // documentNumber
                "Test Hospital", // referringFacility
                "Test Patient", // patientName
                "35", // patientAge
                "Male", // patientSex
                patientId, // patientId
                "STUDY-TEST", // studyId
                "123 Test Street", // patientAddress
                "+1234567890", // patientPhone
                "+0987654321", // physicianPhone
                "Consented", // consentStatus
                "New patient", // treatmentHistory
                "Yes", // culture
                "Yes", // smearMicroscopy
                "No", // genexpert
                "No", // identification
                "No", // dstFirstLine
                "No", // dstSecondLine
                "LJ", // intendedMethod
                "Central Lab", // receivedSite
                "2025-01-15", // receivedDate
                "08:30", // receivedTime
                numSamples // numOfSamples
        );
    }
}
