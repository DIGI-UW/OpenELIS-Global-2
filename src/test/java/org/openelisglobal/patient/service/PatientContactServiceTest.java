package org.openelisglobal.patient;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientContactService patientContactService;

    private List<PatientContact> patientContacts;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/patient_contact.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("patientId");
    }

    @Test
    public void getAll_ShouldReturnAllPatientContacts() {
        patientContacts = patientContactService.getAll();
        assertNotNull(patientContacts);
        assertEquals(13, patientContacts.size());
        assertEquals("8002", patientContacts.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingPatientContactsGivenAPropertyName() {
        patientContacts = patientContactService.getAllMatching("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        assertNotNull(patientContacts);
        assertEquals(5, patientContacts.size());
        assertEquals("8013", patientContacts.get(4).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingPatientContactsGivenAMap() {
        patientContacts = patientContactService.getAllMatching(propertyValues);
        assertNotNull(patientContacts);
        assertEquals(5, patientContacts.size());
        assertEquals("8013", patientContacts.get(4).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedPatientContactsFilteredByAPropertyNameInDescendingOrder() {
        patientContacts = patientContactService.getAllOrdered("patientId", true);
        assertNotNull(patientContacts);
        assertEquals(13, patientContacts.size());
        assertEquals("Cobby", patientContacts.get(10).getPerson().getLastName());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedPatientContactsFilteredByOrderPropertiesInDescendingOrder() {
        orderProperties.add("patientId");
        patientContacts = patientContactService.getAllOrdered(orderProperties, true);
        assertNotNull(patientContacts);
        assertEquals(13, patientContacts.size());
        assertEquals("cobpeters@peters.com", patientContacts.get(7).getPerson().getEmail());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedPatientContactsFilteredByAPropertyNameAndOrderedByAnOrderedPropertyInAscendingOrder() {
        patientContacts = patientContactService.getAllMatchingOrdered("patientId", 3001, "patientId", false);
        assertNotNull(patientContacts);
        assertEquals(3, patientContacts.size());
        assertEquals("701", patientContacts.get(2).getPerson().getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedPatientContactsFilteredByAPatientIdAndOrderedByOrderPropertiesInDescendingOrder() {
        patientContacts = patientContactService.getAllMatchingOrdered("patientId", 3001, orderProperties, true);
        assertNotNull(patientContacts);
        assertEquals(3, patientContacts.size());
        assertEquals("8013", patientContacts.get(2).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedPatientContactsFilteredByAMapAndOrderedByLastUpdatedInDescendingOrder() {
        patientContacts = patientContactService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(patientContacts);
        assertEquals(5, patientContacts.size());
        assertEquals("8010", patientContacts.get(4).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResultsGivenPageNumber() {
        patientContacts = patientContactService.getPage(10);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8012", patientContacts.get(2).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(4, patientContacts.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAPropertyNameAndValue() {
        patientContacts = patientContactService.getMatchingPage("person", 701, 2);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8013", patientContacts.get(2).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(3, patientContacts.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAMap() {
        patientContacts = patientContactService.getMatchingPage(propertyValues, 3);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8010", patientContacts.get(1).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(3, patientContacts.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsFilteredByAnOrderPropertyInDescendingOrder() {
        patientContacts = patientContactService.getOrderedPage("patientId", true, 3);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8002", patientContacts.get(9).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(11, patientContacts.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsGivenAListAndOrderedInAscendingOrder() {
        patientContacts = patientContactService.getOrderedPage(orderProperties, false, 7);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8008", patientContacts.get(5).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(7, patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsGivenAPropertyNameAndValueOrderedByPersonInDescendingOrder() {
        patientContacts = patientContactService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), "person", true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8001", patientContacts.get(3).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(5, patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsGivenLastUpdatedAndOrderedByOrderPropertiesInAscendingOrder() {
        patientContacts = patientContactService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2024-06-10 11:45:00"), orderProperties, false, 3);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8011", patientContacts.get(1).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(5, patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsFilteredByAMapAndOrderedByOrderPropertyInDescendingOrder() {
        patientContacts = patientContactService.getMatchingOrderedPage(propertyValues, "patientId", true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8005", patientContacts.get(1).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(5, patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsFilteredByAListAndAMapInDescendingOrder() {
        patientContacts = patientContactService.getMatchingOrderedPage(propertyValues, orderProperties, true, 4);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(patientContacts);
        assertFalse(patientContacts.isEmpty());
        assertEquals("8013", patientContacts.get(1).getId());
        assertTrue(PAGE_SIZE >= patientContacts.size());
        assertEquals(2, patientContacts.size());
    }

    @Test
    public void deletePatientContact_ShouldDeleteAPatientContactPassedAsParameter() {
        List<PatientContact> initialPatientContacts = patientContactService.getAll();
        assertEquals(13, initialPatientContacts.size());
        PatientContact patientContact = patientContactService.get("8003");
        boolean isFound = initialPatientContacts.stream().anyMatch(pc -> "8003".equals(pc.getId()));
        assertTrue(isFound);
        patientContactService.delete(patientContact);
        List<PatientContact> deletedPatientContact = patientContactService.getAll();
        boolean isStillFound = deletedPatientContact.stream().anyMatch(pc -> "8003".equals(pc.getId()));
        assertFalse(isStillFound);
        assertEquals(12, deletedPatientContact.size());
    }

    @Test
    public void deleteAllPatientContacts_ShouldDeleteAllPatientContacts() {
        List<PatientContact> initialPatientContacts = patientContactService.getAll();
        assertFalse(initialPatientContacts.isEmpty());
        patientContactService.deleteAll(initialPatientContacts);
        List<PatientContact> delectedPatientContacts = patientContactService.getAll();
        assertNotNull(delectedPatientContacts);
        assertTrue(delectedPatientContacts.isEmpty());
    }
}
