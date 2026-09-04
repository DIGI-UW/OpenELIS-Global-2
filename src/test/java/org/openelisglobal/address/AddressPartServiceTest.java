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

    @Test
    public void insertAddressPart_shouldPersistAndBeRetrievableById() {
        AddressPart part = new AddressPart();
        part.setPartName("Pathway"); // ← change "Street" to "Pathway"
        part.setDisplayOrder("10");
        part.setSysUserId("1");

        String insertedId = partService.insert(part);

        assertNotNull("Inserted ID should not be null", insertedId);
        AddressPart retrieved = partService.get(insertedId);
        assertNotNull("Retrieved part should not be null", retrieved);
        assertEquals("Pathway", retrieved.getPartName());
        assertEquals("10", retrieved.getDisplayOrder());
    }

    @Test
    public void insertAddressPart_shouldIncreaseTotalCount() {
        int countBefore = partService.getAll().size();

        AddressPart part = new AddressPart();
        part.setPartName("Alley"); // short, under 20 chars
        part.setDisplayOrder("5");
        part.setSysUserId("1");
        partService.insert(part);

        assertEquals("Count should increase by 1", countBefore + 1, partService.getAll().size());
    }

    @Test
    public void updateDisplayOrder_shouldPersistNewDisplayOrder() {
        AddressPart part = new AddressPart();
        part.setPartName("Avenue"); // short, under 20 chars
        part.setDisplayOrder("30");
        part.setSysUserId("1");
        String id = partService.insert(part);

        AddressPart saved = partService.get(id);
        saved.setDisplayOrder("100");
        partService.save(saved);

        AddressPart reloaded = partService.get(id);
        assertEquals("100", reloaded.getDisplayOrder());
    }

    @Test
    public void insertTwoDifferentParts_shouldHaveDistinctIds() {
        AddressPart part1 = new AddressPart();
        part1.setPartName("Boulevard"); // short, under 20 chars
        part1.setDisplayOrder("40");
        part1.setSysUserId("1");

        AddressPart part2 = new AddressPart();
        part2.setPartName("Highway"); // short, under 20 chars
        part2.setDisplayOrder("41");
        part2.setSysUserId("1");

        String id1 = partService.insert(part1);
        String id2 = partService.insert(part2);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals("Two inserts must produce distinct IDs", id1, id2);
    }
}
