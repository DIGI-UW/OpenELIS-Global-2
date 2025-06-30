package org.openelisglobal.address;

import static org.junit.Assert.*;

import java.util.List;
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

        assertNotNull("The address part list should not be null", addressPartList);
        assertFalse("The address part list should not be empty", addressPartList.isEmpty());

        for (AddressPart addressPart : addressPartList) {
            assertNotNull("AddressPart ID should not be null", addressPart.getId());
            assertNotNull("AddressPart partName should not be null", addressPart.getPartName());
            assertNotNull("AddressPart displayOrder should not be null", addressPart.getDisplayOrder());
        }
    }

    @Test
    public void getAll_shouldGetAllAddressParts() throws Exception {
        assertEquals(3, partService.getAll().size());
    }

    @Test
    public void createAddressPart_shouldCreateAddressPart() throws Exception {
        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        partService.save(part);
        assertEquals("PartName", part.getPartName());
        assertEquals("022", part.getDisplayOrder());
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

        assertEquals("updatedName", savedPart.getPartName());

    }

    @Test
    public void getAddressPartByNam_shouldReturnAddressPartByName() {
        AddressPart part = partService.getAddresPartByName("Village");

        assertEquals("Village", part.getPartName());
        assertEquals("1", part.getDisplayOrder());
    }
}
