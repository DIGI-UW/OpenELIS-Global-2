package org.openelisglobal.shipment.service;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class BoxSampleItemServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/box_sample_item_service.xml");
    }

    @Test
    public void getBoxSampleItemById_shouldReturnExistingItem() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemById(100);

        Assert.assertNotNull(item);
        Assert.assertEquals(Integer.valueOf(100), item.getId());
        Assert.assertEquals(Integer.valueOf(1), item.getShippingBox().getId());
        Assert.assertEquals("1", item.getSampleItem().getId());
        Assert.assertEquals(ReceptionStatus.PENDING, item.getReceptionStatus());
        Assert.assertNull(item.getReceptionNotes());
    }

    @Test
    public void getBoxSampleItemById_shouldReturnNullForUnknownId() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemById(9999);

        Assert.assertNull(item);
    }

    @Test
    public void getBoxSampleItemsByShippingBoxId_shouldReturnAllItemsForBox() {
        List<BoxSampleItem> items = boxSampleItemService.getBoxSampleItemsByShippingBoxId(1);

        Assert.assertEquals(2, items.size());
    }

    @Test
    public void getBoxSampleItemsByShippingBoxId_shouldReturnEmptyForBoxWithNoItems() {
        List<BoxSampleItem> items = boxSampleItemService.getBoxSampleItemsByShippingBoxId(2);

        Assert.assertEquals(0, items.size());
    }

    @Test
    public void getBoxSampleItemDTOsByShippingBoxId_shouldReturnEmptyForBoxWithNoItems() {
        List<SampleItemDTO> dtos = boxSampleItemService.getBoxSampleItemDTOsByShippingBoxId(2);

        Assert.assertEquals(0, dtos.size());
    }

    @Test
    public void getBoxSampleItemBySampleItemId_shouldReturnCorrectItem() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemBySampleItemId("1");

        Assert.assertNotNull(item);
        Assert.assertEquals(Integer.valueOf(100), item.getId());
        Assert.assertEquals(Integer.valueOf(1), item.getShippingBox().getId());
        Assert.assertEquals("1", item.getSampleItem().getId());
        Assert.assertEquals(ReceptionStatus.PENDING, item.getReceptionStatus());
        Assert.assertNull(item.getReceptionNotes());
    }

    @Test
    public void getBoxSampleItemBySampleItemId_shouldReturnNullForUnassignedSampleItem() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemBySampleItemId("3");

        Assert.assertNull(item);
    }

    @Test
    public void getBoxSampleItemsByReceptionStatus_shouldReturnMatchingItems() {
        List<BoxSampleItem> pendingItems = boxSampleItemService.getBoxSampleItemsByReceptionStatus(1,
                ReceptionStatus.PENDING);

        List<BoxSampleItem> receivedItems = boxSampleItemService.getBoxSampleItemsByReceptionStatus(1,
                ReceptionStatus.RECEIVED_GOOD);

        Assert.assertEquals(1, pendingItems.size());

        BoxSampleItem pending = pendingItems.get(0);

        Assert.assertNotNull(pending);
        Assert.assertEquals(Integer.valueOf(100), pending.getId());
        Assert.assertEquals(Integer.valueOf(1), pending.getShippingBox().getId());
        Assert.assertEquals("1", pending.getSampleItem().getId());
        Assert.assertEquals(ReceptionStatus.PENDING, pending.getReceptionStatus());

        Assert.assertEquals(1, receivedItems.size());

        BoxSampleItem received = receivedItems.get(0);

        Assert.assertNotNull(received);
        Assert.assertEquals(Integer.valueOf(101), received.getId());
        Assert.assertEquals(Integer.valueOf(1), received.getShippingBox().getId());
        Assert.assertEquals("2", received.getSampleItem().getId());
        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, received.getReceptionStatus());
        Assert.assertEquals("Arrived in good condition", received.getReceptionNotes());
    }

    @Test
    public void isSampleItemInBox_shouldReturnTrueForAssignedItem() {
        Assert.assertTrue(boxSampleItemService.isSampleItemInBox("1"));
    }

    @Test
    public void isSampleItemInBox_shouldReturnFalseForUnassignedItem() {
        Assert.assertFalse(boxSampleItemService.isSampleItemInBox("3"));
    }

    @Test
    public void countSampleItemsInBox_shouldReturnCorrectCount() {
        int count = boxSampleItemService.countSampleItemsInBox(1);

        Assert.assertEquals(2, count);
    }

    @Test
    public void countSampleItemsInBox_shouldReturnZeroForEmptyBox() {
        int count = boxSampleItemService.countSampleItemsInBox(2);

        Assert.assertEquals(0, count);
    }

    @Test
    public void updateReceptionStatus_shouldUpdateStatusAndNotes() {
        BoxSampleItem updated = boxSampleItemService.updateReceptionStatus(100, ReceptionStatus.RECEIVED_GOOD,
                "Verified OK", 1);

        Assert.assertEquals(Integer.valueOf(100), updated.getId());
        Assert.assertEquals(Integer.valueOf(1), updated.getShippingBox().getId());
        Assert.assertEquals("1", updated.getSampleItem().getId());
        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, updated.getReceptionStatus());
        Assert.assertEquals("Verified OK", updated.getReceptionNotes());

        BoxSampleItem fetched = boxSampleItemService.getBoxSampleItemById(100);

        Assert.assertEquals(Integer.valueOf(100), fetched.getId());
        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, fetched.getReceptionStatus());
        Assert.assertEquals("Verified OK", fetched.getReceptionNotes());
    }
}
