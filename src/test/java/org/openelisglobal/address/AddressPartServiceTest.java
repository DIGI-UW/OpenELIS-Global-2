package org.openelisglobal.address;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

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
    
    @Test(expected = Exception.class)
public void saveAddressPart_withEmptyPartName_shouldThrowException() throws Exception {
    AddressPart part = new AddressPart();
    part.setPartName(""); 
    part.setDisplayOrder("001");

    partService.save(part);
}

@Test(expected = Exception.class)
public void saveDuplicateAddressPart_shouldThrowException() throws Exception {
    AddressPart part1 = new AddressPart();
    part1.setPartName("DuplicatePartName");
    part1.setDisplayOrder("001");

    AddressPart part2 = new AddressPart();
    part2.setPartName("DuplicatePartName");
    part2.setDisplayOrder("002");

    partService.save(part1);
    partService.save(part2); 
}

@Test
public void getAddressPartByName_withNonExistentName_shouldReturnNull() throws Exception {
    AddressPart part = partService.getAddresPartByName("NonExistentName");

    Assert.assertNull(part);
}

@Test
public void getAll_whenNoPartsExist_shouldReturnEmptyList() throws Exception {
    List<AddressPart> parts = partService.getAll();

    Assert.assertTrue(parts.isEmpty());
}

@Test
public void deleteNonExistentAddressPart_shouldNotThrowException() throws Exception {
    AddressPart part = new AddressPart();
    part.setId("NonExistentId");

    partService.delete(part);

    Assert.assertEquals(0, partService.getAll().size()); 
}

@Test(expected = Exception.class)
public void saveAddressPart_withInvalidDisplayOrder_shouldThrowException() throws Exception {
    AddressPart part = new AddressPart();
    part.setPartName("ValidName");
    part.setDisplayOrder("InvalidOrder");

    partService.save(part);
}

@Test
public void saveAddressPart_withNullOptionalFields_shouldSaveSuccessfully() throws Exception {
    AddressPart part = new AddressPart();
    part.setPartName("NameOnly"); 

    partService.save(part);

    AddressPart savedPart = partService.get(part.getId());
    Assert.assertEquals("NameOnly", savedPart.getPartName());
    Assert.assertNull(savedPart.getDisplayOrder()); 
}

@Test
public void saveAddressPart_withLongPartName_shouldSaveSuccessfully() throws Exception {
    AddressPart part = new AddressPart();
    part.setPartName("A".repeat(255)); 
    part.setDisplayOrder("001");

    partService.save(part);

    AddressPart savedPart = partService.get(part.getId());
    Assert.assertEquals("A".repeat(255), savedPart.getPartName());
}
}
