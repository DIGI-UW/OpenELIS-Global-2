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
import org.openelisglobal.inventory.service.InventoryItemTypeService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryItemType;
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
    private InventoryItemTypeService inventoryItemTypeService;

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

        ReportTable table = new ReportTable("Stock Levels",
                List.of("Item Code", "Item Name", "Type", "Location", "Total Quantity", "Units", "Status"));

        List<InventoryItem> sorted = sortItems(items, request, locationByItemId);
        for (InventoryItem item : sorted) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double totalQuantity = lots.stream()
                    .mapToDouble(l -> l.getCurrentQuantity() != null ? l.getCurrentQuantity() : 0.0).sum();
            table.addRow(List.of(item.getId(), item.getName(), itemTypeLabel(item.getItemType()),
                    locationByItemId.get(item.getId()), formatNumber(totalQuantity), item.getUnits(),
                    item.isActive() ? "Active" : "Inactive"));
        }
        return table;
    }

    private ReportTable buildLowStockReport(InventoryReportRequest request) {
        List<InventoryItem> items = inventoryItemService.getLowStockItems();
        Map<String, List<InventoryLot>> lotsByItemId = loadLotsByItemId(items);
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lotsByItemId);
        Map<String, String> locationByItemId = items.stream().collect(Collectors.toMap(InventoryItem::getId,
                item -> summarizeLocation(lotsByItemId.getOrDefault(item.getId(), List.of()), locationsByLotId)));
        items = sortItems(items, request, locationByItemId);

        ReportTable table = new ReportTable("Low Stock Items", List.of("Item Code", "Item Name", "Type", "Location",
                "Current Quantity", "Low Stock Threshold", "Units"));
        for (InventoryItem item : items) {
            List<InventoryLot> lots = lotsByItemId.getOrDefault(item.getId(), List.of());
            double totalQuantity = lots.stream()
                    .mapToDouble(l -> l.getCurrentQuantity() != null ? l.getCurrentQuantity() : 0.0).sum();
            table.addRow(List.of(item.getId(), item.getName(), itemTypeLabel(item.getItemType()),
                    locationByItemId.get(item.getId()), formatNumber(totalQuantity),
                    item.getLowStockThreshold() != null ? item.getLowStockThreshold().toString() : "",
                    item.getUnits()));
        }
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
                .filter(lot -> request.isIncludeExpired() || !lot.isExpired()).collect(Collectors.toList());

        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(allLots);

        Comparator<InventoryLot> byExpiration = Comparator.comparing(InventoryLot::getEffectiveExpirationDate);
        Comparator<InventoryLot> comparator = byExpiration;
        if (request.isGroupByType()) {
            comparator = Comparator.comparing(
                    (InventoryLot l) -> itemTypeLabel(itemsById.get(l.getInventoryItem().getId()).getItemType()))
                    .thenComparing(byExpiration);
        } else if (request.isGroupByLocation()) {
            comparator = Comparator.comparing((InventoryLot l) -> resolveLotLocation(l, locationsByLotId))
                    .thenComparing(byExpiration);
        }
        allLots.sort(comparator);

        ReportTable table = new ReportTable("Expiration Forecast", List.of("Item Code", "Item Name", "Type",
                "Lot Number", "Location", "Expiration Date", "Days Until Expiration", "Current Quantity", "Status"));
        long now = System.currentTimeMillis();
        for (InventoryLot lot : allLots) {
            InventoryItem item = itemsById.get(lot.getInventoryItem().getId());
            long daysUntil = (lot.getEffectiveExpirationDate().getTime() - now) / (1000L * 60 * 60 * 24);
            table.addRow(List.of(item.getId(), item.getName(), itemTypeLabel(item.getItemType()), lot.getLotNumber(),
                    resolveLotLocation(lot, locationsByLotId), formatDate(lot.getEffectiveExpirationDate()),
                    Long.toString(daysUntil),
                    formatNumber(lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0),
                    lot.getStatus() != null ? lot.getStatus().name() : ""));
        }
        return table;
    }

    private ReportTable buildUsageTrendsReport(InventoryReportRequest request) {
        List<InventoryUsage> usages = inventoryUsageService.getByDateRange(request.getStartDate(),
                request.getEndDate());
        Map<Integer, String> userNameCache = new HashMap<>();

        ReportTable table = new ReportTable("Usage Trends",
                List.of("Date", "Item Code", "Item Name", "Lot Number", "Quantity Used", "Performed By"));
        for (InventoryUsage usage : usages) {
            InventoryItem item = usage.getInventoryItem();
            InventoryLot lot = usage.getLot();
            table.addRow(List.of(formatDateTime(usage.getUsageDate()), item != null ? item.getId() : "",
                    item != null ? item.getName() : "", lot != null ? lot.getLotNumber() : "",
                    formatNumber(usage.getQuantityUsed() != null ? usage.getQuantityUsed() : 0.0),
                    resolveUserName(usage.getPerformedByUser(), userNameCache)));
        }
        return table;
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
            comparator = Comparator.comparing((InventoryItem i) -> itemTypeLabel(i.getItemType()))
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

    private String itemTypeLabel(String itemTypeCode) {
        if (itemTypeCode == null) {
            return "";
        }
        InventoryItemType type = inventoryItemTypeService.getByCode(itemTypeCode);
        return type != null ? type.getLabel() : itemTypeCode;
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
