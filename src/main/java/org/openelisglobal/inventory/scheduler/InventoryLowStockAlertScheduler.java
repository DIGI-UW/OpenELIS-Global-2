package org.openelisglobal.inventory.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.inventory.projection.InventoryProjection;
import org.openelisglobal.inventory.projection.InventoryProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Puts inventory into the Alerts module: an item the board calls Reorder now
 * gets an {@code INVENTORY_LOW} alert, and one that recovers has it resolved.
 *
 * <p>
 * The trigger is the board's own status rather than a second definition of
 * "low". {@code getLowStockItems} exists and is tempting, but it compares
 * strictly below the threshold while the board uses at-or-below, so an item
 * sitting exactly on its threshold would be flagged by one and not the other —
 * and a user looking at a red row and no alert has no way to tell which is
 * right. One source, one answer.
 *
 * <p>
 * The quantity goes in {@code contextData}, never in the message. A repeat
 * raise reuses the open row and only rewrites the message when severity rises,
 * so a number in the message would freeze at whatever it was the first time
 * while the stock kept falling.
 *
 * <p>
 * Severity is WARNING even though the board calls this urgent. CRITICAL alerts
 * cannot be acknowledged through the dashboard without a comment, and supplying
 * one resolves them rather than acknowledging them — which would make the next
 * pass raise the whole set again. WARNING is also the honest reading: running
 * out of stock is a warning, not a patient-safety event.
 */
@Service
public class InventoryLowStockAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLowStockAlertScheduler.class);

    /**
     * The entity class name, following the convention the other producers use
     * ("Freezer", "Sample", "Referral"). No inventory alert has ever been written,
     * so this string is the convention from here on.
     */
    static final String ENTITY_TYPE = "InventoryItem";

    private static final long FIFTEEN_MINUTES = 900000L;

    /**
     * Why an alert was closed. The two are not the same event, and the note is what
     * a reader sees months later.
     */
    private static final String RECOVERED = "Stock is back above the reorder threshold";

    private static final String OFF_THE_BOARD = "The item is no longer on the stock board";

    @Autowired
    private InventoryProjectionService inventoryProjectionService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private UserContextHolder userContextHolder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = FIFTEEN_MINUTES)
    public void raiseAndClearLowStockAlerts() {
        try {
            sweep();
        } catch (RuntimeException e) {
            // A scheduled task that throws is not retried and its next run is not
            // scheduled, so a bad row must not take the whole sweep down with it.
            logger.error("Inventory low-stock alert sweep failed", e);
        }
    }

    /**
     * Package-private so a test can run one pass without waiting on the scheduler,
     * which never fires in the test context anyway.
     */
    void sweep() {
        List<InventoryProjection> board = inventoryProjectionService.getBoard();

        Set<Long> low = new HashSet<>();
        Set<Long> onBoard = new HashSet<>();
        for (InventoryProjection row : board) {
            if (row.getItemId() == null) {
                continue;
            }
            onBoard.add(row.getItemId());
            if (row.getStatus() == InventoryProjection.BoardStatus.REORDER_NOW) {
                low.add(row.getItemId());
                raise(row);
            }
        }

        clearRecovered(low, onBoard);
    }

    private void raise(InventoryProjection row) {
        alertService.createAlert(AlertType.INVENTORY_LOW, ENTITY_TYPE, row.getItemId(), AlertSeverity.WARNING,
                messageFor(row), contextFor(row));
    }

    /**
     * Resolves the alert on every item that carries one and is no longer low.
     *
     * <p>
     * One query for the outstanding inventory alerts rather than one per item: the
     * template this follows asks the alert service per entity inside its loop,
     * which is a round trip per row of the catalogue every time it runs. OPEN and
     * ACKNOWLEDGED are the two states a repeat raise reuses, so they are the two a
     * recovery has to clear, and asking for exactly those keeps the sweep off every
     * alert this module has ever resolved.
     *
     * @param stillLow items the board reports as needing a reorder now
     * @param onBoard  every item the board still carries. An item that has left it
     *                 has not recovered: it was deactivated or deleted. Treating
     *                 its absence as a recovery closed the alert with a note
     *                 asserting stock had come back, which nobody had seen happen.
     */
    private void clearRecovered(Set<Long> stillLow, Set<Long> onBoard) {
        for (Alert alert : alertService.getOutstandingAlerts(ENTITY_TYPE, AlertType.INVENTORY_LOW)) {
            Long itemId = alert.getAlertEntityId();
            if (itemId == null || stillLow.contains(itemId)) {
                continue;
            }
            alertService.resolveAlert(alert.getId(), resolvingUserId(),
                    onBoard.contains(itemId) ? RECOVERED : OFF_THE_BOARD);
        }
    }

    /**
     * The daemon user, not a hardcoded id. The scheduler template this follows
     * passes 1 — the admin account the daemon user exists to stop standing in for.
     */
    private Integer resolvingUserId() {
        return Integer.valueOf(userContextHolder.getDaemonSysUserId());
    }

    /**
     * Deliberately carries no figure — not the quantity, not the order-by date. A
     * repeat raise reuses the open row and rewrites the message only when severity
     * rises, so anything numeric here is frozen at the moment the alert was first
     * raised and goes on asserting it while the stock keeps falling. A message with
     * no number in it cannot become untrue.
     */
    private String messageFor(InventoryProjection row) {
        return (row.getName() == null ? "Inventory item" : row.getName()) + " is at or below its reorder threshold";
    }

    /**
     * The figures, as they stood when the alert was raised. A repeat raise leaves
     * these untouched too, so this is a snapshot of the moment rather than a live
     * reading — which is fine for context and would not be fine in the message.
     */
    private String contextFor(InventoryProjection row) {
        Map<String, Object> context = new HashMap<>();
        context.put("itemId", row.getItemId());
        context.put("code", row.getCode());
        context.put("onHand", row.getOnHand());
        context.put("lowStockThreshold", row.getLowStockThreshold());
        context.put("units", row.getUnits());
        context.put("orderByDate", row.getOrderByDate() == null ? null : row.getOrderByDate().toString());
        context.put("runOutEarly", row.getRunOutEarly() == null ? null : row.getRunOutEarly().toString());
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            logger.warn("Could not serialise alert context for item {}", row.getItemId(), e);
            return "{}";
        }
    }
}
