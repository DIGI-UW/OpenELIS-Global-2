package org.openelisglobal.patient.merge.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientMergeConsolidationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PRIMARY_PATIENT_ID = "9001";
    private static final String MERGED_PATIENT_ID = "9002";
    private static final String SYS_USER_ID = "1";

    @Autowired
    private PatientMergeConsolidationService consolidationService;

    @Autowired
    private PatientDAO patientDAO;

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Load system user data (required for admin user foreign key)
        executeDataSetWithStateManagement("testdata/system-user.xml");
        // Load patient merge test data
        executeDataSetWithStateManagement("testdata/patient-merge-testdata.xml");
    }

    /**
     * FR-004 Test: Verify sample_human FK records are reassigned to primary
     * patient.
     */
    @Test
    public void consolidateClinicalData_ShouldReassignSampleHumanRecords() {
        // Verify initial state - all samples belong to merged patient
        int initialMergedCount = countSampleHumanForPatient(MERGED_PATIENT_ID);
        int initialPrimaryCount = countSampleHumanForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 3 samples initially", 3, initialMergedCount);
        assertEquals("Primary patient should have 0 samples initially", 0, initialPrimaryCount);

        // Act
        PatientMergeConsolidationService.ConsolidationResult result = consolidationService
                .consolidateClinicalData(PRIMARY_PATIENT_ID, MERGED_PATIENT_ID, SYS_USER_ID);

        // Assert
        assertEquals("Should reassign 3 sample_human records", 3, result.getSamplesReassigned());

        int finalMergedCount = countSampleHumanForPatient(MERGED_PATIENT_ID);
        int finalPrimaryCount = countSampleHumanForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 0 samples after consolidation", 0, finalMergedCount);
        assertEquals("Primary patient should have 3 samples after consolidation", 3, finalPrimaryCount);
    }

    /**
     * FR-004 Test: Verify patient_contact FK records are reassigned to primary
     * patient.
     */
    @Test
    public void consolidateClinicalData_ShouldReassignPatientContactRecords() {
        // Verify initial state
        int initialMergedCount = countPatientContactForPatient(MERGED_PATIENT_ID);
        int initialPrimaryCount = countPatientContactForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 2 contacts initially", 2, initialMergedCount);
        assertEquals("Primary patient should have 0 contacts initially", 0, initialPrimaryCount);

        // Act
        PatientMergeConsolidationService.ConsolidationResult result = consolidationService
                .consolidateClinicalData(PRIMARY_PATIENT_ID, MERGED_PATIENT_ID, SYS_USER_ID);

        // Assert
        assertEquals("Should reassign 2 patient_contact records", 2, result.getContactsReassigned());

        int finalMergedCount = countPatientContactForPatient(MERGED_PATIENT_ID);
        int finalPrimaryCount = countPatientContactForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 0 contacts after consolidation", 0, finalMergedCount);
        assertEquals("Primary patient should have 2 contacts after consolidation", 2, finalPrimaryCount);
    }

    /**
     * FR-004 Test: Verify electronic_order FK records are reassigned to primary
     * patient.
     */
    @Test
    public void consolidateClinicalData_ShouldReassignElectronicOrderRecords() {
        // Verify initial state
        int initialMergedCount = countElectronicOrderForPatient(MERGED_PATIENT_ID);
        int initialPrimaryCount = countElectronicOrderForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 2 orders initially", 2, initialMergedCount);
        assertEquals("Primary patient should have 0 orders initially", 0, initialPrimaryCount);

        // Act
        PatientMergeConsolidationService.ConsolidationResult result = consolidationService
                .consolidateClinicalData(PRIMARY_PATIENT_ID, MERGED_PATIENT_ID, SYS_USER_ID);

        // Assert
        assertEquals("Should reassign 2 electronic_order records", 2, result.getOrdersReassigned());

        int finalMergedCount = countElectronicOrderForPatient(MERGED_PATIENT_ID);
        int finalPrimaryCount = countElectronicOrderForPatient(PRIMARY_PATIENT_ID);
        assertEquals("Merged patient should have 0 orders after consolidation", 0, finalMergedCount);
        assertEquals("Primary patient should have 2 orders after consolidation", 2, finalPrimaryCount);
    }

    /**
     * FR-004 Test: Verify patient_relations FK records are reassigned (both pat_id
     * and pat_id_source).
     */
    @Test
    public void consolidateClinicalData_ShouldReassignPatientRelationsRecords() {
        // Verify initial state - merged patient is involved in 2 relations
        int initialPatIdMerged = countPatientRelationsPatId(MERGED_PATIENT_ID);
        int initialPatIdSourceMerged = countPatientRelationsPatIdSource(MERGED_PATIENT_ID);
        assertEquals("Merged patient should have 1 pat_id relation initially", 1, initialPatIdMerged);
        assertEquals("Merged patient should have 1 pat_id_source relation initially", 1, initialPatIdSourceMerged);

        // Act
        PatientMergeConsolidationService.ConsolidationResult result = consolidationService
                .consolidateClinicalData(PRIMARY_PATIENT_ID, MERGED_PATIENT_ID, SYS_USER_ID);

        // Assert - both columns should be updated (total 2)
        assertEquals("Should reassign 2 patient_relations records (pat_id + pat_id_source)", 2,
                result.getRelationsReassigned());

        int finalPatIdMerged = countPatientRelationsPatId(MERGED_PATIENT_ID);
        int finalPatIdSourceMerged = countPatientRelationsPatIdSource(MERGED_PATIENT_ID);
        assertEquals("Merged patient should have 0 pat_id relations after consolidation", 0, finalPatIdMerged);
        assertEquals("Merged patient should have 0 pat_id_source relations after consolidation", 0,
                finalPatIdSourceMerged);

        // Verify primary now has these relations
        int finalPatIdPrimary = countPatientRelationsPatId(PRIMARY_PATIENT_ID);
        int finalPatIdSourcePrimary = countPatientRelationsPatIdSource(PRIMARY_PATIENT_ID);
        assertEquals("Primary should have 1 pat_id relation after consolidation", 1, finalPatIdPrimary);
        assertEquals("Primary should have 1 pat_id_source relation after consolidation", 1, finalPatIdSourcePrimary);
    }

    /**
     * FR-004 Test: Verify all clinical data is consolidated in a single operation.
     */
    @Test
    public void consolidateClinicalData_ShouldReassignAllRecordTypes() {
        // Act
        PatientMergeConsolidationService.ConsolidationResult result = consolidationService
                .consolidateClinicalData(PRIMARY_PATIENT_ID, MERGED_PATIENT_ID, SYS_USER_ID);

        // Assert - total of 9 records (3 samples + 2 contacts + 2 orders + 2 relations)
        assertEquals("Should reassign 3 samples", 3, result.getSamplesReassigned());
        assertEquals("Should reassign 2 contacts", 2, result.getContactsReassigned());
        assertEquals("Should reassign 2 orders", 2, result.getOrdersReassigned());
        assertEquals("Should reassign 2 relations", 2, result.getRelationsReassigned());
        assertEquals("Total reassigned should be 9", 9, result.getTotalReassigned());
    }

    /**
     * FR-009 Test: Verify empty fields in primary are filled from merged patient.
     */
    @Test
    public void mergeDemographics_ShouldFillEmptyFieldsFromMergedPatient() {
        // Load patients
        Patient primaryPatient = patientDAO.getData(PRIMARY_PATIENT_ID);
        Patient mergedPatient = patientDAO.getData(MERGED_PATIENT_ID);

        assertNotNull("Primary patient should exist", primaryPatient);
        assertNotNull("Merged patient should exist", mergedPatient);

        Person primaryPerson = primaryPatient.getPerson();
        Person mergedPerson = mergedPatient.getPerson();

        assertNotNull("Primary person should exist", primaryPerson);
        assertNotNull("Merged person should exist", mergedPerson);

        // Verify initial state - primary has gaps, merged has complete data
        assertTrue("Primary state should be empty", isEmpty(primaryPerson.getState()));
        assertTrue("Primary zip should be empty", isEmpty(primaryPerson.getZipCode()));
        assertTrue("Primary email should be empty", isEmpty(primaryPerson.getEmail()));

        assertEquals("Merged state should have value", "Nairobi County", mergedPerson.getState());
        assertEquals("Merged zip should have value", "00100", mergedPerson.getZipCode().trim());
        assertEquals("Merged email should have value", "merged@test.com", mergedPerson.getEmail());

        // Act
        List<String> mergedFields = consolidationService.mergeDemographics(primaryPatient, mergedPatient);

        // Assert - gaps should be filled
        assertTrue("Should merge state field", mergedFields.contains("state"));
        assertTrue("Should merge zipCode field", mergedFields.contains("zipCode"));
        assertTrue("Should merge email field", mergedFields.contains("email"));

        assertEquals("Primary state should now have merged value", "Nairobi County", primaryPerson.getState());
        assertEquals("Primary zip should now have merged value", "00100", primaryPerson.getZipCode().trim());
        assertEquals("Primary email should now have merged value", "merged@test.com", primaryPerson.getEmail());
    }

    /**
     * FR-009 Test: Verify primary values are NOT overwritten by merged values.
     */
    @Test
    public void mergeDemographics_ShouldPreservePrimaryValues() {
        // Load patients
        Patient primaryPatient = patientDAO.getData(PRIMARY_PATIENT_ID);
        Patient mergedPatient = patientDAO.getData(MERGED_PATIENT_ID);

        Person primaryPerson = primaryPatient.getPerson();
        Person mergedPerson = mergedPatient.getPerson();

        // Verify initial state - both have city values
        assertEquals("Primary city should be Kampala", "Kampala", primaryPerson.getCity());
        assertEquals("Merged city should be Nairobi", "Nairobi", mergedPerson.getCity());

        // Act
        List<String> mergedFields = consolidationService.mergeDemographics(primaryPatient, mergedPatient);

        // Assert - city should NOT be in merged fields (primary value preserved)
        assertTrue("Should NOT merge city field (primary has value)", !mergedFields.contains("city"));
        assertEquals("Primary city should remain Kampala", "Kampala", primaryPerson.getCity());
    }

    /**
     * FR-009 Test: Verify empty dataset returns no merged fields.
     */
    @Test
    public void mergeDemographics_WhenNoGaps_ShouldReturnEmptyList() {
        // Create patients with complete primary demographics
        Patient primary = new Patient();
        Person primaryPerson = new Person();
        primaryPerson.setStreetAddress("123 Main St");
        primaryPerson.setCity("City");
        primaryPerson.setState("State");
        primaryPerson.setZipCode("12345");
        primaryPerson.setCountry("Country");
        primaryPerson.setPrimaryPhone("111");
        primaryPerson.setWorkPhone("222");
        primaryPerson.setCellPhone("333");
        primaryPerson.setFax("444");
        primaryPerson.setEmail("email@test.com");
        primary.setPerson(primaryPerson);

        Patient merged = new Patient();
        Person mergedPerson = new Person();
        mergedPerson.setEmail("other@test.com");
        merged.setPerson(mergedPerson);

        // Act
        List<String> mergedFields = consolidationService.mergeDemographics(primary, merged);

        // Assert - no fields should be merged
        assertTrue("Should return empty list when no gaps", mergedFields.isEmpty());
    }

    // ============================================
    // Helper Methods Updated to DBUnit / Pure JDBC
    // ============================================

    private int getCount(String query, String patientId) {
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setLong(1, Long.parseLong(patientId));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute test assertion count query", e);
        }
        return 0;
    }

    private int countSampleHumanForPatient(String patientId) {
        return getCount("SELECT COUNT(*) FROM sample_human WHERE patient_id = ?", patientId);
    }

    private int countPatientContactForPatient(String patientId) {
        return getCount("SELECT COUNT(*) FROM patient_contact WHERE patient_id = ?", patientId);
    }

    private int countElectronicOrderForPatient(String patientId) {
        return getCount("SELECT COUNT(*) FROM electronic_order WHERE patient_id = ?", patientId);
    }

    private int countPatientRelationsPatId(String patientId) {
        return getCount("SELECT COUNT(*) FROM patient_relations WHERE pat_id = ?", patientId);
    }

    private int countPatientRelationsPatIdSource(String patientId) {
        return getCount("SELECT COUNT(*) FROM patient_relations WHERE pat_id_source = ?", patientId);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
