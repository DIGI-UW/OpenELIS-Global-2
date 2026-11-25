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

@Service
public class InventoryLowStockAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLowStockAlertScheduler.class);

    static final String ENTITY_TYPE = "InventoryItem";

    private static final long FIFTEEN_MINUTES = 900000L;

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
            logger.error("Inventory low-stock alert sweep failed", e);
        }
    }

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
        // Not CRITICAL: the dashboard cannot acknowledge one without resolving it
        alertService.createAlert(AlertType.INVENTORY_LOW, ENTITY_TYPE, row.getItemId(), AlertSeverity.WARNING,
                messageFor(row), contextFor(row));
    }

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

    private Integer resolvingUserId() {
        return Integer.valueOf(userContextHolder.getDaemonSysUserId());
    }

    /** No figures: a repeat raise keeps the first message, so they go stale. */
    private String messageFor(InventoryProjection row) {
        return (row.getName() == null ? "Inventory item" : row.getName()) + " needs reordering now";
    }

    /** A snapshot from the first raise, frozen like the message. */
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
