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
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryOrderCycleService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
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
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(InventoryProjectionCalculator.WINDOW_DAYS - 1L);

        List<InventoryItem> items = inventoryItemService.getAllActive();
        Map<Long, Double> usableByItem = inventoryLotDAO.getAvailableQuantityByItem();
        Map<Long, List<InventoryUsage>> usageByItem = usageByItem(windowStart, today);
        Map<Long, List<Integer>> cycleDaysByItem = cycleDaysByItem(today);

        List<InventoryProjection> board = new ArrayList<>(items.size());
        for (InventoryItem item : items) {
            List<InventoryUsage> usage = usageByItem.getOrDefault(item.getId(), List.of());

            InventoryProjection row = InventoryProjectionCalculator.project(
                    usableByItem.getOrDefault(item.getId(), 0.0), dailyUse(usage, windowStart),
                    item.getLowStockThreshold(),
                    InventoryProjectionCalculator.resolveLeadTime(item.getLeadTimeDays(),
                            InventoryProjectionCalculator
                                    .observedLeadTime(cycleDaysByItem.getOrDefault(item.getId(), List.of()))),
                    latestUsageDate(usage), today);

            row.setItemId(item.getId());
            row.setCode(item.getCode());
            row.setName(item.getName());
            row.setItemType(item.getItemType() == null ? null : item.getItemType().name());
            row.setUnits(item.getUnits());
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

    private Map<Long, List<InventoryUsage>> usageByItem(LocalDate windowStart, LocalDate today) {
        Timestamp from = Timestamp.valueOf(windowStart.atStartOfDay());
        Timestamp to = Timestamp.valueOf(today.plusDays(1).atStartOfDay());

        Map<Long, List<InventoryUsage>> byItem = new HashMap<>();
        for (InventoryUsage usage : inventoryUsageService.getByDateRange(from, to)) {
            if (usage.getInventoryItem() != null) {
                byItem.computeIfAbsent(usage.getInventoryItem().getId(), key -> new ArrayList<>()).add(usage);
            }
        }
        return byItem;
    }

    private double[] dailyUse(List<InventoryUsage> usage, LocalDate windowStart) {
        double[] daily = new double[InventoryProjectionCalculator.WINDOW_DAYS];
        for (InventoryUsage record : usage) {
            if (record.getUsageDate() == null || record.getQuantityUsed() == null) {
                continue;
            }
            long offset = ChronoUnit.DAYS.between(windowStart, toLocalDate(record));
            if (offset >= 0 && offset < daily.length) {
                daily[(int) offset] += record.getQuantityUsed();
            }
        }
        return daily;
    }

    private LocalDate latestUsageDate(List<InventoryUsage> usage) {
        return usage.stream().filter(record -> record.getUsageDate() != null).map(this::toLocalDate)
                .max(Comparator.naturalOrder()).orElse(null);
    }

    private LocalDate toLocalDate(InventoryUsage usage) {
        return usage.getUsageDate().toLocalDateTime().toLocalDate();
    }
}
