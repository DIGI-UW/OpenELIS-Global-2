package org.openelisglobal.organisation;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationContactService;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OrganizationContactService organizationContactService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/organization.xml");
    }

    public void testDataBaseData() {
        List<OrganizationContact> organizationContacts = organizationContactService.getAll();
        organizationContacts.forEach(organizationContact -> {
            System.out.print(organizationContact.getPosition() + " ");
        });
    }

    @Test
    public void getListForOrganizationId() {
        List<OrganizationContact> organizationContacts = organizationContactService.getListForOrganizationId("3");
        assertEquals(2, organizationContacts.size());
        assertEquals("Manager", organizationContacts.get(0).getPosition());
        assertEquals("Coordinator", organizationContacts.get(1).getPosition());

    }

    @Test
    public void getAll_shouldReturnAllPreloadedContacts() {
        List<OrganizationContact> allContacts = organizationContactService.getAll();
        assertNotNull("getAll() must not return null", allContacts);
        assertTrue("Should return preloaded contacts from fixture", allContacts.size() > 0);
    }

    @Test
    public void getListForOrganizationId_withUnknownOrgId_shouldReturnEmptyList() {
        List<OrganizationContact> result = organizationContactService.getListForOrganizationId("99999");
        assertNotNull("Result must not be null for unknown org ID", result);
        assertTrue("Should return empty list for an org with no contacts", result.isEmpty());
    }

}
