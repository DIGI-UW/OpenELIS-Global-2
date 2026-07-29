package org.openelisglobal.inventory.report;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Builds the tabular data behind each of the 6 report types
 * {@code InventoryReports.jsx} exposes. Every {@code build*Report} method
 * returns a plain {@link ReportTable}; {@link InventoryReportWriter} turns that
 * into the requested export format. Column headers are plain English — unlike
 * the frontend's react-intl catalog, these files are downloaded artifacts
 * rather than rendered UI, and localizing them would mean adding a parallel set
 * of entries to the legacy Java message-bundle system, which is out of scope
 * here.
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

        Map<String, List<InventoryLot>> lotsByItemId = loadLotsByItemId(items);
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lotsByItemId);
        Map<String, String> locationByItemId = items.stream().collect(Collectors.toMap(InventoryItem::getId,
                item -> summarizeLocation(lotsByItemId.getOrDefault(item.getId(), List.of()), locationsByLotId)));

        ReportTable table = new ReportTable("Stock Levels", List.of("Item Code", "Item Name", "Type", "Category",
                "Location", "Available Quantity", "Total Quantity", "Units", "Status"));

        List<InventoryItem> sorted = sortItems(items, request, locationByItemId);
        double totalSum = 0;
        double availableSum = 0;
        for (InventoryItem item : sorted) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double totalQuantity = totalQuantity(lots);
            double availableQuantity = availableQuantity(lots);
            totalSum += totalQuantity;
            availableSum += availableQuantity;
            table.addRow(List.of(item.getId(), item.getName(), nullToEmpty(item.getItemType()),
                    nullToEmpty(item.getCategory()), locationByItemId.get(item.getId()),
                    formatNumber(availableQuantity), formatNumber(totalQuantity), item.getUnits(),
                    item.isActive() ? "Active" : "Inactive"));
        }
        addQuantityTotalsRow(table, sorted.size(), 5, availableSum, 6, totalSum);
        return table;
    }

    /**
     * "Low stock" is judged against {@link InventoryLot#isAvailableForUse}
     * quantity, not the raw sum of every lot — a threshold check against total
     * quantity would count EXPIRED/DISPOSED/QUARANTINED/QC-failed stock as if it
     * were usable, which is exactly backwards for an alert meant to answer "what do
     * we need to reorder." {@code InventoryItemService.getLowStockItems()} now does
     * this same available-quantity check (previously it delegated to a native-SQL
     * query with the raw-sum flaw — also the source behind the Inventory
     * Dashboard's low-stock tile, fixed there too).
     */
    private ReportTable buildLowStockReport(InventoryReportRequest request) {
        List<InventoryItem> items = inventoryItemService.getLowStockItems();
        Map<String, List<InventoryLot>> lotsByItemId = loadLotsByItemId(items);
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lotsByItemId);
        Map<String, String> locationByItemId = items.stream().collect(Collectors.toMap(InventoryItem::getId,
                item -> summarizeLocation(lotsByItemId.getOrDefault(item.getId(), List.of()), locationsByLotId)));
        items = sortItems(items, request, locationByItemId);

        ReportTable table = new ReportTable("Low Stock Items", List.of("Item Code", "Item Name", "Type", "Category",
                "Location", "Available Quantity", "Total Quantity", "Low Stock Threshold", "Units"));
        double availableSum = 0;
        double totalSum = 0;
        for (InventoryItem item : items) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double availableQuantity = availableQuantity(lots);
            double totalQuantity = totalQuantity(lots);
            availableSum += availableQuantity;
            totalSum += totalQuantity;
            table.addRow(List.of(item.getId(), item.getName(), nullToEmpty(item.getItemType()),
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
        Map<String, InventoryItem> itemsById = items.stream()
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        List<InventoryLot> allLots = inventoryLotService.getAll().stream()
                .filter(lot -> lot.getInventoryItem() != null && itemsById.containsKey(lot.getInventoryItem().getId()))
                .filter(lot -> lot.getEffectiveExpirationDate() != null)
                .filter(lot -> request.isIncludeExpired() || !lot.isExpired())
                // Honor the date-range filter the UI already shows for every report type —
                // previously silently ignored here despite the description promising
                // "expiring within specified date range." No range provided means no
                // filter, keeping the previous "show everything ahead" default.
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
                            (InventoryLot l) -> nullToEmpty(itemsById.get(l.getInventoryItem().getId()).getItemType()))
                    .thenComparing(byExpiration);
        } else if (request.isGroupByLocation()) {
            comparator = Comparator.comparing((InventoryLot l) -> resolveLotLocation(l, locationsByLotId))
                    .thenComparing(byExpiration);
        }
        allLots.sort(comparator);

        ReportTable table = new ReportTable("Expiration Forecast",
                List.of("Item Code", "Item Name", "Type", "Lot Number", "Location", "Expiration Date",
                        "Days Until Expiration", "Urgency", "Current Quantity", "Status"));
        long now = System.currentTimeMillis();
        for (InventoryLot lot : allLots) {
            InventoryItem item = itemsById.get(lot.getInventoryItem().getId());
            long daysUntil = (lot.getEffectiveExpirationDate().getTime() - now) / (1000L * 60 * 60 * 24);
            table.addRow(List.of(item.getId(), item.getName(), nullToEmpty(item.getItemType()), lot.getLotNumber(),
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
     * A "trend" report aggregates — one row per item summarizing consumption over
     * the range, sorted by heaviest use first — rather than a raw per-transaction
     * log (which {@code TRANSACTION_HISTORY} already covers).
     */
    private ReportTable buildUsageTrendsReport(InventoryReportRequest request) {
        List<InventoryUsage> usages = inventoryUsageService.getByDateRange(request.getStartDate(),
                request.getEndDate());

        Map<String, List<InventoryUsage>> usagesByItemId = usages.stream()
                .filter(usage -> usage.getInventoryItem() != null)
                .collect(Collectors.groupingBy(usage -> usage.getInventoryItem().getId()));

        ReportTable table = new ReportTable("Usage Trends", List.of("Item Code", "Item Name", "Type",
                "Total Quantity Used", "Usage Events", "Avg Quantity Per Use", "First Use", "Last Use"));

        List<Map.Entry<String, List<InventoryUsage>>> sortedByUsage = usagesByItemId.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<String, List<InventoryUsage>> e) -> totalQuantityUsed(e.getValue()))
                        .reversed())
                .collect(Collectors.toList());

        double grandTotalUsed = 0;
        int totalEvents = 0;
        for (Map.Entry<String, List<InventoryUsage>> entry : sortedByUsage) {
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

            table.addRow(List.of(item.getId(), item.getName(), nullToEmpty(item.getItemType()), formatNumber(totalUsed),
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
                "Lot Number", "Transaction Type", "Quantity Change", "Quantity After", "Performed By"));
        for (InventoryTransaction transaction : transactions) {
            InventoryLot lot = transaction.getLot();
            InventoryItem item = lot != null ? lot.getInventoryItem() : null;
            table.addRow(List.of(formatDateTime(transaction.getTransactionDate()), item != null ? item.getId() : "",
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
                        "Current Quantity", "Status", "QC Status", "Location"));
        for (InventoryLot lot : lots) {
            InventoryItem item = lot.getInventoryItem();
            table.addRow(List.of(item.getId(), item.getName(), lot.getLotNumber(), formatDate(lot.getReceiptDate()),
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
     * Usable stock only — {@link InventoryLot#isAvailableForUse()} excludes
     * EXPIRED/DISPOSED/QUARANTINED lots and anything that failed QC. This is the
     * number that answers "how much can we actually use," as distinct from
     * {@link #totalQuantity} (raw physical sum, including dead stock).
     */
    private double availableQuantity(List<InventoryLot> lots) {
        return lots.stream().filter(InventoryLot::isAvailableForUse)
                .mapToDouble(l -> l.getCurrentQuantity() != null ? l.getCurrentQuantity() : 0.0).sum();
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
     * "Group by" reads here as "cluster adjacent rows in the flat exported table" —
     * sort by type and/or each item's {@link #summarizeLocation} result
     * (precomputed by the caller, since it needs the item's lots) ahead of name,
     * rather than emitting separate section headers.
     */
    private List<InventoryItem> sortItems(List<InventoryItem> items, InventoryReportRequest request,
            Map<String, String> locationByItemId) {
        Comparator<InventoryItem> comparator = Comparator.comparing(InventoryItem::getName,
                Comparator.nullsLast(Comparator.naturalOrder()));
        if (request.isGroupByLocation()) {
            comparator = Comparator.comparing((InventoryItem i) -> locationByItemId.getOrDefault(i.getId(), ""))
                    .thenComparing(comparator);
        }
        if (request.isGroupByType()) {
            comparator = Comparator.comparing((InventoryItem i) -> nullToEmpty(i.getItemType()))
                    .thenComparing(comparator);
        }
        return items.stream().sorted(comparator).collect(Collectors.toList());
    }

    private Map<String, List<InventoryLot>> loadLotsByItemId(List<InventoryItem> items) {
        Map<String, List<InventoryLot>> lotsByItemId = new HashMap<>();
        for (InventoryItem item : items) {
            lotsByItemId.put(item.getId(), inventoryLotService.getByInventoryItemId(item.getId()));
        }
        return lotsByItemId;
    }

    private Map<String, Map<String, Object>> loadLocationsByLotId(Map<String, List<InventoryLot>> lotsByItemId) {
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
        return String.format("%.2f", value);
    }
}
