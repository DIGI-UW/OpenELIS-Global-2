package org.openelisglobal.inventory.report;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Headers stay in English: a download is not rendered UI, so react-intl misses
 * it.
 */
@Service
public class InventoryReportServiceImpl implements InventoryReportService {

    private static final String UNASSIGNED_LOCATION = "Unassigned";
    private static final String MULTIPLE_LOCATIONS = "Multiple locations";

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SystemUserService systemUserService;

    private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private final ThreadLocal<SimpleDateFormat> dateTimeFormat = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm"));

    @Override
    @Transactional(readOnly = true)
    public ReportTable generateReport(InventoryReportRequest request) {
        switch (request.getReportType()) {
        case "STOCK_LEVELS":
            return buildStockLevelsReport(request);
        case "EXPIRATION_FORECAST":
            return buildExpirationForecastReport(request);
        case "USAGE_TRENDS":
            requireDateRange(request);
            return buildUsageTrendsReport(request);
        case "LOT_TRACEABILITY":
            return buildLotTraceabilityReport(request);
        case "LOW_STOCK":
            return buildLowStockReport(request);
        case "TRANSACTION_HISTORY":
            requireDateRange(request);
            return buildTransactionHistoryReport(request);
        default:
            throw new LocalizedValidationException("reports.error.unknownReportType",
                    "Unknown report type: " + request.getReportType());
        }
    }

