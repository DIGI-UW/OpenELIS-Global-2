package org.openelisglobal.inventory.projection;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryProjectionServiceImpl implements InventoryProjectionService {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    /**
     * Reads the whole board in three queries rather than three per item: every
     * active item, every lot, and the window's usage, each fetched once and grouped
     * in memory. A facility holds a few hundred items and a month of usage, so the
     * grouping is cheap and the query count stays flat as the catalog grows.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InventoryProjection> getBoard() {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(InventoryProjectionCalculator.WINDOW_DAYS - 1L);

        List<InventoryItem> items = inventoryItemService.getAllActive();
        Map<Long, Double> usableByItem = usableQuantityByItem();
        Map<Long, List<InventoryUsage>> usageByItem = usageByItem(windowStart, today);

        List<InventoryProjection> board = new ArrayList<>(items.size());
        for (InventoryItem item : items) {
            List<InventoryUsage> usage = usageByItem.getOrDefault(item.getId(), List.of());

            InventoryProjection row = InventoryProjectionCalculator.project(
                    usableByItem.getOrDefault(item.getId(), 0.0), dailyUse(usage, windowStart),
                    item.getLowStockThreshold(),
                    // Observed lead time needs order-to-receipt history, which arrives with
                    // mark-as-ordered. Until then the tier resolves to set or default.
                    InventoryProjectionCalculator.resolveLeadTime(item.getLeadTimeDays(), null), latestUsageDate(usage),
                    today);

            row.setItemId(item.getId());
            row.setCode(item.getCode());
            row.setName(item.getName());
            row.setItemType(item.getItemType() == null ? null : item.getItemType().name());
            row.setUnits(item.getUnits());
            board.add(row);
        }

        board.sort(Comparator.comparingInt((InventoryProjection row) -> row.getStatus().ordinal())
                .thenComparing(InventoryProjection::getRunOutEarly, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InventoryProjection::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        return board;
    }

    /**
     * On-hand is the stock a reorder decision can count on: expired, disposed and
     * quarantined lots are excluded, but a lot still awaiting QC is counted.
     *
     * <p>
     * This is deliberately wider than {@link InventoryLot#isAvailableForUse()},
     * which gates consumption and admits only QC-passed stock. Three things settle
     * it. A delivery awaiting QC is physically on the shelf, so Count mode would
     * otherwise report a discrepancy on every item with one outstanding. Telling a
     * lab to reorder stock that arrived yesterday is the wrong answer. And the same
     * rule already backs the low-stock list and the reports, so a narrower one here
     * would make two surfaces of this module disagree about the same item.
     *
     * <p>
     * The cost is that on-hand can exceed what consumption would release. The row
     * expansion is where that resolves: it shows each lot's QC status and marks
     * "use first" only on a lot that is usable now.
     */
    private Map<Long, Double> usableQuantityByItem() {
        Map<Long, Double> usable = new HashMap<>();
        for (InventoryLot lot : inventoryLotService.getAll()) {
            if (lot.countsAsAvailableStock() && lot.getInventoryItem() != null) {
                usable.merge(lot.getInventoryItem().getId(), lot.getCurrentQuantity(), Double::sum);
            }
        }
        return usable;
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

    /**
     * Spreads usage records over one slot per day of the window. Days with no usage
     * stay at zero on purpose: they are real evidence of how fast the item moves,
     * and dropping them would overstate the median.
     */
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
