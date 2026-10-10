
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
        List<OrganizationAddress> orgAddresses = addressService.getAddressPartsByOrganizationId("1000");

        Assert.assertEquals(2, orgAddresses.size());
        Assert.assertEquals("The first element should be Amore", orgAddresses.get(0).getValue(), "Amore");
        Assert.assertEquals("The first element should be 12345678", orgAddresses.get(1).getValue(), "12345678");
    }

    @Test
    public void getAll_shouldReturnAllPreloadedOrganizationAddresses() throws Exception {
        List<OrganizationAddress> all = addressService.getAll();

        Assert.assertNotNull("getAll() must not return null", all);
        Assert.assertEquals("Should have exactly 3 preloaded organisation addresses", 3, all.size());
    }

    @Test
    public void getAddressPartsByOrganizationId_withUnknownOrgId_shouldReturnEmptyList() throws Exception {
        List<OrganizationAddress> result = addressService.getAddressPartsByOrganizationId("99999");

        Assert.assertNotNull("Result must not be null for an unknown org ID", result);
        Assert.assertTrue("Should return empty list for an org with no addresses", result.isEmpty());
    }

    @Test
    public void updateOrganizationAddress_shouldPersistNewValue() throws Exception {
        // Fetch the first address belonging to org 1000
        List<OrganizationAddress> addresses = addressService.getAddressPartsByOrganizationId("1000");
        Assert.assertFalse("Need at least one address to update", addresses.isEmpty());

        OrganizationAddress toUpdate = addresses.get(0);
        toUpdate.setValue("UpdatedStreetName");
        addressService.save(toUpdate);

        // Re-fetch and verify
        List<OrganizationAddress> reloaded = addressService.getAddressPartsByOrganizationId("1000");
        boolean found = reloaded.stream().anyMatch(a -> "UpdatedStreetName".equals(a.getValue()));
        Assert.assertTrue("Updated value should be persisted and visible on reload", found);
    }
}
