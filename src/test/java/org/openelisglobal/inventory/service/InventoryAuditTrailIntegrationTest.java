package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * Integration test to verify that inventory entities (InventoryItem,
 * InventoryLot, InventoryUsage) properly create audit trail entries in the
 * history table when created, updated, or deleted. Also tests that the audit
 * trail data can be properly transformed for UI consumption.
 */
@Rollback
public class InventoryAuditTrailIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Autowired
    private InventoryStorageLocationService storageLocationService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    private static final String TEST_USER_ID = "1";

    @Before
    public void setup() throws Exception {
        // Load test data including reference_tables registrations for audit trail
        executeDataSetWithStateManagement("testdata/inventory-audit-test-data.xml");
    }

    // ============================================================================
    // INVENTORY ITEM AUDIT TRAIL TESTS
    // ============================================================================

    @Test
    public void testInventoryItem_Insert_CreatesHistoryEntry() {
        // Given: A new inventory item
        InventoryItem item = createTestInventoryItem("Audit Test Item - INSERT");
        item.setSysUserId(TEST_USER_ID);

        // When: Item is inserted
        Long itemId = inventoryItemService.insert(item);
        assertNotNull("Item should be inserted", itemId);

        // Then: History entry should be created
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");
        assertNotNull("Reference table for inventory_item should exist", refTable);

        List<History> historyList = historyService.getHistoryByRefIdAndRefTableId(itemId.toString(), refTable.getId());

        assertFalse("History should be created for item insert", historyList.isEmpty());
        History insertHistory = historyList.get(0);

        assertEquals("Should be INSERT activity", "I", insertHistory.getActivity());
        assertEquals("Reference ID should match item ID", itemId.toString(), insertHistory.getReferenceId());
        assertEquals("Should track user ID", TEST_USER_ID, insertHistory.getSysUserId());
        assertNotNull("Timestamp should be set", insertHistory.getTimestamp());
    }

    @Test
    public void testInventoryItem_Update_CreatesHistoryEntry() {
        // Given: An existing inventory item
        InventoryItem item = inventoryItemService.get(2000L);
        assertNotNull("Test item should exist", item);

        int initialHistoryCount = getHistoryCount("INVENTORY_ITEM", "2000");

        // When: Item is updated
        item.setDescription("UPDATED - Audit trail test description");
        item.setLowStockThreshold(25);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // Then: New history entry should be created
        int updatedHistoryCount = getHistoryCount("INVENTORY_ITEM", "2000");
        assertTrue("History count should increase after update", updatedHistoryCount > initialHistoryCount);

        List<History> historyList = getHistoryList("INVENTORY_ITEM", "2000");
        History updateHistory = historyList.get(0); // Most recent

        assertEquals("Should be UPDATE activity", "U", updateHistory.getActivity());
        assertEquals("Should track user ID", TEST_USER_ID, updateHistory.getSysUserId());

        // Verify the changes are captured in XML format
        String changesXml = new String(updateHistory.getChanges());
        assertNotNull("Changes XML should not be null", changesXml);
        assertTrue("Should capture description change",
                changesXml.contains("description") || changesXml.contains("lowStockThreshold"));
    }

    // ============================================================================
    // INVENTORY LOT AUDIT TRAIL TESTS
    // ============================================================================

    @Test
    public void testInventoryLot_Insert_CreatesHistoryEntry() {
        // Given: A new inventory lot
        InventoryItem item = inventoryItemService.get(2000L);
        InventoryStorageLocation location = storageLocationService.get(2000L);

        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setStorageLocation(location);
        lot.setLotNumber("AUDIT-TEST-LOT-" + System.currentTimeMillis());
        lot.setExpirationDate(Timestamp.from(Instant.now().plus(365, ChronoUnit.DAYS)));
        lot.setReceiptDate(Timestamp.from(Instant.now()));
        lot.setInitialQuantity(200.0);
        lot.setCurrentQuantity(200.0);
        lot.setQcStatus(QCStatus.PENDING);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setFhirUuid(UUID.randomUUID());
        lot.setSysUserId(TEST_USER_ID);

        // When: Lot is inserted
        Long lotId = inventoryLotService.insert(lot);
        assertNotNull("Lot should be inserted", lotId);

        // Then: History entry should be created
        List<History> historyList = getHistoryList("INVENTORY_LOT", lotId.toString());
        assertFalse("History should be created for lot insert", historyList.isEmpty());

        History insertHistory = historyList.get(0);
        assertEquals("Should be INSERT activity", "I", insertHistory.getActivity());
        assertEquals("Reference ID should match lot ID", lotId.toString(), insertHistory.getReferenceId());
        assertEquals("Should track user ID", TEST_USER_ID, insertHistory.getSysUserId());
    }

    @Test
    public void testInventoryLot_Update_CreatesHistoryEntry() {
        // Given: An existing inventory lot
        InventoryLot lot = inventoryLotService.get(2000L);
        assertNotNull("Test lot should exist", lot);

        int initialHistoryCount = getHistoryCount("INVENTORY_LOT", "2000");

        // When: Lot is updated (quantity adjustment and status change)
        lot.setCurrentQuantity(75.0);
        lot.setStatus(LotStatus.IN_USE);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // Then: New history entry should be created
        int updatedHistoryCount = getHistoryCount("INVENTORY_LOT", "2000");
        assertTrue("History count should increase after update", updatedHistoryCount > initialHistoryCount);

        List<History> historyList = getHistoryList("INVENTORY_LOT", "2000");
        History updateHistory = historyList.get(0); // Most recent

        assertEquals("Should be UPDATE activity", "U", updateHistory.getActivity());
        assertEquals("Should track user ID", TEST_USER_ID, updateHistory.getSysUserId());

        // Verify the changes are captured
        String changesXml = new String(updateHistory.getChanges());
        assertNotNull("Changes XML should not be null", changesXml);
    }

    @Test
    public void testInventoryLot_StatusChange_CreatesAuditTrail() {
        // Given: An active lot
        InventoryLot lot = inventoryLotService.get(2001L);
        assertEquals("Lot should initially be IN_USE", LotStatus.IN_USE, lot.getStatus());

        int initialHistoryCount = getHistoryCount("INVENTORY_LOT", "2001");

        // When: Lot status is changed to DISPOSED
        lot.setStatus(LotStatus.DISPOSED);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // Then: History should capture the status change
        int updatedHistoryCount = getHistoryCount("INVENTORY_LOT", "2001");
        assertTrue("Status change should create audit trail", updatedHistoryCount > initialHistoryCount);

        List<History> historyList = getHistoryList("INVENTORY_LOT", "2001");
        History statusChangeHistory = historyList.get(0);

        String changesXml = new String(statusChangeHistory.getChanges());
        assertTrue("Should capture status field change", changesXml.contains("status"));
    }

    // ============================================================================
    // INVENTORY USAGE AUDIT TRAIL TESTS
    // ============================================================================

    @Test
    public void testInventoryUsage_Insert_CreatesHistoryEntry() {
        // Given: A new inventory usage record
        InventoryItem item = inventoryItemService.get(2000L);
        InventoryLot lot = inventoryLotService.get(2000L);

        InventoryUsage usage = new InventoryUsage();
        usage.setInventoryItem(item);
        usage.setLot(lot);
        usage.setQuantityUsed(5.0);
        usage.setUsageDate(Timestamp.from(Instant.now()));
        usage.setPerformedByUser(Integer.parseInt(TEST_USER_ID));
        usage.setSysUserId(TEST_USER_ID);

        // When: Usage is recorded
        Long usageId = inventoryUsageService.insert(usage);
        assertNotNull("Usage should be inserted", usageId);

        // Then: History entry should be created (if usage tracking is enabled)
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_USAGE");
        if (refTable != null && "Y".equals(refTable.getKeepHistory())) {
            List<History> historyList = historyService.getHistoryByRefIdAndRefTableId(usageId.toString(),
                    refTable.getId());

            assertFalse("History should be created for usage insert", historyList.isEmpty());
            History insertHistory = historyList.get(0);

            assertEquals("Should be INSERT activity", "I", insertHistory.getActivity());
            assertEquals("Should track user ID", TEST_USER_ID, insertHistory.getSysUserId());
        }
    }

    // ============================================================================
    // AUDIT TRAIL DATA TRANSFORMATION TESTS (for UI consumption)
    // ============================================================================

    @Test
    public void testHistoryTransformation_ParsesXMLChanges() {
        // Given: An item with update history
        InventoryItem item = inventoryItemService.get(2000L);
        item.setDescription("Changed for transformation test");
        item.setLowStockThreshold(15);
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // When: We retrieve and parse the history
        List<History> historyList = getHistoryList("INVENTORY_ITEM", "2000");
        History history = historyList.get(0);

        // Then: We should be able to parse the XML changes
        assertNotNull("History should exist", history);
        assertNotNull("Changes should exist", history.getChanges());

        String changesXml = new String(history.getChanges());
        assertTrue("Changes should be in XML format", changesXml.contains("<field"));

        // Verify we can extract field information
        Map<String, Map<String, String>> parsedChanges = parseXmlChanges(changesXml);
        assertNotNull("Should be able to parse changes", parsedChanges);
    }

    @Test
    public void testHistoryTransformation_IncludesTimestampAndUser() {
        // Given: A lot with history
        InventoryLot lot = inventoryLotService.get(2000L);
        Timestamp beforeUpdate = Timestamp.from(Instant.now());

        lot.setCurrentQuantity(80.0);
        lot.setSysUserId(TEST_USER_ID);
        inventoryLotService.update(lot);

        // When: We retrieve the history
        List<History> historyList = getHistoryList("INVENTORY_LOT", "2000");
        History history = historyList.get(0);

        // Then: Timestamp and user information should be present
        assertNotNull("Timestamp should be set", history.getTimestamp());
        assertTrue("Timestamp should be recent",
                history.getTimestamp().after(beforeUpdate) || history.getTimestamp().equals(beforeUpdate));

        assertEquals("User ID should be tracked", TEST_USER_ID, history.getSysUserId());
    }

    @Test
    public void testHistoryTransformation_SortsDescendingByTimestamp() {
        // Given: Multiple updates to an item
        InventoryItem item = inventoryItemService.get(2000L);

        for (int i = 0; i < 3; i++) {
            item.setDescription("Update iteration " + i);
            item.setSysUserId(TEST_USER_ID);
            inventoryItemService.update(item);

            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // When: We retrieve the history
        List<History> historyList = getHistoryList("INVENTORY_ITEM", "2000");

        // Then: History should be sorted by timestamp descending (newest first)
        assertTrue("Should have multiple history entries", historyList.size() >= 3);

        for (int i = 1; i < historyList.size(); i++) {
            Timestamp current = historyList.get(i - 1).getTimestamp();
            Timestamp previous = historyList.get(i).getTimestamp();
            assertTrue("Should be sorted descending by timestamp", current.compareTo(previous) >= 0);
        }
    }

    @Test
    public void testHistoryTracking_CapturesMultipleFieldChanges() {
        // Given: An item with multiple fields changed
        InventoryItem item = inventoryItemService.get(2000L);

        String originalDescription = item.getDescription();
        Integer originalThreshold = item.getLowStockThreshold();

        // When: Multiple fields are updated in single transaction
        item.setDescription("Multi-field change test");
        item.setLowStockThreshold(30);
        item.setManufacturer("New Manufacturer Inc.");
        item.setSysUserId(TEST_USER_ID);
        inventoryItemService.update(item);

        // Then: History should capture all changes
        List<History> historyList = getHistoryList("INVENTORY_ITEM", "2000");
        History history = historyList.get(0);

        String changesXml = new String(history.getChanges());
        Map<String, Map<String, String>> parsedChanges = parseXmlChanges(changesXml);

        assertTrue("Should capture multiple field changes", parsedChanges.size() >= 2);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private InventoryItem createTestInventoryItem(String name) {
        InventoryItem item = new InventoryItem();
        item.setName(name);
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setIsActive("Y");
        item.setFhirUuid(UUID.randomUUID());
        return item;
    }

    private List<History> getHistoryList(String tableName, String referenceId) {
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName(tableName);
        assertNotNull("Reference table should exist: " + tableName, refTable);
        return historyService.getHistoryByRefIdAndRefTableId(referenceId, refTable.getId());
    }

    private int getHistoryCount(String tableName, String referenceId) {
        return getHistoryList(tableName, referenceId).size();
    }

    /**
     * Simple XML parser for testing - matches the logic in
     * InventoryAuditLogRestController
     */
    private Map<String, Map<String, String>> parseXmlChanges(String xml) {
        Map<String, Map<String, String>> changes = new java.util.HashMap<>();
        if (xml == null || xml.trim().isEmpty()) {
            return changes;
        }

        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder
                    .parse(new org.xml.sax.InputSource(new java.io.StringReader("<changes>" + xml + "</changes>")));

            org.w3c.dom.NodeList fieldNodes = doc.getElementsByTagName("field");
            for (int i = 0; i < fieldNodes.getLength(); i++) {
                org.w3c.dom.Element fieldElement = (org.w3c.dom.Element) fieldNodes.item(i);
                String fieldName = fieldElement.getAttribute("name");

                Map<String, String> fieldChange = new java.util.HashMap<>();

                org.w3c.dom.NodeList oldNodes = fieldElement.getElementsByTagName("old");
                if (oldNodes.getLength() > 0) {
                    fieldChange.put("old", oldNodes.item(0).getTextContent());
                }

                org.w3c.dom.NodeList newNodes = fieldElement.getElementsByTagName("new");
                if (newNodes.getLength() > 0) {
                    fieldChange.put("new", newNodes.item(0).getTextContent());
                }

                changes.put(fieldName, fieldChange);
            }
        } catch (Exception e) {
            fail("Failed to parse XML changes: " + e.getMessage());
        }

        return changes;
    }
}
