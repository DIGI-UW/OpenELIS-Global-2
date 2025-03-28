package org.openelisglobal.organisation;

import static org.junit.Assert.assertEquals;

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

}
