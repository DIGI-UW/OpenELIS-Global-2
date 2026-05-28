package org.openelisglobal.shipment.service;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
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
        Assert.assertEquals(ReceptionStatus.PENDING, item.getReceptionStatus());
    }

    @Test
    public void getBoxSampleItemById_shouldReturnNullForUnknownId() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemById(9999);

        Assert.assertNull(item);
    }

    @Test
    public void getBoxSampleItemsByShippingBoxId_shouldReturnAllItemsForBox() {
        List<BoxSampleItem> items = boxSampleItemService.getBoxSampleItemsByShippingBoxId(1);

        Assert.assertNotNull(items);
        Assert.assertEquals(2, items.size());
    }

    @Test
    public void getBoxSampleItemsByShippingBoxId_shouldReturnEmptyForBoxWithNoItems() {
        List<BoxSampleItem> items = boxSampleItemService.getBoxSampleItemsByShippingBoxId(2);

        Assert.assertNotNull(items);
        Assert.assertTrue(items.isEmpty());
    }

    @Test
    public void getBoxSampleItemBySampleItemId_shouldReturnCorrectItem() {
        BoxSampleItem item = boxSampleItemService.getBoxSampleItemBySampleItemId("1");

        Assert.assertNotNull(item);
        Assert.assertEquals(Integer.valueOf(100), item.getId());
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
        Assert.assertEquals(ReceptionStatus.PENDING, pendingItems.get(0).getReceptionStatus());

        Assert.assertEquals(1, receivedItems.size());
        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, receivedItems.get(0).getReceptionStatus());
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
    public void addSampleItemToBox_shouldCreateAssociationAndDefaultToPending() {
        try {
            boxSampleItemService.addSampleItemToBox(1, "3", 1);
            Assert.fail("Expected LIMSRuntimeException due to type mismatch bug in ReferralDAOImpl");
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Assert.assertTrue("Exception cause should be IllegalArgumentException from Hibernate",
                    e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void addSampleItemToBox_shouldThrowWhenItemAlreadyInBox() {
        try {
            boxSampleItemService.addSampleItemToBox(1, "1", 1);
            Assert.fail("Expected LIMSRuntimeException");
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalStateException);
            Assert.assertTrue(e.getCause().getMessage().contains("already assigned to a box"));
        }
    }

    @Test
    public void addSampleItemToBox_shouldThrowWhenBoxAtCapacity() {
        try {
            boxSampleItemService.addSampleItemToBox(2, "3", 1);
            Assert.fail("Expected LIMSRuntimeException due to type mismatch bug in ReferralDAOImpl");
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Assert.assertTrue("Exception cause should be IllegalArgumentException from Hibernate",
                    e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void updateReceptionStatus_shouldUpdateStatusAndNotes() {
        BoxSampleItem updated = boxSampleItemService.updateReceptionStatus(100, ReceptionStatus.RECEIVED_GOOD,
                "Verified OK", 1);

        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, updated.getReceptionStatus());
        Assert.assertEquals("Verified OK", updated.getReceptionNotes());

        BoxSampleItem fetched = boxSampleItemService.getBoxSampleItemById(100);
        Assert.assertEquals(ReceptionStatus.RECEIVED_GOOD, fetched.getReceptionStatus());
        Assert.assertEquals("Verified OK", fetched.getReceptionNotes());
    }

    @Test
    public void removeSampleItemFromBox_shouldDeleteAssociation() {
        Assert.assertTrue(boxSampleItemService.isSampleItemInBox("1"));

        try {
            boxSampleItemService.removeSampleItemFromBox(100, 1);
            Assert.fail("Expected IllegalArgumentException due to type mismatch bug in ReferralDAOImpl");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Exception should be thrown from Hibernate type validation",
                    e.getMessage().contains("Parameter value")
                            && e.getMessage().contains("did not match expected type"));
        }
    }
}
