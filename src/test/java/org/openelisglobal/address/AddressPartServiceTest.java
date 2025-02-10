package org.openelisglobal.address;

import java.util.List;
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
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/address-part.xml");
    }

    @Test
    public void verifyTestData() {
        List<AddressPart> addressPartList = partService.getAll();
        System.out.println("address parts we have in db: " + addressPartList.size());
        addressPartList.forEach(addressPart -> System.out.println(
                addressPart.getId() + " - " + addressPart.getPartName() + " - " + addressPart.getDisplayOrder()));
    }

    @Test
    public void getAll_shouldGetAllAddressParts() throws Exception {
        Assert.assertEquals(3, partService.getAll().size());
    }

    @Test
    public void createAddressPart_shouldCreateAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        partService.save(part);
        Assert.assertEquals("PartName", part.getPartName());
        Assert.assertEquals("022", part.getDisplayOrder());
    }

    @Test
    public void updateAddressPart_shouldUpdateAddressPart() {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);
        AddressPart savedPart = partService.get(partId);
        savedPart.setPartName("updatedName");
        partService.save(savedPart);

        Assert.assertEquals("updatedName", savedPart.getPartName());

    }

    @Test
    public void getAddressPartByNam_shouldReturnAddressPartByName() {
        AddressPart part = partService.getAddresPartByName("Village");

        Assert.assertEquals("Village", part.getPartName());
        Assert.assertEquals("1", part.getDisplayOrder());
    }
}
