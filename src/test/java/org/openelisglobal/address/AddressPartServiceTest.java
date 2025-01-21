package org.openelisglobal.address;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.springframework.beans.factory.annotation.Autowired;

public class AddressPartServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    AddressPartService partService;

    @Before
    public void init() {
        partService.deleteAll(partService.getAll());
    }

    @After
    public void tearDown() {
        partService.deleteAll(partService.getAll());
    }

    @Test
    public void createAddressPart_shouldCreateAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(0, partService.getAll().size());

        partService.save(part);

        Assert.assertEquals(1, partService.getAll().size());
        Assert.assertEquals("PartName", part.getPartName());
        Assert.assertEquals("022", part.getDisplayOrder());
    }

    @Test
    public void getAll_shouldGetAllAddressParts() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        partService.save(part);

        AddressPart part2 = new AddressPart();
        part2.setPartName("PartName2");
        part2.setDisplayOrder("023");

        partService.save(part2);

        Assert.assertEquals(2, partService.getAll().size());

    }

    @Test
    public void updateAddressPart_shouldUpdateAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(0, partService.getAll().size());

        String partId = partService.insert(part);
        AddressPart savedPart = partService.get(partId);
        savedPart.setPartName("upadtedName");
        partService.save(savedPart);

        Assert.assertEquals("upadtedName", savedPart.getPartName());

    }

    @Test
    public void deleteAddressPart_shouldDeleteAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(0, partService.getAll().size());

        String partId = partService.insert(part);
        AddressPart savedPart = partService.get(partId);
        savedPart.setPartName("upadtedName");
        partService.delete(savedPart);

        Assert.assertEquals(0, partService.getAll().size());

    }

    @Test
    public void getAddressPartByNam_shouldReturnAddressPartByName() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        Assert.assertEquals(0, partService.getAll().size());

        partService.save(part);

        Assert.assertEquals("022", part.getDisplayOrder());
    }
}
