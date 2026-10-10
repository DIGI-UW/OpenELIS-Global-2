package org.openelisglobal.organisation;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationTypeServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrganizationTypeService organizationTypeService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/organization.xml");
    }

    @Test
    public void testDatabaseData() {
        List<OrganizationType> organizationTypes = organizationTypeService.getAll();

        assertNotNull("OrganizationType list should not be null", organizationTypes);
        assertFalse("OrganizationType list should not be empty", organizationTypes.isEmpty());

        for (OrganizationType organizationType : organizationTypes) {
            assertNotNull("OrganizationType ID should not be null", organizationType.getId());
            assertNotNull("OrganizationType name should not be null", organizationType.getName());
        }
    }

    @Test
    public void getAllOrganizationTypes() {
        List<OrganizationType> organizationTypes = organizationTypeService.getAllOrganizationTypes();
        assertEquals(2, organizationTypes.size());
        assertEquals("Healthcare", organizationTypes.get(0).getName());
        assertEquals("referingClinic", organizationTypes.get(1).getName());
    }

    @Test
    public void getOrganizationTypeByName() {
        OrganizationType organizationType1 = organizationTypeService.getOrganizationTypeByName("referingClinic");
        assertEquals("referingClinic", organizationType1.getName());
        assertEquals("ReferingClinic Organization", organizationType1.getDescription());

        OrganizationType organizationType2 = organizationTypeService.getOrganizationTypeByName("Healthcare");
        assertEquals("Healthcare", organizationType2.getName());
        assertEquals("Healthcare Organization", organizationType2.getDescription());
    }

    @Test
    public void insert_shouldPersistNewOrganizationType() {
        OrganizationType newType = new OrganizationType();
        newType.setName("Laboratory");
        newType.setDescription("Laboratory Organization");
        newType.setSysUserId("1");

        String insertedId = organizationTypeService.insert(newType);
        assertNotNull("Inserted ID should not be null", insertedId);

        OrganizationType retrieved = organizationTypeService.getOrganizationTypeByName("Laboratory");
        assertNotNull("Inserted type should be retrievable by name", retrieved);
        assertEquals("Laboratory", retrieved.getName());
        assertEquals("Laboratory Organization", retrieved.getDescription());
    }

    @Test
    public void getOrganizationIdsForType_shouldReturnLinkedOrganizationIds() {
        // Type ID "1" corresponds to "Healthcare" in the fixture
        List<String> orgIds = organizationTypeService.getOrganizationIdsForType("1");
        assertNotNull("Result should not be null", orgIds);
        assertTrue("Should return at least one organization ID for type 1", orgIds.size() > 0);
    }

    @Test
    public void getOrganizationIdsForType_withUnknownTypeId_shouldReturnEmptyList() {
        List<String> orgIds = organizationTypeService.getOrganizationIdsForType("99999");
        assertNotNull("Result must not be null for unknown type ID", orgIds);
        assertTrue("Should return empty list for unknown type ID", orgIds.isEmpty());
    }

}
