package org.openelisglobal.address;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.OrganizationAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;

public class OrganizationAddressServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    OrganizationAddressService addressService;

    @Autowired
    AddressPartService partService;

    @Autowired
    OrganizationService orgService;

    @Before
    public void init() {
        addressService.deleteAll(addressService.getAll());
        partService.deleteAll(partService.getAll());
        orgService.deleteAll(orgService.getAll());
    }

    @After
    public void tearDown() {
        addressService.deleteAll(addressService.getAll());
        partService.deleteAll(partService.getAll());
        orgService.deleteAll(orgService.getAll());
    }

    @Test
    public void createOrganizationAdress_shouldCreateOrganisationAdress() throws Exception {

        Organization organization = new Organization();
        organization.setOrganizationName("MTN");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("Y");
        String orgId = orgService.insert(organization);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        OrganizationAddress address = new OrganizationAddress();
        address.setAddressPartId(partId);
        address.setOrganizationId(orgId);
        address.setType("B");
        address.setValue("123");

        Assert.assertEquals(0, addressService.getAll().size());

        addressService.save(address);

        Assert.assertEquals(1, addressService.getAll().size());
    }

    @Test
    public void getAddressPartsByOrganizationId_shouldReturnAddressPartsByOrganizationId() throws Exception {

        Organization organization = new Organization();
        organization.setOrganizationName("MTN");
        organization.setIsActive("Y");
        organization.setMlsSentinelLabFlag("Y");
        String orgId = orgService.insert(organization);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        OrganizationAddress address = new OrganizationAddress();
        address.setAddressPartId(partId);
        address.setOrganizationId(orgId);
        address.setType("B");
        address.setValue("123");

        addressService.save(address);

        Assert.assertEquals(1, addressService.getAddressPartsByOrganizationId(orgId).size());
    }
}