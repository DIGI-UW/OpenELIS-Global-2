package org.openelisglobal.projectorganization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.projectorganization.service.ProjectOrganizationService;
import org.openelisglobal.projectorganization.valueholder.ProjectOrganization;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectOrganizationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ProjectOrganizationService projectOrganizationService;

    private List<ProjectOrganization> projectOrganizationList;
    private List<ProjectOrganization> patientContacts;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int PAGE_SIZE = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/project-organization.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2022-09-14 15:00:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("organization");
    }

    @Test
    public void getAll_ShouldReturnAllProjectOrganizations() {
        projectOrganizationList = projectOrganizationService.getAll();
        assertNotNull(projectOrganizationList);
        assertEquals(4, projectOrganizationList.size());
        assertEquals("202", projectOrganizationList.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingProjectOrganizationsGivenAPropertyName() {
        projectOrganizationList = projectOrganizationService.getAllMatching("lastupdated",
                Timestamp.valueOf("2022-09-14 15:00:00"));
        assertNotNull(projectOrganizationList);
        assertEquals(3, projectOrganizationList.size());
        assertEquals("1002", projectOrganizationList.get(0).getOrganization().getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingProjectOrganizationsGivenAMap() {
        projectOrganizationList = projectOrganizationService.getAllMatching(propertyValues);
        assertNotNull(projectOrganizationList);
        assertEquals(3, projectOrganizationList.size());
        assertEquals("204", projectOrganizationList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedProjectOrganizationsFilteredByAPropertyNameInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getAllOrdered("project", true);
        assertNotNull(projectOrganizationList);
        assertEquals(4, projectOrganizationList.size());
        assertEquals("1002", projectOrganizationList.get(2).getOrganization().getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedProjectOrganizationsFilteredByOrderPropertiesInDescendingOrder() {
        orderProperties.add("project");
        projectOrganizationList = projectOrganizationService.getAllOrdered(orderProperties, true);
        assertNotNull(projectOrganizationList);
        assertEquals(4, projectOrganizationList.size());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedProjectOrganizationsFilteredByAPropertyNameAndOrderedByAnOrderedPropertyInAscendingOrder() {
        projectOrganizationList = projectOrganizationService.getAllMatchingOrdered("project", 102, "organization",
                false);
        assertNotNull(projectOrganizationList);
        assertEquals(2, projectOrganizationList.size());
        assertEquals("102", projectOrganizationList.get(0).getProject().getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedProjectOrganizationsFilteredByAPatientIdAndOrderedByOrderPropertiesInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getAllMatchingOrdered("project", 102, orderProperties,
                true);
        assertNotNull(projectOrganizationList);
        assertEquals(2, projectOrganizationList.size());
        assertEquals("205", projectOrganizationList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedProjectOrganizationsFilteredByAMapAndOrderedByLastUpdatedInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(projectOrganizationList);
        assertEquals(3, projectOrganizationList.size());
        assertEquals("205", projectOrganizationList.get(2).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResultsGivenPageNumber() {
        projectOrganizationList = projectOrganizationService.getPage(1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("204", projectOrganizationList.get(2).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(4, projectOrganizationList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAPropertyNameAndValue() {
        projectOrganizationList = projectOrganizationService.getMatchingPage("organization", 1001, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("205", projectOrganizationList.get(1).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(2, projectOrganizationList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResultsFilteredByAMap() {
        projectOrganizationList = projectOrganizationService.getMatchingPage(propertyValues, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("204", projectOrganizationList.get(1).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(3, projectOrganizationList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsFilteredByAnOrderPropertyInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getOrderedPage("organization", true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("201", projectOrganizationList.get(2).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(4, projectOrganizationList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResultsGivenAListAndOrderedInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getOrderedPage(orderProperties, false, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("202", projectOrganizationList.get(2).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(4, projectOrganizationList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsGivenAPropertyNameAndValueOrderedByPersonInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2022-09-14 15:00:00"), "project", true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("202", projectOrganizationList.get(0).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(3, projectOrganizationList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsGivenLastUpdatedAndOrderedByOrderPropertiesInAscendingOrder() {
        projectOrganizationList = projectOrganizationService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2023-06-04 13:00:00"), orderProperties, false, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("201", projectOrganizationList.get(0).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(1, projectOrganizationList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsFilteredByAMapAndOrderedByOrderPropertyInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getMatchingOrderedPage(propertyValues, "organization",
                true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("204", projectOrganizationList.get(1).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(3, projectOrganizationList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResultsFilteredByAListAndAMapInDescendingOrder() {
        projectOrganizationList = projectOrganizationService.getMatchingOrderedPage(propertyValues, orderProperties,
                true, 1);
        PAGE_SIZE = Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(projectOrganizationList);
        assertFalse(projectOrganizationList.isEmpty());
        assertEquals("204", projectOrganizationList.get(1).getId());
        assertTrue(PAGE_SIZE >= projectOrganizationList.size());
        assertEquals(3, projectOrganizationList.size());
    }

    @Test
    public void deleteProjectOrganization_ShouldDeleteAProjectOrganizationPassedAsParameter() {
        List<ProjectOrganization> initialProjectOrganizations = projectOrganizationService.getAll();
        assertEquals(4, initialProjectOrganizations.size());
        ProjectOrganization patientContact = projectOrganizationService.get("202");
        boolean isFound = initialProjectOrganizations.stream().anyMatch(pc -> "202".equals(pc.getId()));
        assertTrue(isFound);
        projectOrganizationService.delete(patientContact);
        List<ProjectOrganization> deletedProjectOrganization = projectOrganizationService.getAll();
        boolean isStillFound = deletedProjectOrganization.stream().anyMatch(pc -> "202".equals(pc.getId()));
        assertFalse(isStillFound);
        assertEquals(3, deletedProjectOrganization.size());
    }

    @Test
    public void deleteAllProjectOrganizations_ShouldDeleteAllProjectOrganizations() {
        List<ProjectOrganization> initialProjectOrganizations = projectOrganizationService.getAll();
        assertFalse(initialProjectOrganizations.isEmpty());
        projectOrganizationService.deleteAll(initialProjectOrganizations);
        List<ProjectOrganization> delectedProjectOrganizations = projectOrganizationService.getAll();
        assertNotNull(delectedProjectOrganizations);
        assertTrue(delectedProjectOrganizations.isEmpty());
    }
}
