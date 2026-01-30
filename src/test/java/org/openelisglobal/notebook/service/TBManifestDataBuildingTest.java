package org.openelisglobal.notebook.service;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow;

/**
 * Unit tests for TB manifest data building and transformation logic. These
 * tests don't require Spring context and verify the data structures.
 */
public class TBManifestDataBuildingTest {

    @Test
    public void testTBManifestRowRecordStructure() {
        // Verify the TBManifestRow record contains all expected fields
        TBManifestRow row = createFullManifestRow();

        assertEquals("Row number should match", 1, row.rowNumber());
        assertEquals("Sample ID should match", "TB-001", row.sampleId());
        assertEquals("Specimen type should match", "Sputum", row.specimenType());
        assertEquals("Specimen quality should match", "Good", row.specimenQuality());
        assertEquals("Document number should match", "DOC-2025-001", row.documentNumber());
        assertEquals("Referring facility should match", "District Hospital", row.referringFacility());
        assertEquals("Patient name should match", "John Doe", row.patientName());
        assertEquals("Patient age should match", "45", row.patientAge());
        assertEquals("Patient sex should match", "Male", row.patientSex());
        assertEquals("Patient ID should match", "PAT-001", row.patientId());
        assertEquals("Study ID should match", "STUDY-001", row.studyId());
        assertEquals("Patient address should match", "123 Main St", row.patientAddress());
        assertEquals("Patient phone should match", "+1234567890", row.patientPhone());
        assertEquals("Physician phone should match", "+0987654321", row.physicianPhone());
        assertEquals("Consent status should match", "Consented", row.consentStatus());
        assertEquals("Treatment history should match", "New patient", row.treatmentHistory());
        assertEquals("Culture should match", "Yes", row.culture());
        assertEquals("Smear microscopy should match", "Yes", row.smearMicroscopy());
        assertEquals("GeneXpert should match", "No", row.genexpert());
        assertEquals("Identification should match", "No", row.identification());
        assertEquals("DST first line should match", "Yes", row.dstFirstLine());
        assertEquals("DST second line should match", "No", row.dstSecondLine());
        assertEquals("Intended method should match", "LJ", row.intendedMethod());
        assertEquals("Received site should match", "Central Lab", row.receivedSite());
        assertEquals("Received date should match", "2025-01-15", row.receivedDate());
        assertEquals("Received time should match", "08:30", row.receivedTime());
        assertEquals("Number of samples should match", 2, row.numOfSamples());
    }

    @Test
    public void testBuildManifestDataContainsAllFields() {
        // Test the data building logic (simulating what buildManifestData does)
        TBManifestRow row = createFullManifestRow();
        Map<String, Object> data = buildManifestDataFromRow(row);

        // Verify all fields are present
        assertEquals("Specimen type should be in data", "Sputum", data.get("specimenType"));
        assertEquals("Specimen quality should be in data", "Good", data.get("specimenQuality"));
        assertEquals("Document number should be in data", "DOC-2025-001", data.get("documentNumber"));
        assertEquals("Referring facility should be in data", "District Hospital", data.get("referringFacility"));
        assertEquals("Patient name should be in data", "John Doe", data.get("patientName"));
        assertEquals("Patient age should be in data", "45", data.get("patientAge"));
        assertEquals("Patient sex should be in data", "Male", data.get("patientSex"));
        assertEquals("Patient ID should be in data", "PAT-001", data.get("patientId"));
        assertEquals("Study ID should be in data", "STUDY-001", data.get("studyId"));
        assertEquals("Patient address should be in data", "123 Main St", data.get("patientAddress"));
        assertEquals("Patient phone should be in data", "+1234567890", data.get("patientPhone"));
        assertEquals("Physician phone should be in data", "+0987654321", data.get("physicianPhone"));
        assertEquals("Consent status should be in data", "Consented", data.get("consentStatus"));
        assertEquals("Treatment history should be in data", "New patient", data.get("treatmentHistory"));
        assertEquals("Intended method should be in data", "LJ", data.get("intendedMethod"));
        assertEquals("Received site should be in data", "Central Lab", data.get("receivedSite"));
        assertEquals("Received date should be in data", "2025-01-15", data.get("receivedDate"));
        assertEquals("Received time should be in data", "08:30", data.get("receivedTime"));

        // Verify requested tests are combined correctly
        String requestedTests = (String) data.get("requestedTests");
        assertNotNull("Requested tests should be present", requestedTests);
        assertTrue("Should contain Culture", requestedTests.contains("Culture"));
        assertTrue("Should contain Smear Microscopy", requestedTests.contains("Smear Microscopy"));
        assertTrue("Should contain DST First Line", requestedTests.contains("DST First Line"));
        assertFalse("Should not contain GeneXpert", requestedTests.contains("GeneXpert"));
    }

