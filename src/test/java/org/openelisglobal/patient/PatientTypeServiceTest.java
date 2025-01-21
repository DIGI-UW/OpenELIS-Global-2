package org.openelisglobal.patient;

import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PatientTypeService typeService;

    @Before
    public void init() {
        typeService.deleteAll(typeService.getAll());
    }

    @After
    public void tearDown() {
        typeService.deleteAll(typeService.getAll());
    }

    @Test
    public void createPatientType_shouldCreateNewPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        Assert.assertEquals(1, typeService.getAllPatientTypes().size());
        Assert.assertEquals("Test Type Description", savedPatientType.getDescription());
        Assert.assertEquals("Test Type", savedPatientType.getType());
    }

    @Test
    public void UpdatePatientType_shouldReturnUpdatedPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);
        savedPatientType.setType("Test2 Type");
        typeService.save(savedPatientType);

        Assert.assertEquals(1, typeService.getAllPatientTypes().size());
        Assert.assertEquals("Test Type Description", savedPatientType.getDescription());
        Assert.assertEquals("Test2 Type", savedPatientType.getType());
    }

    @Test
    public void deletePatientType_shouldDeletePatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);
        typeService.delete(savedPatientType);

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());
    }

    @Test
    public void getallPatientTypes_shouldReturnPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        Assert.assertEquals(1, typeService.getAllPatientTypes().size());
    }

    @Test
    public void getTotalPatientTypeCount_shouldReturnTotalPatientTypeCount() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        Assert.assertEquals(1, typeService.getTotalPatientTypeCount().longValue());
    }

    @Test
    public void getPatientTypes_shouldReturnListOfFilteredPatientTypes() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.get(patientTypeId);

        PatientType patientType2 = new PatientType();
        patientType2.setDescription("Test2 Type Description");
        patientType2.setType("Test2 Type");

        String patientTypeId2 = typeService.insert(patientType2);
        Assert.assertEquals(2, typeService.getAll().size());

        List<PatientType> savedPatientTypes = typeService.getPatientTypes("Test2");

        Assert.assertEquals(1, savedPatientTypes.size());
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
        Assert.assertEquals(2, typeService.getAll().size());

        List<PatientType> patientTypesPage = typeService.getPageOfPatientType(1);

        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        Assert.assertTrue(patientTypesPage.size() <= expectedPageSize);

        if (expectedPageSize >= 2) {
            Assert.assertTrue(patientTypesPage.stream().anyMatch(p -> p.getType().equals("Test Type")));
            Assert.assertTrue(patientTypesPage.stream().anyMatch(p -> p.getType().equals("Test2 Type")));
        }
    }

    @Test
    public void getData_shouldCopyPropertiesFromDatabase() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        String patientTypeId = typeService.insert(patientType);

        PatientType patientType2 = new PatientType();
        patientType2.setId(patientTypeId);
        typeService.getData(patientType2);

        Assert.assertEquals("Test Type", patientType2.getType());
    }

    @Test
    public void getallPatientTypeByName_shouldReturnPatientType() throws Exception {
        PatientType patientType = new PatientType();
        patientType.setDescription("Test Type Description");
        patientType.setType("Test Type");

        Assert.assertEquals(0, typeService.getAllPatientTypes().size());

        String patientTypeId = typeService.insert(patientType);
        PatientType savedPatientType = typeService.getPatientTypeByName(patientType);

        Assert.assertEquals("Test Type", savedPatientType.getType());
    }
}
