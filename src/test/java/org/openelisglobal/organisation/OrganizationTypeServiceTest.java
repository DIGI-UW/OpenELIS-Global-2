package org.openelisglobal.organisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
}
