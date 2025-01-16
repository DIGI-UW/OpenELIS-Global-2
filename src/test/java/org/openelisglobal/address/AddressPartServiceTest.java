package org.openelisglobal.address;

import org.junit.Assert;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.springframework.beans.factory.annotation.Autowired;

/*
     * this test class depends on the preloaded dummy data from the database
     * check; schema:Clinlims User:Clinlims Password: Clinlims table:address_part
     */
public class AddressPartServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    AddressPartService partService;

    @Test
    public void createAddressPart_shouldCreateAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(6, partService.getAll().size());

        partService.save(part);

        Assert.assertEquals(7, partService.getAll().size());
        Assert.assertEquals("PartName", part.getPartName());
        Assert.assertEquals("022", part.getDisplayOrder());

        partService.delete(part);
    }

    @Test
    public void getAll_shouldGetAllAddressParts() throws Exception {

        Assert.assertEquals(6, partService.getAll().size());

    }

    @Test
    public void updateAddressPart_shouldUpdateAddressPart() throws Exception {
        AddressPart savedPart = partService.get("1");
        savedPart.setPartName("upadtedName");
        partService.save(savedPart);

        Assert.assertEquals("upadtedName", savedPart.getPartName());

    }

    @Test
    public void deleteAddressPart_shouldDeleteAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(6, partService.getAll().size());

        String partId = partService.insert(part);
        AddressPart savedPart = partService.get(partId);
        partService.delete(savedPart);

        Assert.assertEquals(6, partService.getAll().size());

    }

    @Test
    public void getAddressPartByName_shouldReturnAddressPartByName() throws Exception {
        AddressPart part = partService.getAddresPartByName("village");

        Assert.assertEquals("3", part.getId());
    }
}
