package org.openelisglobal.patientidentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientIdentityServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientIdentityService patientIdentityService;

    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;

    @Autowired
    private PatientService patientService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-identity.xml");
    }

    public void testDataInDataBase() {
        List<PatientIdentity> patientIdentities = patientIdentityService.getAll();
        patientIdentities.forEach(patientIdentity -> {
            System.out.print(patientIdentity.getIdentityData() + " ");
        });
    }

    @Test
    public void getAll_shouldReturnAllPatientIdentities() {
        List<PatientIdentity> patientIdentities = patientIdentityService.getAll();
        assertTrue(patientIdentities.size() == 3);
    }

    @Test
    public void get_shouldReturnPatientIdentityWithGivenId() {
        PatientIdentity patientIdentity = patientIdentityService.get("1");
        assertNotNull(patientIdentity);
        assertEquals("334-422-A", patientIdentity.getIdentityData());
        assertEquals("1", patientIdentity.getPatientId());
        assertEquals("9", patientIdentity.getIdentityTypeId());
    }

    @Test
    public void getPatientIdentitiesForPatient_shouldReturnAllIdentitiesForPatient() {
        List<PatientIdentity> patientIdentities = patientIdentityService.getPatientIdentitiesForPatient("1");
        // assertTrue(patientIdentities.size() == 1);
        assertEquals(3, patientIdentities.size());
        assertEquals("334-422-A", patientIdentities.get(0).getIdentityData());
    }

    @Test
    public void getPatientIdentitiesForPatient_shouldReturnEmptyListForNonExistingPatient() {
        List<PatientIdentity> patientIdentities = patientIdentityService.getPatientIdentitiesForPatient("999");
        assertTrue(patientIdentities.isEmpty());
    }

    @Test
    public void getPatitentIdentityForPatientAndType_shouldReturnMatchingIdentity() {
        PatientIdentity patientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType("1", "9");
        assertNotNull(patientIdentity);
        assertEquals("334-422-A", patientIdentity.getIdentityData());
    }

    @Test
    public void getPatitentIdentityForPatientAndType_shouldReturnNullForNonExistingMatch() {
        PatientIdentity patientIdentity = patientIdentityService.getPatitentIdentityForPatientAndType("1", "5");
        assertEquals(null, patientIdentity);
    }

    @Test
    public void getPatientIdentitiesByValueAndType_shouldReturnMatchingIdentities() {
        List<PatientIdentity> patientIdentities = patientIdentityService.getPatientIdentitiesByValueAndType("334-422-A",
                "9");
        assertTrue(patientIdentities.size() == 1);
        assertEquals("1", patientIdentities.get(0).getPatientId());
    }

    @Test
    public void getPatientIdentitiesByValueAndType_shouldReturnEmptyListForNonExistingMatch() {
        List<PatientIdentity> patientIdentities = patientIdentityService
                .getPatientIdentitiesByValueAndType("NONEXISTENT", "1");
        assertTrue(patientIdentities.isEmpty());
    }

    @Test
    public void save_shouldSavePatientIdentityToDatabase() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_identity" });
        // Create a new patient identity
        PatientIdentity patientIdentity = new PatientIdentity();
        patientIdentity.setIdentityTypeId("9");
        patientIdentity.setPatientId("2");
        patientIdentity.setIdentityData("NEW_PASSPORT_123");
        assertEquals(0, patientIdentityService.getAll().size());

        // Save the patient identity
        patientIdentityService.save(patientIdentity);

        // Verify that the patient identity was saved
        // PatientIdentity savedIdentity = patientIdentityService.get("1");
        // assertNotNull(savedIdentity);
        // assertEquals("NEW_PASSPORT_123", savedIdentity.getIdentityData());
        assertEquals(1, patientIdentityService.getAll().size());

        patientIdentityService.delete(patientIdentity);
    }

    @Test
    public void update_shouldUpdateExistingPatientIdentity() {
        // Get an existing patient identity
        PatientIdentity patientIdentity = patientIdentityService.get("1");
        assertNotNull(patientIdentity);

        // Update the patient identity
        String updatedData = "UPDATED_DATA";
        patientIdentity.setIdentityData(updatedData);
        patientIdentityService.update(patientIdentity);

        // Verify that the patient identity was updated
        PatientIdentity updatedIdentity = patientIdentityService.get("1");
        assertEquals(updatedData, updatedIdentity.getIdentityData());
    }

    @Test
    public void delete_shouldRemovePatientIdentityFromDatabase() {
        // Get the count before deletion
        List<PatientIdentity> beforeDelete = patientIdentityService.getAll();
        int countBeforeDelete = beforeDelete.size();

        // Delete a patient identity
        PatientIdentity patientIdentity = patientIdentityService.get("1");
        patientIdentityService.delete(patientIdentity);

        // Verify that the patient identity was deleted
        List<PatientIdentity> afterDelete = patientIdentityService.getAll();
        int countAfterDelete = afterDelete.size();
        assertEquals(countBeforeDelete - 1, countAfterDelete);
    }
}