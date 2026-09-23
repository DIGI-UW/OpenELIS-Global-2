package org.openelisglobal.inventory.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Inventory reaching the Alerts module, and leaving it again.
 *
 * <p>
 * The scheduler never fires in this context — {@code AppTestConfig} excludes
 * {@code SchedulerConfig}, so {@code @EnableScheduling} never applies — which
 * is what makes it testable: each case drives exactly one pass by hand and
 * asserts on what the pass left behind.
 */
public class InventoryLowStockAlertIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "ALERTTEST_";

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private InventoryLowStockAlertScheduler scheduler;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private AlertService alertService;

    private JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.alert WHERE alert_entity_type = ?",
                InventoryLowStockAlertScheduler.ENTITY_TYPE);
        jdbc.update("DELETE FROM clinlims.inventory_lot WHERE inventory_item_id IN"
                + " (SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item_tag WHERE item_id IN"
                + " (SELECT id FROM clinlims.inventory_item WHERE code LIKE ?)", CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    /** An item below its threshold, which is what the board calls Reorder now. */
    private Long lowStockItem(String suffix, double onHand, int threshold) {
        InventoryItem item = new InventoryItem();
        item.setCode(CODE_PREFIX + suffix);
        item.setName(CODE_PREFIX + suffix);
        item.setUnits("tests");
        item.setLowStockThreshold(threshold);
        item.setFhirUuid(UUID.randomUUID());
        item.setSysUserId("1");
        Long itemId = inventoryItemService.insert(item);

        if (onHand > 0) {
            InventoryLot lot = new InventoryLot();
            lot.setInventoryItem(inventoryItemService.get(itemId));
            lot.setLotNumber(CODE_PREFIX + suffix + "-LOT");
            lot.setFhirUuid(UUID.randomUUID());
            lot.setInitialQuantity(onHand);
            lot.setCurrentQuantity(onHand);
            lot.setStatus(LotStatus.ACTIVE);
            lot.setQcStatus(QCStatus.PASSED);
            lot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000));
            lot.setSysUserId("1");
            inventoryLotService.insert(lot);
        }
        return itemId;
    }

    private List<Alert> alertsFor(Long itemId) {
        return alertService.getAlertsByEntity(InventoryLowStockAlertScheduler.ENTITY_TYPE, itemId);
    }

    private void raiseStockAbove(Long itemId, double quantity) {
        jdbc.update("UPDATE clinlims.inventory_lot SET current_quantity = ? WHERE inventory_item_id = ?", quantity,
                itemId);
    }

    @Test
    public void anItemBelowItsThresholdRaisesOneAlert() {
        Long itemId = lowStockItem("LOW", 2, 10);

        scheduler.sweep();

        List<Alert> alerts = alertsFor(itemId);
        assertEquals("exactly one alert for the item", 1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals(AlertType.INVENTORY_LOW, alert.getAlertType());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(AlertSeverity.WARNING, alert.getSeverity());
        assertTrue("the message names the item: " + alert.getMessage(),
                alert.getMessage().contains(CODE_PREFIX + "LOW"));
    }

    @Test
    public void anItemWithEnoughStockRaisesNothing() {
        Long itemId = lowStockItem("HEALTHY", 80, 10);

        scheduler.sweep();

        assertTrue("a well-stocked item has no alert", alertsFor(itemId).isEmpty());
    }

    /**
     * The figures live in contextData, not the message, because a repeat raise
     * leaves the message alone and a number in it would freeze at the first value.
     */
    @Test
    public void theQuantityTravelsInContextDataRatherThanTheMessage() throws Exception {
        Long itemId = lowStockItem("CONTEXT", 3, 10);

        scheduler.sweep();

        Alert alert = alertsFor(itemId).get(0);
        assertNotNull("context data is written", alert.getContextData());
        JsonNode context = objectMapper.readTree(alert.getContextData());
        assertEquals(3.0, context.path("onHand").asDouble(), 0.0001);
        assertEquals(10, context.path("lowStockThreshold").asInt());
        assertEquals(itemId.longValue(), context.path("itemId").asLong());
    }

    /**
     * The message must make no numeric claim, because a repeat raise never rewrites
     * it. Asserting the figures reach contextData is not enough on its own: putting
     * them in both places passes that and still ships a message that goes stale the
     * moment stock moves.
     */
    @Test
    public void theMessageMakesNoNumericClaimThatCouldGoStale() {
        Long itemId = lowStockItem("STALE", 3, 10);
        scheduler.sweep();
        String whenRaised = alertsFor(itemId).get(0).getMessage();

        raiseStockAbove(itemId, 1);
        scheduler.sweep();

        String now = alertsFor(itemId).get(0).getMessage();
        assertEquals("a repeat raise leaves the message alone, so it had better not date", whenRaised, now);
        assertFalse("the message states a quantity it will never update: " + now, now.matches(".*\\d.*"));
    }

    /**
     * A second pass must reuse the row rather than pile up a new one every quarter
     * of an hour. This is the whole reason the dedupe exists.
     */
    @Test
    public void aSecondPassReusesTheSameAlertAndBumpsItsDuplicateCount() {
        Long itemId = lowStockItem("REPEAT", 2, 10);

        scheduler.sweep();
        scheduler.sweep();
        scheduler.sweep();

        List<Alert> alerts = alertsFor(itemId);
        assertEquals("still one row after three passes", 1, alerts.size());
        assertEquals("and it counted the repeats", Integer.valueOf(2), alerts.get(0).getDuplicateCount());
    }

    /**
     * Acknowledging is what a person does to say they have seen it. The next pass
     * must not undo that by reopening the alert, or acknowledging would be
     * pointless on anything that stays low.
     */
    @Test
    public void anAcknowledgedAlertIsNotReopenedByTheNextPass() {
        Long itemId = lowStockItem("ACKED", 2, 10);
        scheduler.sweep();
        Alert raised = alertsFor(itemId).get(0);
        alertService.acknowledgeAlert(raised.getId(), 1, "ordering next week");

        scheduler.sweep();

        List<Alert> alerts = alertsFor(itemId);
        assertEquals("no second row", 1, alerts.size());
        assertEquals("and it stays acknowledged", AlertStatus.ACKNOWLEDGED, alerts.get(0).getStatus());
    }

    @Test
    public void anItemThatRecoversHasItsAlertResolved() {
        Long itemId = lowStockItem("RECOVERS", 2, 10);
        scheduler.sweep();
        assertEquals(AlertStatus.OPEN, alertsFor(itemId).get(0).getStatus());

        raiseStockAbove(itemId, 90);
        scheduler.sweep();

        List<Alert> alerts = alertsFor(itemId);
        assertEquals("the same row, resolved rather than a new one", 1, alerts.size());
        assertEquals(AlertStatus.RESOLVED, alerts.get(0).getStatus());
        assertEquals("Stock is back above the reorder threshold", alerts.get(0).getResolutionNotes());
    }

    /**
     * Deactivating a short item takes it off the board. That is not a recovery:
     * nobody restocked anything, and closing the alert with a note saying stock had
     * come back put a claim in the audit trail that never happened.
     */
    @Test
    public void anItemThatLeavesTheBoardIsNotRecordedAsHavingRecovered() {
        Long itemId = lowStockItem("DEACTIVATED", 2, 10);
        scheduler.sweep();
        assertEquals(AlertStatus.OPEN, alertsFor(itemId).get(0).getStatus());

        jdbc.update("UPDATE clinlims.inventory_item SET is_active = 'N' WHERE id = ?", itemId);
        scheduler.sweep();

        Alert alert = alertsFor(itemId).get(0);
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertEquals("The item is no longer on the stock board", alert.getResolutionNotes());
    }

    /**
     * Recovery has to clear an acknowledged alert too. Only OPEN and ACKNOWLEDGED
     * block a re-raise, so leaving an acknowledged one behind would mean the item
     * never alerts again the next time it runs low.
     */
    @Test
    public void recoveryAlsoClearsAnAlertSomebodyHadAcknowledged() {
        Long itemId = lowStockItem("ACKED_RECOVERS", 2, 10);
        scheduler.sweep();
        alertService.acknowledgeAlert(alertsFor(itemId).get(0).getId(), 1, "seen");

        raiseStockAbove(itemId, 90);
        scheduler.sweep();

        assertEquals(AlertStatus.RESOLVED, alertsFor(itemId).get(0).getStatus());
    }

    /**
     * And once it has been cleared, running low again has to alert afresh —
     * otherwise a resolved alert would silence the item permanently.
     */
    @Test
    public void anItemThatRunsLowAgainAfterRecoveringAlertsAgain() {
        Long itemId = lowStockItem("AGAIN", 2, 10);
        scheduler.sweep();
        raiseStockAbove(itemId, 90);
        scheduler.sweep();

        raiseStockAbove(itemId, 1);
        scheduler.sweep();

        List<Alert> alerts = alertsFor(itemId);
        assertEquals("a fresh row, because the first was resolved", 2, alerts.size());
        assertTrue("and one of them is open again", alerts.stream().anyMatch(a -> a.getStatus() == AlertStatus.OPEN));
    }

    /**
     * The sweep must not touch alerts belonging to other modules. They share one
     * table, and the resolve pass reads every row of it.
     */
    @Test
    public void theSweepLeavesOtherModulesAlertsAlone() {
        Alert freezerAlert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 999999L,
                AlertSeverity.WARNING, "ALERTTEST freezer alert", "{}");

        scheduler.sweep();

        Alert after = alertService.get(freezerAlert.getId());
        assertEquals("a freezer alert is none of this sweep's business", AlertStatus.OPEN, after.getStatus());
        jdbc.update("DELETE FROM clinlims.alert WHERE id = ?", freezerAlert.getId());
    }
}
