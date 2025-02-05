
package org.openelisglobal.address;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * this test class depends on the preloaded dummy data from the database for both AddressPart and Organisation
 * check; schema:Clinlims User:Clinlims Password: Clinlims table:address_part
 * check; schema:Clinlims User:Clinlims Password: Clinlims table:organisation
 */
public class OrganizationAddressServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    OrganizationAddressService addressService;

    @Autowired
    OrganizationService orgService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/organization-address.xml");
    }

    @Test
    public void createOrganizationAddress_shouldCreateOrganisationAddress() throws Exception {

        Organization organization = new Organization();
        organization.setOrganizationName("MTN");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("Y");
        String orgId = orgService.insert(organization);

        OrganizationAddress address = new OrganizationAddress();
        address.setAddressPartId("6");
        address.setOrganizationId(orgId);
        address.setType("v");
        address.setValue("Lumumba Street");

        Assert.assertEquals(3, addressService.getAll().size());

        addressService.save(address);

        Assert.assertEquals(4, addressService.getAll().size());
    }

    @Test
    public void getAddressPartsByOrganizationId_shouldReturnAddressPartsByOrganizationId() throws Exception {
        List<OrganizationAddress> orgAddresses = addressService.getAddressPartsByOrganizationId("3");

        Assert.assertEquals(2, orgAddresses.size());
        Assert.assertEquals("The first element should be Amore", orgAddresses.get(0).getValue(), "Amore");
        Assert.assertEquals("The first element should be 12345678", orgAddresses.get(1).getValue(), "12345678");
    }
}