    @Test
    public void testBuildManifestDataWithNullFields() {
        // Test that null fields are not added to the map
        TBManifestRow row = new TBManifestRow(1, // rowNumber
                null, // sampleId
                "Sputum", // specimenType - required
                null, // specimenQuality
                null, // documentNumber
                null, // referringFacility
                "Jane Doe", // patientName
                null, // patientAge
                null, // patientSex
                null, // patientId
                null, // studyId
                null, // patientAddress
                null, // patientPhone
                null, // physicianPhone
                null, // consentStatus
                null, // treatmentHistory
                null, // culture
                null, // smearMicroscopy
                null, // genexpert
                null, // identification
                null, // dstFirstLine
                null, // dstSecondLine
                null, // intendedMethod
                null, // receivedSite
                null, // receivedDate
                null, // receivedTime
                1 // numOfSamples
        );

        Map<String, Object> data = buildManifestDataFromRow(row);

        // Only non-null fields should be present
        assertEquals("Specimen type should be present", "Sputum", data.get("specimenType"));
        assertEquals("Patient name should be present", "Jane Doe", data.get("patientName"));
        assertNull("Document number should be null", data.get("documentNumber"));
        assertNull("Referring facility should be null", data.get("referringFacility"));
        assertNull("Requested tests should be null (no tests selected)", data.get("requestedTests"));
    }

    @Test
    public void testExternalIdGeneration() {
        // Test the external ID format: TB-{sampleId}-{sequence}
        assertEquals("TB-001-001", generateExternalId("001", 1));
        assertEquals("TB-001-010", generateExternalId("001", 10));
        assertEquals("TB-TB-001-001", generateExternalId("TB-001", 1));
        assertEquals("TB-SAMPLE-999", generateExternalId("SAMPLE", 999));
    }

    @Test
    public void testRequestedTestsCombination() {
        // Test various combinations of requested tests
        TBManifestRow allTests = new TBManifestRow(1, "TB-001", "Sputum", null, null, null, "Patient", null, null, null,
                null, null, null, null, null, null, "Yes", // culture
                "Yes", // smearMicroscopy
                "Yes", // genexpert
                "Yes", // identification
                "Yes", // dstFirstLine
                "Yes", // dstSecondLine
                null, null, null, null, 1);

        Map<String, Object> data = buildManifestDataFromRow(allTests);
        String requestedTests = (String) data.get("requestedTests");

        assertNotNull("Should have requested tests", requestedTests);
        assertTrue("Should contain all 6 tests", requestedTests.contains("Culture"));
        assertTrue("Should contain all 6 tests", requestedTests.contains("Smear Microscopy"));
        assertTrue("Should contain all 6 tests", requestedTests.contains("GeneXpert"));
        assertTrue("Should contain all 6 tests", requestedTests.contains("Identification"));
        assertTrue("Should contain all 6 tests", requestedTests.contains("DST First Line"));
        assertTrue("Should contain all 6 tests", requestedTests.contains("DST Second Line"));
    }

