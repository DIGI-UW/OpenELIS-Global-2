package org.openelisglobal.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.inventory.service.InventoryReceiptService;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryRecieptServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryReceiptService inventoryRecieptService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-receipt.xml");
        // Initialize any required data or state here
    }

    @Test
    public void getAll_shouldReturnAllInventoryReceipts() {

        List<InventoryReceipt> receipts = inventoryRecieptService.getAll();

        assertEquals(2, receipts.size());
        assertEquals("1", receipts.get(0).getId());
        assertEquals("2", receipts.get(1).getId());
    }

    @Test
    public void getInventoryReceiptById_shouldReturnInventoryReceiptById() {
        InventoryReceipt receipt = inventoryRecieptService.getInventoryReceiptById("1");
        assertEquals("1", receipt.getId());
        assertEquals("1", receipt.getInventoryItemId());
    }

    @Test
    public void getAllInventoryReceipts_shouldReturnAllInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllInventoryReceipts();
        assertEquals(2, receipts.size());
        assertEquals("1", receipts.get(0).getId());
        assertEquals("2", receipts.get(1).getId());
    }

    @Test
    public void getData_shouldReturnInventoryReceiptData() {
        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setId("1");
        inventoryRecieptService.getData(receipt);
        assertEquals("1", receipt.getId());
        assertEquals("1", receipt.getInventoryItemId());
    }

    @Test
    public void getInventoryReceiptByInventoryItemId_shouldReturnInventoryReceiptByInventoryItemId() {
        InventoryReceipt receipt = inventoryRecieptService.getInventoryReceiptByInventoryItemId("1");
        assertEquals("1", receipt.getId());
        assertEquals("1", receipt.getInventoryItemId());
    }

    @Test
    public void getAllMatchingInventoryReceipts_shouldReturnAllMatchingInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatching("inventoryItemId", "1");
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnAllMatchingInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatching(map);
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getAllOrderedInventoryReceipts_shouldReturnAllOrderedInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllOrdered("id", false);
        assertEquals(2, receipts.size());
        assertEquals("1", receipts.get(0).getId());
        assertEquals("2", receipts.get(1).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrderedInventoryReceipts() {
        List<String> list = List.of("id");
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllOrdered(list, false);
        assertEquals(2, receipts.size());
        assertEquals("1", receipts.get(0).getId());
        assertEquals("2", receipts.get(1).getId());
    }

    @Test
    public void getAllMatchingOrderedInventoryReceipts_shouldReturnAllMatchingOrderedInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatchingOrdered("inventoryItemId", "1", "id",
                false);
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrderedInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatchingOrdered(map, "id", false);
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenListAndMap_shouldReturnAllMatchingOrderedInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<String> list = List.of("id");
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatchingOrdered(map, list, false);
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOredered() {
        List<String> list = List.of("id");
        List<InventoryReceipt> receipts = inventoryRecieptService.getAllMatchingOrdered("inventoryItemId", "1", list,
                false);
        assertEquals(1, receipts.size());
        assertEquals("1", receipts.get(0).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);

    }

    @Test
    public void getAllMatchingPage_shouldReturnPageOfMatchingInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingPage("inventoryItemId", "1", 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingPageGivenMap_shouldReturnPageOfMatchingInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingPage(map, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPage_shouldReturnPageOfMatchingOrderedInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingOrderedPage("inventoryItemId", "1", "id",
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenMap_shouldReturnPageOfMatchingOrderedInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenList_shouldReturnPageOfMatchingOrderedInventoryReceipts() {
        List<String> list = List.of("id");
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingOrderedPage("inventoryItemId", "1", list,
                false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getAllMatchingOrderedPageGivenListAndMap_shouldReturnPageOfMatchingOrderedInventoryReceipts() {
        Map<String, Object> map = Map.of("inventoryItemId", "1");
        List<String> list = List.of("id");
        List<InventoryReceipt> receipts = inventoryRecieptService.getMatchingOrderedPage(map, list, false, 1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(receipts.size() <= expectedPages);
    }

    @Test
    public void getPrevious_shouldReturnPreviousInventoryReceipt() {
        InventoryReceipt receipt = inventoryRecieptService.getPrevious("2");
        assertEquals("1", receipt.getId());
    }

    @Test
    public void getNext_shouldReturnNextInventoryReceipt() {
        InventoryReceipt receipt = inventoryRecieptService.getNext("1");
        assertEquals("2", receipt.getId());
    }

    @Test
    public void deleteAll_shouldDeleteAllInventoryReceipts() {
        List<InventoryReceipt> receipts = inventoryRecieptService.getAll();
        inventoryRecieptService.deleteAll(receipts);
        List<InventoryReceipt> receipts2 = inventoryRecieptService.getAll();
        assertEquals(0, receipts2.size());
    }

    @Test
    public void delete_shouldDeleteAReceipt() {
        InventoryReceipt receipt = inventoryRecieptService.getInventoryReceiptById("1");
        inventoryRecieptService.delete(receipt);
        List<InventoryReceipt> receipts = inventoryRecieptService.getAll();
        assertEquals(1, receipts.size());
        assertEquals("2", receipts.get(0).getId());
    }

    @Test
    public void getCount__shouldReturnCountOfInventoryReceipts() {
        int count = inventoryRecieptService.getCount();
        assertEquals(2, count);
    }

    @Test
    public void insert_shouldInsertInventoryReceipt() {
        List<InventoryReceipt> receipts1 = inventoryRecieptService.getAll();
        inventoryRecieptService.deleteAll(receipts1);
        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setInventoryItemId("1");
        String receiptId = inventoryRecieptService.insert(receipt);
        List<InventoryReceipt> receipts = inventoryRecieptService.getAll();
        assertEquals(1, receipts.size());
        assertEquals(receiptId, receipts.get(0).getId());
    }

    @Test
    public void update_shouldUpdateInventoryReceipt() {
        InventoryReceipt receipt = inventoryRecieptService.getInventoryReceiptById("1");
        receipt.setInventoryItemId("2");
        inventoryRecieptService.update(receipt);
        InventoryReceipt updatedReceipt = inventoryRecieptService.getInventoryReceiptById("1");
        assertEquals("2", updatedReceipt.getInventoryItemId());
    }

    @Test
    public void save_shouldSaveInventoryReceipt() {
        List<InventoryReceipt> receipts1 = inventoryRecieptService.getAll();
        inventoryRecieptService.deleteAll(receipts1);
        InventoryReceipt receipt = new InventoryReceipt();
        receipt.setInventoryItemId("1");
        InventoryReceipt savedReceipt = inventoryRecieptService.save(receipt);
        InventoryReceipt updatedReceipt = inventoryRecieptService.getInventoryReceiptById(savedReceipt.getId());
        assertEquals("1", updatedReceipt.getInventoryItemId());
    }

}
