package org.openelisglobal.organisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    OrganizationService organisationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/organization.xml");
    }

    @Test
    public void getDatabaseData() {
        List<Organization> organizationList = organisationService.getAll();
        assertNotNull("Organization list should not be null", organizationList);
        assertFalse("Organization list should not be empty", organizationList.isEmpty());

        for (Organization organization : organizationList) {
            assertNotNull("Organization city should not be null", organization.getCity());
        }
    }

    @Test
    public void getData_shouldReturnDataGivenOrganization() {
        Organization organisation = new Organization();
        organisation.setId("3");
        organisationService.getData(organisation);
        assertEquals("New York", organisation.getCity());
        assertEquals("NY", organisation.getState());
    }

    @Test
    public void getActiveOrganizations_shouldReturnActiveOrganizations() {
        List<Organization> organizationList = organisationService.getActiveOrganizations();
        organizationList.forEach(organization -> {
            assertEquals("Y", organization.getIsActive());
        });
    }

    @Test
    public void getOrganizationByFhirId_shouldReturnOrganization() {
        String fhir_uuid1 = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";
        String fhir_uuid2 = "a3bdeff8-8d5b-4023-bd7c-932b4b98b6d4";
        Organization organization = organisationService.getOrganizationByFhirId(fhir_uuid1);
        assertEquals("New York", organization.getCity());
        assertEquals("NY", organization.getState());
        organization = organisationService.getOrganizationByFhirId(fhir_uuid2);
        assertEquals("Los Angeles", organization.getCity());
        assertEquals("CA", organization.getState());
    }

    @Test
    public void getOrganisationBtShortName_shouldReturnOrganization() {
        String shortName = "GHG";
        Organization organization = organisationService.getOrganizationByShortName(shortName, false);
        assertEquals("New York", organization.getCity());
        assertEquals("NY", organization.getState());
    }

    @Test
    public void deactivateOrganizations_shouldDeactivateActiveOrganizations() {
        List<Organization> organizationList = organisationService.getAll();
        organisationService.deactivateOrganizations(organizationList);
        organizationList = organisationService.getAll();
        organizationList.forEach(organization -> {
            assertEquals("N", organization.getIsActive());
        });
    }

    @Test
    public void deactivateAllOrganizations_shouldDeactivateAllOrganizations() {
        organisationService.deactivateAllOrganizations();
        List<Organization> organizationList = organisationService.getAll();
        organizationList.forEach(organization -> {
            assertEquals("N", organization.getIsActive());
        });
    }

    @Test
    public void activateOrganizations_shouldActivateDeactivatedOrganizations() {
        organisationService.deactivateAllOrganizations();
        List<Organization> organizationList = organisationService.getAll();
        List<String> organizationNames = organizationList.stream().map(Organization::getName)
                .collect(Collectors.toList());
        organisationService.activateOrganizations(organizationNames);
        organizationList = organisationService.getAll();
        organizationList.forEach(organization -> {
            assertEquals("Y", organization.getIsActive());
        });
    }

    @Test
    public void activateOrganizationsAndDeactivateOthers_shouldActivateDeactivatedOrganizationsAndDeactivateOthers() {
        organisationService.deactivateAllOrganizations();
        List<Organization> organizationList = organisationService.getAll();
        List<String> organizationNames = organizationList.stream().map(Organization::getName)
                .collect(Collectors.toList());
        organisationService.activateOrganizationsAndDeactivateOthers(organizationNames);
        organizationList = organisationService.getAll();
        organizationList.forEach(organization -> {
            assertEquals("Y", organization.getIsActive());
        });
    }

    @Test
    public void getPagesOfOrganizations_shouldReturnPagesOfOrganizations() {
        List<Organization> organizationList = organisationService.getPageOfOrganizations(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(organizationList.size() <= expectedPages);
    }

    @Test
    public void getTotalOrganizationCount_shouldReturnTotalOrganizationCount() {
        int totalOrganizationCount = organisationService.getTotalOrganizationCount();
        List<Organization> organizationList = organisationService.getAll();
        assertEquals(organizationList.size(), totalOrganizationCount);
    }

    @Test
    public void getTypeIdsForOrganizationId_shouldReturnTypeIdsForOrganizationId() {
        String organizationId = "3";
        List<String> typeIds = organisationService.getTypeIdsForOrganizationId(organizationId);
        assertTrue(typeIds.size() > 0);
    }

    @Test
    public void getOrganisationById_shouldReturnOrganisationGivenId() {
        String organizationId = "3";
        Organization organization = organisationService.getOrganizationById(organizationId);
        assertEquals("New York", organization.getCity());
        assertEquals("NY", organization.getState());
    }

    @Test
    public void getOrganizationsByTypeName_shouldReturnOrganizationsByTypeName() {
        String orderByProperty = "id";
        String referralOrgType = "Healthcare";
        List<Organization> organizationList = organisationService.getOrganizationsByTypeName(orderByProperty,
                referralOrgType);
        assertTrue(organizationList.size() > 0);

    }

    @Test
    public void getOrganizationsByTypeNameAndLeadingChars_shouldReturnOrganizationsByTypeNameAndLeadingChars() {
        String partialName = "Global";
        String typeName = "Healthcare";
        List<Organization> organizationList = organisationService.getOrganizationsByTypeNameAndLeadingChars(partialName,
                typeName);
        assertTrue(organizationList.size() > 0);
    }

    @Test
    public void getPagesOfSearchedOrganizations() {
        int startRecNo = 1;
        String searchString = "Global";
        List<Organization> organizationList = organisationService.getPagesOfSearchedOrganizations(startRecNo,
                searchString);
        assertTrue(organizationList.size() > 0);
    }

    @Test
    public void getOrganizations_shouldReturnOrganizations() {
        String filter = "Global";
        List<Organization> organizationList = organisationService.getOrganizations(filter);
        assertTrue(organizationList.size() > 0);
    }

    @Test
    public void deleteAllLinksForOrganization_shouldDeleteAllLinksForOrganization() {
        String id = "3";
        organisationService.deleteAllLinksForOrganization(id);
        List<String> typeIds = organisationService.getTypeIdsForOrganizationId(id);
        assertTrue(typeIds.size() == 0);
    }

    @Test
    public void linkOrganizationAndType_shouldLinkOrganizationAndType() {
        organisationService.deleteAllLinksForOrganization("3");
        String typeId = "1";
        Organization organization = organisationService.get("3");
        organisationService.linkOrganizationAndType(organization, typeId);
        String organizationId = "3";
        List<String> typeIds = organisationService.getTypeIdsForOrganizationId(organizationId);
        assertTrue(typeIds.size() > 0);

    }

    @Test
    public void getOrganizationsByParentId_shouldReturnOrganisationsGivenParentId() {
        List<Organization> organizationList = organisationService.getOrganizationsByParentId("3");
        assertTrue(organizationList.size() > 0);
    }

    @Test
    public void testGetOrganizationByCode() {
        Organization organization = organisationService.getOrganizationByCode("CHC003");
        assertNotNull(organization);
        assertEquals("5", organization.getId());
    }

    @Test
    public void testSearchOrganizationsWithTypes() {
        List<Organization> organizations = organisationService.searchOrganizationsWithTypes("Health");
        assertNotNull(organizations);
        assertEquals(2, organizations.size());
    }

    @Test
    public void testActivateOrganizations_WhenOrgNameContainsGivenOrganizationName() {
        List<Organization> organizations = organisationService.getAll();
        List<String> organizationNames = organizations.stream().map(Organization::getOrganizationName).toList();
        organisationService.activateOrganizations(organizationNames);
        organizations = organisationService.getAll();
        organizations.forEach(organization -> {
            assertEquals("Y", organization.getIsActive());
        });
    }

    @Test
    public void testGetOrganizationByLocalAbbreviation() {
        Organization org = organisationService.get("4");
        Organization organization = organisationService.getOrganizationByLocalAbbreviation(org, true);
        assertNotNull(organization);
    }

    @Test
    public void testGetAllOrganizations() {
        List<Organization> organizations = organisationService.getAllOrganizations();
        assertNotNull(organizations);
        assertFalse(organizations.isEmpty());
        assertEquals(3, organizations.size());
    }

    @Test
    public void testGetOrganizationsByTypeName_ShouldReturnOrganizationsWithOrderPropertyAndTypeName() {
        String[] typeNames = { "Healthcare", "referingClinic" };
        List<Organization> organizations = organisationService.getOrganizationsByTypeName("lastupdated", typeNames);
        assertNotNull(organizations);
        assertFalse(organizations.isEmpty());
        assertEquals(2, organizations.size());
    }

    @Test
    public void testSave_ShouldThrowExceptionForDuplicateRecords() {
        Organization organization = new Organization();
        organization.setId("8");
        organization.setOrganizationName("Global Health Org");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("N");
        organization.setOrganization(organisationService.get("3"));

        assertThrows(LIMSDuplicateRecordException.class, () -> {
            organisationService.save(organization);
        });
    }

    @Test
    public void testUpdate_ShouldThrowExceptionForDuplicateRecords() {
        Organization organization = new Organization();
        organization.setId("8");
        organization.setOrganizationName("Global Health Org");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("N");
        organization.setOrganization(organisationService.get("3"));

        assertThrows(LIMSDuplicateRecordException.class, () -> {
            organisationService.update(organization);
        });
    }

    @Test
    public void testInsert_ShouldThrowExceptionForDuplicateRecords() {
        Organization organization = new Organization();
        organization.setId("8");
        organization.setOrganizationName("Global Health Org");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("N");
        organization.setOrganization(organisationService.get("3"));

        assertThrows(LIMSDuplicateRecordException.class, () -> {
            organisationService.insert(organization);
        });
    }

    @Test
    public void testDelete_ShouldPerformSoftDelete() {
        Organization orgToDelete = organisationService.get("3");
        organisationService.delete(orgToDelete);
        Organization updatedOrg = organisationService.get("3");
        assertNotNull("Organization should still exist in the database (soft delete)", updatedOrg.getId());
        assertEquals("The isActive flag should be updated to NO/N", "N", updatedOrg.getIsActive());
    }

    @Test
    public void testGetTotalSearchedOrganizationCount_ReturnNumberOfSearchResults() {
        int resultCount = organisationService.getTotalSearchedOrganizationCount("Health");
        assertEquals(3, resultCount);
    }

    @Test
    public void testGetOrganizationByName() {
        Organization organization = organisationService.get("3");
        Organization returnedOrg = organisationService.getOrganizationByName(organization, true);
        assertNotNull(returnedOrg);
    }

    @Test
    public void testGetActiveOrganizationByName() {
        Organization organization = organisationService.get("3");
        Organization returnedOrg = organisationService.getActiveOrganizationByName(organization, true);
        assertNotNull(returnedOrg);
    }

}