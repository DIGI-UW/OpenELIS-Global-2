package org.openelisglobal.patientidentitytype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientIdentityTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientIdentityTypeService patientIdentityTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-identity-type.xml");
    }

    @Test
    public void getAll_shouldReturnAllPatientIdentityTypes() {
        List<PatientIdentityType> patientIdentityTypes = patientIdentityTypeService.getAll();
        assertTrue(patientIdentityTypes.size() == 3);
    }

    @Test
    public void get_shouldReturnPatientIdentityType() {
        PatientIdentityType patientIdentityType = patientIdentityTypeService.get("9");
        assertEquals("NATIONAL", patientIdentityType.getIdentityType());
        assertEquals("Patient ID Number", patientIdentityType.getDescription());
    }

    @Test
    public void getNamedIdentityType_shouldReturnNamedIdentityType() {
        PatientIdentityType patientIdentityType = patientIdentityTypeService.getNamedIdentityType("NATIONAL");
        assertNotNull(patientIdentityType);
        assertEquals("Patient ID Number", patientIdentityType.getDescription());
    }

    @Test
    public void getAllPatientIdenityTypes_shouldReturnAllPatientIdentityTypes() {
        List<PatientIdentityType> patientIdentityTypes = patientIdentityTypeService.getAllPatientIdenityTypes();
        assertTrue(patientIdentityTypes.size() == 3);
    }

    @Test
    public void insert_shouldInsertPatientIdentityType() {
        PatientIdentityType patientIdentityType = new PatientIdentityType();
        patientIdentityType.setIdentityType("NEWTYPE");
        patientIdentityType.setDescription("New Descrpyion");
        patientIdentityTypeService.insert(patientIdentityType);
        List<PatientIdentityType> patientIdentityTypes = patientIdentityTypeService.getAllPatientIdenityTypes();
        assertTrue(patientIdentityTypes.size() == 4);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void insert_shouldThrowExceptionForDuplicatePatientIdentityType() {
        PatientIdentityType patientIdentityType = new PatientIdentityType();
        patientIdentityType.setIdentityType("NATIONAL"); // Already exists
        patientIdentityType.setDescription("Duplicate National ID");
        patientIdentityTypeService.insert(patientIdentityType);
    }

    @Test
    public void save_shouldSavePatientIdentityType() {
        PatientIdentityType patientIdentityType = new PatientIdentityType();
        patientIdentityType.setIdentityType("NEWSAVETYPE");
        patientIdentityType.setDescription("New Save Descrpyion");
        patientIdentityTypeService.save(patientIdentityType);
        List<PatientIdentityType> patientIdentityTypes = patientIdentityTypeService.getAllPatientIdenityTypes();
        assertTrue(patientIdentityTypes.size() == 4);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void save_shouldThrowExceptionForDuplicatePatientIdentityType() {
        PatientIdentityType patientIdentityType = new PatientIdentityType();
        patientIdentityType.setIdentityType("NATIONAL"); // Already exists
        patientIdentityType.setDescription("Duplicate Passport");
        patientIdentityTypeService.save(patientIdentityType);
    }

    @Test
    public void update_shouldUpdatePatientIdentityType() {
        PatientIdentityType patientIdentityType = patientIdentityTypeService.get("9");
        patientIdentityType.setDescription("Updated Passport Number");
        patientIdentityType.setIdentityType("PASSPORT");
        assertTrue(patientIdentityTypeService.getAll().size() == 3);
        patientIdentityTypeService.update(patientIdentityType);
        PatientIdentityType updatedType = patientIdentityTypeService.get("9");
        assertEquals("Updated Passport Number", updatedType.getDescription());
        assertEquals("PASSPORT", updatedType.getIdentityType());
        assertTrue(patientIdentityTypeService.getAll().size() == 3);
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void update_shouldThrowExceptionForDuplicatePatientIdentityType() {
        PatientIdentityType patientIdentityType = patientIdentityTypeService.get("9");
        patientIdentityType.setIdentityType("NATIONAL"); // Already exists in record with id="1"
        patientIdentityTypeService.update(patientIdentityType);
    }

    @Test
    public void delete_shouldDeletePatientIdentityType() {
        PatientIdentityType patientIdentityType = patientIdentityTypeService.get("9");
        patientIdentityTypeService.delete(patientIdentityType);
        List<PatientIdentityType> patientIdentityTypes = patientIdentityTypeService.getAllPatientIdenityTypes();
        assertTrue(patientIdentityTypes.size() == 2);
    }
}