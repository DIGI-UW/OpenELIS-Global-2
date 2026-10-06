package org.openelisglobal.inventory.projection;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.dao.InventoryUsageDAO.DailyUsage;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryOrderCycleService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryProjectionServiceImpl implements InventoryProjectionService {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryOrderCycleService inventoryOrderCycleService;

    @Autowired
    private InventoryLotDAO inventoryLotDAO;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryProjection> getBoard() {
        return getBoard(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryProjection> getBoard(boolean includeInactive) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(InventoryProjectionCalculator.WINDOW_DAYS - 1L);

        List<InventoryItem> items = includeInactive ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();
        Map<Long, Double> usableByItem = inventoryLotDAO.getAvailableQuantityByItem();
        UsageWindow usage = usageWindow(windowStart, today);
        Map<Long, List<Integer>> cycleDaysByItem = cycleDaysByItem(today);

        List<InventoryProjection> board = new ArrayList<>(items.size());
        for (InventoryItem item : items) {
            InventoryProjection row = InventoryProjectionCalculator.project(
                    usableByItem.getOrDefault(item.getId(), 0.0), usage.dailyUse().getOrDefault(item.getId(), NO_USE),
                    item.getLowStockThreshold(),
                    InventoryProjectionCalculator.resolveLeadTime(item.getLeadTimeDays(),
                            InventoryProjectionCalculator
                                    .observedLeadTime(cycleDaysByItem.getOrDefault(item.getId(), List.of()))),
                    usage.latestUsage().get(item.getId()), today);

            row.setItemId(item.getId());
            row.setCode(item.getCode());
            row.setName(item.getName());
            row.setTags(item.getTags() == null ? List.of() : new ArrayList<>(item.getTags()));
            row.setUnits(item.getUnits());
            row.setTrackLots(item.tracksLots());
            row.setUpc(item.getUpc());
            row.setLastCountedOn(
                    item.getLastCountedAt() == null ? null : item.getLastCountedAt().toLocalDateTime().toLocalDate());
            row.setActive(item.isActive());
            row.setOrderedOn(item.getOrderedAt() == null ? null : item.getOrderedAt().toLocalDateTime().toLocalDate());
            row.setOrderExpectedDate(item.getOrderExpectedDate());
            row.setOrderNote(item.getOrderNote());
            board.add(row);
        }

        board.sort(Comparator.comparingInt((InventoryProjection row) -> row.getStatus().ordinal())
                .thenComparing(InventoryProjection::getRunOutEarly, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InventoryProjection::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        return board;
    }

    private Map<Long, List<Integer>> cycleDaysByItem(LocalDate today) {
        Timestamp cutoff = Timestamp
                .valueOf(today.minusDays(InventoryProjectionCalculator.LEAD_TIME_HISTORY_DAYS).atStartOfDay());
        Map<Long, List<Integer>> byItem = new HashMap<>();
        for (InventoryOrderCycle cycle : inventoryOrderCycleService.getReceivedSince(cutoff)) {
            if (cycle.getInventoryItem() == null || cycle.getInventoryItem().getId() == null) {
                continue;
            }
            byItem.computeIfAbsent(cycle.getInventoryItem().getId(), key -> new ArrayList<>())
                    .add(cycle.getLeadTimeDays());
        }
        return byItem;
    }

    /**
     * One slot per day of the window, oldest first; days with no use stay at zero.
     */
    private record UsageWindow(Map<Long, double[]> dailyUse, Map<Long, LocalDate> latestUsage) {
    }

    private static final double[] NO_USE = new double[InventoryProjectionCalculator.WINDOW_DAYS];

    private UsageWindow usageWindow(LocalDate windowStart, LocalDate today) {
        Timestamp from = Timestamp.valueOf(windowStart.atStartOfDay());
        Timestamp to = Timestamp.valueOf(today.plusDays(1).atStartOfDay());

        Map<Long, double[]> dailyUse = new HashMap<>();
        Map<Long, LocalDate> latestUsage = new HashMap<>();
        for (DailyUsage total : inventoryUsageService.getDailyTotals(from, to)) {
            long offset = ChronoUnit.DAYS.between(windowStart, total.day());
            if (offset < 0 || offset >= InventoryProjectionCalculator.WINDOW_DAYS) {
                continue;
            }
            dailyUse.computeIfAbsent(total.itemId(),
                    key -> new double[InventoryProjectionCalculator.WINDOW_DAYS])[(int) offset] += total.quantity();
            latestUsage.merge(total.itemId(), total.day(), (left, right) -> left.isAfter(right) ? left : right);
        }
        return new UsageWindow(dailyUse, latestUsage);
    }
}