    private void requireDateRange(InventoryReportRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new LocalizedValidationException("reports.error.dateRangeRequired",
                    "This report type requires a start and end date");
        }
    }

    private ReportTable buildStockLevelsReport(InventoryReportRequest request) {
        List<InventoryItem> items = request.isIncludeInactive() ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();

        Map<Long, List<InventoryLot>> lotsByItemId = loadLotsByItemId(items);
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lotsByItemId);
        Map<Long, String> locationByItemId = items.stream().collect(Collectors.toMap(InventoryItem::getId,
                item -> summarizeLocation(lotsByItemId.getOrDefault(item.getId(), List.of()), locationsByLotId)));

        ReportTable table = new ReportTable("Stock Levels", List.of("Item Code", "Item Name", "Type", "Category",
                "Location", "Available Quantity", "Total Quantity", "Units", "Status"), Set.of(5, 6));

        List<InventoryItem> sorted = sortItems(items, request, locationByItemId);
        double totalSum = 0;
        double availableSum = 0;
        for (InventoryItem item : sorted) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double totalQuantity = totalQuantity(lots);
            double availableQuantity = availableQuantity(lots);
            totalSum += totalQuantity;
            availableSum += availableQuantity;
            table.addRow(List.of(item.getCode(), item.getName(),
                    nullToEmpty(item.getItemType() == null ? null : item.getItemType().name()),
                    nullToEmpty(item.getCategory()), locationByItemId.get(item.getId()),
                    formatNumber(availableQuantity), formatNumber(totalQuantity), item.getUnits(),
                    item.isActive() ? "Active" : "Inactive"));
        }
        addQuantityTotalsRow(table, sorted.size(), 5, availableSum, 6, totalSum);
        return table;
    }

    /**
     * Judged on {@link #availableQuantity}, so dead stock cannot pad an item past
     * its reorder threshold and out of the report.
     */
    private ReportTable buildLowStockReport(InventoryReportRequest request) {
        List<InventoryItem> activeItems = inventoryItemService.getAllActive();
        Map<Long, List<InventoryLot>> lotsByItemId = loadLotsByItemId(activeItems);
        List<InventoryItem> items = activeItems.stream().filter(item -> item.getLowStockThreshold() != null)
                .filter(item -> availableQuantity(lotsByItemId.getOrDefault(item.getId(), List.of())) < item
                        .getLowStockThreshold())
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lotsByItemId);
        Map<Long, String> locationByItemId = items.stream().collect(Collectors.toMap(InventoryItem::getId,
                item -> summarizeLocation(lotsByItemId.getOrDefault(item.getId(), List.of()), locationsByLotId)));
        items = sortItems(items, request, locationByItemId);

        ReportTable table = new ReportTable("Low Stock Items", List.of("Item Code", "Item Name", "Type", "Category",
                "Location", "Available Quantity", "Total Quantity", "Low Stock Threshold", "Units"), Set.of(5, 6, 7));
        double availableSum = 0;
        double totalSum = 0;
        for (InventoryItem item : items) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double availableQuantity = availableQuantity(lots);
            double totalQuantity = totalQuantity(lots);
            availableSum += availableQuantity;
            totalSum += totalQuantity;
            table.addRow(List.of(item.getCode(), item.getName(),
                    nullToEmpty(item.getItemType() == null ? null : item.getItemType().name()),
                    nullToEmpty(item.getCategory()), locationByItemId.get(item.getId()),
                    formatNumber(availableQuantity), formatNumber(totalQuantity),
                    item.getLowStockThreshold().toString(), item.getUnits()));
        }
        addQuantityTotalsRow(table, items.size(), 5, availableSum, 6, totalSum);
        return table;
    }

    private ReportTable buildExpirationForecastReport(InventoryReportRequest request) {
        List<InventoryItem> items = request.isIncludeInactive() ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();
        Map<Long, InventoryItem> itemsById = items.stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        List<InventoryLot> allLots = inventoryLotService.getAll().stream()
                .filter(lot -> lot.getInventoryItem() != null && itemsById.containsKey(lot.getInventoryItem().getId()))
                .filter(lot -> lot.getEffectiveExpirationDate() != null)
                .filter(lot -> request.isIncludeExpired() || !lot.isExpired())
                .filter(lot -> request.getStartDate() == null
                        || !lot.getEffectiveExpirationDate().before(request.getStartDate()))
                .filter(lot -> request.getEndDate() == null
                        || !lot.getEffectiveExpirationDate().after(request.getEndDate()))
                .collect(Collectors.toList());

        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(allLots);

        Comparator<InventoryLot> byExpiration = Comparator.comparing(InventoryLot::getEffectiveExpirationDate);
        Comparator<InventoryLot> comparator = byExpiration;
        if (request.isGroupByType()) {
            comparator = Comparator
                    .comparing(
                            (InventoryLot l) -> nullToEmpty(itemTypeName(itemsById.get(l.getInventoryItem().getId()))))
                    .thenComparing(byExpiration);
        } else if (request.isGroupByLocation()) {
            comparator = Comparator.comparing((InventoryLot l) -> resolveLotLocation(l, locationsByLotId))
                    .thenComparing(byExpiration);
        }
        allLots.sort(comparator);

        ReportTable table = new ReportTable(
                "Expiration Forecast", List.of("Item Code", "Item Name", "Type", "Lot Number", "Location",
                        "Expiration Date", "Days Until Expiration", "Urgency", "Current Quantity", "Status"),
                Set.of(6, 8));
        long now = System.currentTimeMillis();
        for (InventoryLot lot : allLots) {
            InventoryItem item = itemsById.get(lot.getInventoryItem().getId());
            long daysUntil = Math.floorDiv(lot.getEffectiveExpirationDate().getTime() - now, 1000L * 60 * 60 * 24);
            table.addRow(List.of(item.getCode(), item.getName(),
                    nullToEmpty(item.getItemType() == null ? null : item.getItemType().name()), lot.getLotNumber(),
                    resolveLotLocation(lot, locationsByLotId), formatDate(lot.getEffectiveExpirationDate()),
                    Long.toString(daysUntil), expirationUrgency(daysUntil),
                    formatNumber(lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0),
                    lot.getStatus() != null ? lot.getStatus().name() : ""));
        }
        return table;
    }

    /**
     * At-a-glance triage bucket so the reader doesn't have to do date math per row.
     */
    private String expirationUrgency(long daysUntil) {
        if (daysUntil < 0) {
            return "EXPIRED";
        }
        if (daysUntil <= 7) {
            return "THIS_WEEK";
        }
        if (daysUntil <= 30) {
            return "THIS_MONTH";
        }
        return "LATER";
    }

    /**
     * One row per item, heaviest use first. The raw per-transaction log is
     * {@code TRANSACTION_HISTORY}'s job.
     */
    private ReportTable buildUsageTrendsReport(InventoryReportRequest request) {
        List<InventoryUsage> usages = inventoryUsageService.getByDateRange(request.getStartDate(),
                request.getEndDate());

        Map<Long, List<InventoryUsage>> usagesByItemId = usages.stream()
                .filter(usage -> usage.getInventoryItem() != null)
                .collect(Collectors.groupingBy(usage -> usage.getInventoryItem().getId()));

        ReportTable table = new ReportTable("Usage Trends", List.of("Item Code", "Item Name", "Type",
                "Total Quantity Used", "Usage Events", "Avg Quantity Per Use", "First Use", "Last Use"),
                Set.of(3, 4, 5));

        List<Map.Entry<Long, List<InventoryUsage>>> sortedByUsage = usagesByItemId.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<Long, List<InventoryUsage>> e) -> totalQuantityUsed(e.getValue()))
                        .reversed())
                .collect(Collectors.toList());

        double grandTotalUsed = 0;
        int totalEvents = 0;
        for (Map.Entry<Long, List<InventoryUsage>> entry : sortedByUsage) {
            List<InventoryUsage> itemUsages = entry.getValue();
            InventoryItem item = itemUsages.get(0).getInventoryItem();
            double totalUsed = totalQuantityUsed(itemUsages);
            int eventCount = itemUsages.size();
            Timestamp firstUse = itemUsages.stream().map(InventoryUsage::getUsageDate).min(Comparator.naturalOrder())
                    .orElse(null);
            Timestamp lastUse = itemUsages.stream().map(InventoryUsage::getUsageDate).max(Comparator.naturalOrder())
                    .orElse(null);
            grandTotalUsed += totalUsed;
            totalEvents += eventCount;

            table.addRow(List.of(item.getCode(), item.getName(),
                    nullToEmpty(item.getItemType() == null ? null : item.getItemType().name()), formatNumber(totalUsed),
                    Integer.toString(eventCount), formatNumber(eventCount == 0 ? 0.0 : totalUsed / eventCount),
                    formatDateTime(firstUse), formatDateTime(lastUse)));
        }
        List<String> totalsRow = new java.util.ArrayList<>(
                java.util.Collections.nCopies(table.getHeaders().size(), ""));
        totalsRow.set(0, "TOTAL (" + sortedByUsage.size() + " items)");
        totalsRow.set(3, formatNumber(grandTotalUsed));
        totalsRow.set(4, Integer.toString(totalEvents));
        table.addRow(totalsRow);
        return table;
    }

    private double totalQuantityUsed(List<InventoryUsage> usages) {
        return usages.stream().mapToDouble(u -> u.getQuantityUsed() != null ? u.getQuantityUsed() : 0.0).sum();
    }

    private ReportTable buildTransactionHistoryReport(InventoryReportRequest request) {
        List<InventoryTransaction> transactions = inventoryTransactionService.getByDateRange(request.getStartDate(),
                request.getEndDate());
        Map<Integer, String> userNameCache = new HashMap<>();

        ReportTable table = new ReportTable("Transaction History", List.of("Date", "Item Code", "Item Name",
                "Lot Number", "Transaction Type", "Quantity Change", "Quantity After", "Performed By"), Set.of(5, 6));
        for (InventoryTransaction transaction : transactions) {
            InventoryLot lot = transaction.getLot();
            InventoryItem item = lot != null ? lot.getInventoryItem() : null;
            table.addRow(List.of(formatDateTime(transaction.getTransactionDate()), item != null ? item.getCode() : "",
                    item != null ? item.getName() : "", lot != null ? lot.getLotNumber() : "",
                    transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "",
                    formatNumber(transaction.getQuantityChange() != null ? transaction.getQuantityChange() : 0.0),
                    formatNumber(transaction.getQuantityAfter() != null ? transaction.getQuantityAfter() : 0.0),
                    resolveUserName(transaction.getPerformedByUser(), userNameCache)));
        }
        return table;
    }

    private ReportTable buildLotTraceabilityReport(InventoryReportRequest request) {
        List<InventoryLot> lots = inventoryLotService.getAll().stream().filter(lot -> lot.getInventoryItem() != null)
                .filter(lot -> request.isIncludeInactive() || lot.getInventoryItem().isActive())
                .sorted(Comparator.comparing((InventoryLot l) -> l.getInventoryItem().getName(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(InventoryLot::getLotNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lots);

        ReportTable table = new ReportTable("Lot Traceability",
                List.of("Item Code", "Item Name", "Lot Number", "Receipt Date", "Expiration Date", "Initial Quantity",
                        "Current Quantity", "Status", "QC Status", "Location"),
                Set.of(5, 6));
        for (InventoryLot lot : lots) {
            InventoryItem item = lot.getInventoryItem();
            table.addRow(List.of(item.getCode(), item.getName(), lot.getLotNumber(), formatDate(lot.getReceiptDate()),
                    formatDate(lot.getExpirationDate()),
                    formatNumber(lot.getInitialQuantity() != null ? lot.getInitialQuantity() : 0.0),
                    formatNumber(lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0),
                    lot.getStatus() != null ? lot.getStatus().name() : "",
                    lot.getQcStatus() != null ? lot.getQcStatus().name() : "",
                    resolveLotLocation(lot, locationsByLotId)));
        }
        return table;
    }

    // --- shared helpers ---

    /** Raw physical sum across every lot, regardless of usability. */
    private double totalQuantity(List<InventoryLot> lots) {
        return lots.stream().mapToDouble(l -> l.getCurrentQuantity() != null ? l.getCurrentQuantity() : 0.0).sum();
    }

    /**
     * Sum of {@link InventoryLot#countsAsAvailableStock()} stock, the rule the
     * low-stock alert shares so tile and report cannot disagree.
     */
    private double availableQuantity(List<InventoryLot> lots) {
        return lots.stream().filter(InventoryLot::countsAsAvailableStock).mapToDouble(InventoryLot::getCurrentQuantity)
                .sum();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Appends a "TOTAL (N items)" row with two summed quantity columns filled in.
     */
    private void addQuantityTotalsRow(ReportTable table, int itemCount, int availableColumnIndex, double availableSum,
            int totalColumnIndex, double totalSum) {
        List<String> row = new java.util.ArrayList<>(java.util.Collections.nCopies(table.getHeaders().size(), ""));
        row.set(0, "TOTAL (" + itemCount + " items)");
        row.set(availableColumnIndex, formatNumber(availableSum));
        row.set(totalColumnIndex, formatNumber(totalSum));
        table.addRow(row);
    }

    /**
     * "Group by" clusters adjacent rows in the flat exported table: sort by type
     * and/or location ahead of name, rather than emitting section headers.
     */
    private List<InventoryItem> sortItems(List<InventoryItem> items, InventoryReportRequest request,
            Map<Long, String> locationByItemId) {
        Comparator<InventoryItem> comparator = Comparator.comparing(InventoryItem::getName,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (request.isGroupByLocation()) {
            comparator = Comparator.comparing((InventoryItem i) -> locationByItemId.getOrDefault(i.getId(), ""))
                    .thenComparing(comparator);
        }
        if (request.isGroupByType()) {
            comparator = Comparator.comparing((InventoryItem i) -> nullToEmpty(itemTypeName(i)))
                    .thenComparing(comparator);
        }
        return items.stream().sorted(comparator).collect(Collectors.toList());
    }

    private String itemTypeName(InventoryItem item) {
        return item == null || item.getItemType() == null ? null : item.getItemType().name();
    }

    /**
     * One query for every lot, grouped in memory, rather than a
     * {@code getByInventoryItemId} round trip per item.
     */
    private Map<Long, List<InventoryLot>> loadLotsByItemId(List<InventoryItem> items) {
        java.util.Set<Long> itemIds = items.stream().map(InventoryItem::getId).collect(Collectors.toSet());
        return inventoryLotService.getAll().stream()
                .filter(lot -> lot.getInventoryItem() != null && itemIds.contains(lot.getInventoryItem().getId()))
                .collect(Collectors.groupingBy(lot -> lot.getInventoryItem().getId()));
    }

    private Map<String, Map<String, Object>> loadLocationsByLotId(Map<Long, List<InventoryLot>> lotsByItemId) {
        List<InventoryLot> allLots = lotsByItemId.values().stream().flatMap(List::stream).collect(Collectors.toList());
        return loadLocationsByLotId(allLots);
    }

    private Map<String, Map<String, Object>> loadLocationsByLotId(List<InventoryLot> lots) {
        List<Long> lotIds = lots.stream().map(InventoryLot::getId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return sampleStorageService.getLocationsForInventoryLots(lotIds);
    }

    private String resolveLotLocation(InventoryLot lot, Map<String, Map<String, Object>> locationsByLotId) {
        if (lot.getId() == null) {
            return UNASSIGNED_LOCATION;
        }
        Map<String, Object> location = locationsByLotId.get(lot.getId().toString());
        if (location == null || location.get("hierarchicalPath") == null) {
            return UNASSIGNED_LOCATION;
        }
        String path = String.valueOf(location.get("hierarchicalPath"));
        return path.isEmpty() ? UNASSIGNED_LOCATION : path;
    }

    private String summarizeLocation(List<InventoryLot> lots, Map<String, Map<String, Object>> locationsByLotId) {
        java.util.Set<String> distinctLocations = lots.stream().map(lot -> resolveLotLocation(lot, locationsByLotId))
                .filter(loc -> !UNASSIGNED_LOCATION.equals(loc)).collect(Collectors.toSet());
        if (distinctLocations.isEmpty()) {
            return UNASSIGNED_LOCATION;
        }
        if (distinctLocations.size() == 1) {
            return distinctLocations.iterator().next();
        }
        return MULTIPLE_LOCATIONS;
    }

    private String resolveUserName(Integer userId, Map<Integer, String> cache) {
        if (userId == null) {
            return "";
        }
        return cache.computeIfAbsent(userId, id -> {
            try {
                SystemUser user = systemUserService.get(id.toString());
                if (user == null) {
                    return id.toString();
                }
                String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
                String last = user.getLastName() != null ? user.getLastName().trim() : "";
                String combined = (first + " " + last).trim();
                return combined.isEmpty() ? id.toString() : combined;
            } catch (Exception e) {
                return id.toString();
            }
        });
    }

    private String formatDate(Timestamp timestamp) {
        return timestamp != null ? dateFormat.get().format(timestamp) : "";
    }

    private String formatDateTime(Timestamp timestamp) {
        return timestamp != null ? dateTimeFormat.get().format(timestamp) : "";
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
