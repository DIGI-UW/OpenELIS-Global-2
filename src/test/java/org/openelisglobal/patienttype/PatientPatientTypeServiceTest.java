package org.openelisglobal.patienttype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientPatientTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientPatientTypeService patientPatientTypeService;

    private List<PatientPatientType> patientPatientTypes;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-patient-type.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-01 12:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("patientId");
    }

    @Test
    public void getPatientTypeForPatient_ShouldReturnPatientTypeUsingAPatientId() {
        PatientType patientType = patientPatientTypeService.getPatientTypeForPatient("1001");
        assertNotNull(patientType);
        assertEquals("Inpatient", patientType.getType());
    }

    @Test
    public void getPatientPatientTypeForPatient_ShouldReturnPatientPatientTypeUsingAPatientId() {
        PatientPatientType patientPatientType = patientPatientTypeService.getPatientPatientTypeForPatient("1001");
        assertNotNull(patientPatientType);
        assertEquals(Timestamp.valueOf("2025-06-01 12:00:00.0"), patientPatientType.getLastupdated());
    }

    @Test
    public void getAll_ShouldReturnAllPatientTypes() {
        patientPatientTypes = patientPatientTypeService.getAll();
        assertNotNull(patientPatientTypes);
        assertEquals(3, patientPatientTypes.size());
        assertEquals("3", patientPatientTypes.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingPatientTypes_UsingPropertyNameAndValue() {
        patientPatientTypes = patientPatientTypeService.getAllMatching("patientId", "1002");
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("3", patientPatientTypes.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingPatientTypes_UsingAMap() {
        patientPatientTypes = patientPatientTypeService.getAllMatching(propertyValues);
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("2", patientPatientTypes.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedPatientTypes_UsingAnOrderProperty() {
        patientPatientTypes = patientPatientTypeService.getAllOrdered("patientTypeId", false);
        assertNotNull(patientPatientTypes);
        assertEquals(3, patientPatientTypes.size());
        assertEquals("3", patientPatientTypes.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        patientPatientTypes = patientPatientTypeService.getAllOrdered(orderProperties, true);
        assertNotNull(patientPatientTypes);
        assertEquals(3, patientPatientTypes.size());
        assertEquals("2", patientPatientTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedPatientTypes_UsingPropertyNameAndValueAndAnOrderProperty() {
        patientPatientTypes = patientPatientTypeService.getAllMatchingOrdered("patientId", "1002", "lastupdated", true);
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("2", patientPatientTypes.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedPatientTypes_UsingPropertyNameAndValueAndAList() {
        patientPatientTypes = patientPatientTypeService.getAllMatchingOrdered("patientId", "1002", orderProperties,
                true);
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("2", patientPatientTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedPatientTypes_UsingAMapAndAnOrderProperty() {
        patientPatientTypes = patientPatientTypeService.getAllMatchingOrdered(propertyValues, "patientTypeId", true);
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("2", patientPatientTypes.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedPatientTypes_UsingAMapAndAList() {
        patientPatientTypes = patientPatientTypeService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(patientPatientTypes);
        assertEquals(2, patientPatientTypes.size());
        assertEquals("1", patientPatientTypes.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfPatientTypes_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfPatientTypes_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingPage("patientId", "1001", 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfPatientTypes_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfPatientTypes_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfPatientTypes_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfPatientTypes_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingOrderedPage("patientId", "1002", "lastupdated", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfPatientTypes_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingOrderedPage("patientId", "1002", orderProperties,
                true, 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfPatientTypes_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingOrderedPage(propertyValues, "patientId", false, 1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfPatientTypes_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        patientPatientTypes = patientPatientTypeService.getMatchingOrderedPage(propertyValues, orderProperties, false,
                1);
        assertTrue(NUMBER_OF_PAGES >= patientPatientTypes.size());
    }

    @Test
    public void delete_ShouldDeleteAPatientType() {
        patientPatientTypes = patientPatientTypeService.getAll();
        assertEquals(3, patientPatientTypes.size());
        PatientPatientType patientPatientType = patientPatientTypeService.get("2");
        patientPatientTypeService.delete(patientPatientType);
        List<PatientPatientType> newPatientPatientTypes = patientPatientTypeService.getAll();
        assertEquals(2, newPatientPatientTypes.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllPatientTypes() {
        patientPatientTypes = patientPatientTypeService.getAll();
        assertEquals(3, patientPatientTypes.size());
        patientPatientTypeService.deleteAll(patientPatientTypes);
        List<PatientPatientType> updatedPatientPatientTypes = patientPatientTypeService.getAll();
        assertTrue(updatedPatientPatientTypes.isEmpty());
    }
}
