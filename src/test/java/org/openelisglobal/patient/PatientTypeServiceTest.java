package org.openelisglobal.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.jupiter.BaseWebContextSensitiveTests;
import org.openelisglobal.patient.service.PatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientTypeServiceTest extends BaseWebContextSensitiveTests {

    @Autowired
    PatientTypeService typeService;

    @BeforeEach
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/patient.xml");
    }

    @AfterEach
    public void tearDown() {
        typeService.deleteAll(typeService.getAll());
    }

    @Test
    public void createPatientType_shouldCreateNewPatientType() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "patient_type", "patient", "person", "patient_identity" });
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        assertEquals(1, typeService.getAllPatientTypes().size());
        assertEquals("Test Type Description", savedPatientType.getDescription());
        assertEquals("Test Type", savedPatientType.getType());

        typeService.delete(savedPatientType);
    }

    @Test
    public void UpdatePatientType_shouldReturnUpdatedPatientType() throws Exception {
        PatientType savedPatientType = typeService.get("6");
        savedPatientType.setType("Test2 Type");
        typeService.save(savedPatientType);

        assertEquals("Discharged", savedPatientType.getDescription());
        assertEquals("Test2 Type", savedPatientType.getType());
    }

    @Test
    public void deletePatientType_shouldDeletePatientType() throws Exception {
        PatientType savedPatientType = typeService.get("6");
        typeService.delete(savedPatientType);

        assertEquals(0, typeService.getAllPatientTypes().size());
    }

    @Test
    public void getallPatientTypes_shouldReturnPatientType() throws Exception {
        assertEquals(1, typeService.getAllPatientTypes().size());
    }

    @Test
    public void getTotalPatientTypeCount_shouldReturnTotalPatientTypeCount() throws Exception {
        assertEquals(1, typeService.getTotalPatientTypeCount().longValue());
    }

    @Test
    public void getPatientTypes_shouldReturnListOfFilteredPatientTypes() throws Exception {
        List<PatientType> savedPatientTypes = typeService.getPatientTypes("Discharged");
        assertEquals(1, savedPatientTypes.size());
    }

    @Test
    public void getPageOfPatientType_shouldReturnPatientTypes() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");
        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        PatientType patientType2 = new PatientType();
        patientType2.setDescription("Test2 Type Description");
        patientType2.setType("Test2 Type");
        String patientTypeId2 = typeService.insert(patientType2);

        assertEquals(3, typeService.getAll().size());

        List<PatientType> patientTypesPage = typeService.getPageOfPatientType(1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        assertTrue(patientTypesPage.size() <= expectedPageSize);

        if (expectedPageSize >= 2) {
            assertTrue(patientTypesPage.stream().anyMatch(p -> p.getType().equals("Test Type")));
            assertTrue(patientTypesPage.stream().anyMatch(p -> p.getType().equals("Test2 Type")));
        }
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() throws Exception {
        String patientTypeId = "6";

        PatientType patientType2 = new PatientType();
        patientType2.setId(patientTypeId);
        typeService.getData(patientType2);

        assertEquals("D", patientType2.getType());
    }

    @Test
    public void getallPatientTypeByName_shouldReturnPatientType() throws Exception {
        PatientType patientType = typeService.get("6");
        PatientType savedPatientType = typeService.getPatientTypeByName(patientType);
        assertEquals("D", savedPatientType.getType());
    }
}