    // Helper method to create a full manifest row with all fields populated
    private TBManifestRow createFullManifestRow() {
        return new TBManifestRow(1, // rowNumber
                "TB-001", // sampleId
                "Sputum", // specimenType
                "Good", // specimenQuality
                "DOC-2025-001", // documentNumber
                "District Hospital", // referringFacility
                "John Doe", // patientName
                "45", // patientAge
                "Male", // patientSex
                "PAT-001", // patientId
                "STUDY-001", // studyId
                "123 Main St", // patientAddress
                "+1234567890", // patientPhone
                "+0987654321", // physicianPhone
                "Consented", // consentStatus
                "New patient", // treatmentHistory
                "Yes", // culture
                "Yes", // smearMicroscopy
                "No", // genexpert
                "No", // identification
                "Yes", // dstFirstLine
                "No", // dstSecondLine
                "LJ", // intendedMethod
                "Central Lab", // receivedSite
                "2025-01-15", // receivedDate
                "08:30", // receivedTime
                2 // numOfSamples
        );
    }

    /**
     * Replicates the buildManifestData logic from TBSampleCreationServiceImpl for
     * testing
     */
    private Map<String, Object> buildManifestDataFromRow(TBManifestRow row) {
        Map<String, Object> data = new HashMap<>();

        if (row.specimenType() != null)
            data.put("specimenType", row.specimenType());
        if (row.specimenQuality() != null)
            data.put("specimenQuality", row.specimenQuality());
        if (row.documentNumber() != null)
            data.put("documentNumber", row.documentNumber());
        if (row.referringFacility() != null)
            data.put("referringFacility", row.referringFacility());
        if (row.patientName() != null)
            data.put("patientName", row.patientName());
        if (row.patientAge() != null)
            data.put("patientAge", row.patientAge());
        if (row.patientSex() != null)
            data.put("patientSex", row.patientSex());
        if (row.patientId() != null)
            data.put("patientId", row.patientId());
        if (row.studyId() != null)
            data.put("studyId", row.studyId());
        if (row.patientAddress() != null)
            data.put("patientAddress", row.patientAddress());
        if (row.patientPhone() != null)
            data.put("patientPhone", row.patientPhone());
        if (row.physicianPhone() != null)
            data.put("physicianPhone", row.physicianPhone());
        if (row.consentStatus() != null)
            data.put("consentStatus", row.consentStatus());
        if (row.treatmentHistory() != null)
            data.put("treatmentHistory", row.treatmentHistory());

        // Requested tests as a combined string
        java.util.List<String> tests = new java.util.ArrayList<>();
        if ("Yes".equalsIgnoreCase(row.culture()))
            tests.add("Culture");
        if ("Yes".equalsIgnoreCase(row.smearMicroscopy()))
            tests.add("Smear Microscopy");
        if ("Yes".equalsIgnoreCase(row.genexpert()))
            tests.add("GeneXpert");
        if ("Yes".equalsIgnoreCase(row.identification()))
            tests.add("Identification");
        if ("Yes".equalsIgnoreCase(row.dstFirstLine()))
            tests.add("DST First Line");
        if ("Yes".equalsIgnoreCase(row.dstSecondLine()))
            tests.add("DST Second Line");
        if (!tests.isEmpty()) {
            data.put("requestedTests", String.join(", ", tests));
        }

        if (row.intendedMethod() != null)
            data.put("intendedMethod", row.intendedMethod());
        if (row.receivedSite() != null)
            data.put("receivedSite", row.receivedSite());
        if (row.receivedDate() != null)
            data.put("receivedDate", row.receivedDate());
        if (row.receivedTime() != null)
            data.put("receivedTime", row.receivedTime());

        return data;
    }

    /**
     * Replicates the generateExternalId logic from TBSampleCreationServiceImpl
     */
    private String generateExternalId(String sampleId, int sequenceNumber) {
        return String.format("TB-%s-%03d", sampleId, sequenceNumber);
    }
}
