package org.openelisglobal.inventory.report;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Comparator;
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
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.storage.service.SampleStorageService;
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

    private final ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private final ThreadLocal<SimpleDateFormat> dateTimeFormat = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm"));

    @Override
    @Transactional(readOnly = true)
    public ReportTable generateReport(InventoryReportRequest request) {
        switch (request.getReportType()) {
        case "RECEIVED":
            requireDateRange(request);
            return buildReceivedReport(request);
        case "CONSUMED":
            requireDateRange(request);
            return buildConsumedReport(request);
        case "STOCK_ON_HAND":
            return buildStockOnHandReport(request);
        case "EXPIRING":
            return buildExpiringReport(request);
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

    /**
     * What arrived in the period, one row per receipt. Reads RECEIPT transactions
     * rather than lot receipt dates: a lot's {@code receipt_date} is overwritten by
     * every later receive against it, and every lot carried over by the legacy
     * migration shares that migration's timestamp, so it cannot answer "what came
     * in during August". Stock that predates the transaction log therefore does not
     * appear here, which is correct for a period report — it was not received in
     * the period.
     */
    private ReportTable buildReceivedReport(InventoryReportRequest request) {
        List<InventoryTransaction> receipts = inventoryTransactionService.getByTypeAndDateRange(TransactionType.RECEIPT,
                request.getStartDate(), request.getEndDate());

        ReportTable table = new ReportTable("Received", List.of("Date Received", "Item Code", "Item Name", "Tags",
                "Lot Number", "Quantity Received", "Expiration Date"), Set.of(5));

        double totalReceived = 0;
        int rows = 0;
        for (InventoryTransaction receipt : receipts) {
            InventoryLot lot = receipt.getLot();
            InventoryItem item = lot != null ? lot.getInventoryItem() : null;
            if (item == null || !matchesTags(item, request)) {
                continue;
            }
            double quantity = receipt.getQuantityChange() != null ? receipt.getQuantityChange() : 0.0;
            totalReceived += quantity;
            rows++;
            table.addRow(List.of(formatDateTime(receipt.getTransactionDate()), nullToEmpty(item.getCode()),
                    nullToEmpty(item.getName()), formatTags(item), nullToEmpty(lot.getLotNumber()),
                    formatNumber(quantity), formatDate(lot.getEffectiveExpirationDate())));
        }
        addTotalsRow(table, rows + " receipts", Map.of(5, totalReceived));
        return table;
    }

    /**
     * What was used in the period, one row per item. Reads {@code inventory_usage},
     * the same table the board projects from, so the report and the run-out
     * estimate cannot disagree. Wastage is deliberately excluded: disposal, manual
     * adjustment and count shrinkage write a transaction and no usage row, and
     * folding them in here would report them as consumption.
     */
    private ReportTable buildConsumedReport(InventoryReportRequest request) {
        List<InventoryUsage> usages = inventoryUsageService.getByDateRange(request.getStartDate(),
                request.getEndDate());

        Map<Long, List<InventoryUsage>> usagesByItemId = usages.stream()
                .filter(usage -> usage.getInventoryItem() != null)
                .filter(usage -> matchesTags(usage.getInventoryItem(), request))
                .collect(Collectors.groupingBy(usage -> usage.getInventoryItem().getId()));

        ReportTable table = new ReportTable("Consumed", List.of("Item Code", "Item Name", "Tags", "Total Quantity Used",
                "Usage Events", "Avg Quantity Per Use", "First Use", "Last Use"), Set.of(3, 4, 5));

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

            table.addRow(List.of(nullToEmpty(item.getCode()), nullToEmpty(item.getName()), formatTags(item),
                    formatNumber(totalUsed), Integer.toString(eventCount),
                    formatNumber(eventCount == 0 ? 0.0 : totalUsed / eventCount), formatDateTime(firstUse),
                    formatDateTime(lastUse)));
        }
        addTotalsRow(table, sortedByUsage.size() + " items", Map.of(3, grandTotalUsed, 4, (double) totalEvents));
        return table;
    }

    /**
     * Stock as it stood at a chosen instant, one row per lot with its expiry.
     *
     * <p>
     * With no date, this is today and the lot's own quantity answers it. With a
     * date, the quantity comes from replaying {@code inventory_transaction}: the
     * lot table holds only today's figure, while {@code quantity_after} records
     * what each write left behind. A lot with no transaction before that instant is
     * omitted rather than shown as zero — it had not been received yet.
     *
     * <p>
     * The replay is only as good as the transaction log. A write that moves stock
     * without recording a transaction leaves this report reporting the last figure
     * that was recorded; {@code PUT /rest/inventory/lots/{id}} is one such path and
     * is a known open defect, not something this report papers over.
     */
    private ReportTable buildStockOnHandReport(InventoryReportRequest request) {
        List<InventoryItem> items = request.isIncludeInactive() ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();
        Map<Long, InventoryItem> itemsById = items.stream().filter(item -> matchesTags(item, request))
                .collect(Collectors.toMap(InventoryItem::getId, item -> item, (a, b) -> a));

        boolean asOfPastDate = request.getEndDate() != null;
        Map<Long, Double> quantityByLotId = asOfPastDate
                ? inventoryTransactionService.getQuantityOnHandAsOf(request.getEndDate())
                : Map.of();

        List<InventoryLot> lots = inventoryLotService.getAll().stream()
                .filter(lot -> lot.getInventoryItem() != null && itemsById.containsKey(lot.getInventoryItem().getId()))
                .sorted(Comparator
                        .comparing(
                                (InventoryLot l) -> nullToEmpty(itemsById.get(l.getInventoryItem().getId()).getName()))
                        .thenComparing(l -> nullToEmpty(l.getLotNumber())))
                .collect(Collectors.toList());
        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(lots);

        ReportTable table = new ReportTable("Stock on Hand", List.of("Item Code", "Item Name", "Tags", "Lot Number",
                "Expiration Date", "Quantity", "Location", "Status", "QC Status"), Set.of(5));

        double totalQuantity = 0;
        int rows = 0;
        for (InventoryLot lot : lots) {
            Double quantity = asOfPastDate ? quantityByLotId.get(lot.getId()) : lot.getCurrentQuantity();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            InventoryItem item = itemsById.get(lot.getInventoryItem().getId());
            totalQuantity += quantity;
            rows++;
            table.addRow(List.of(nullToEmpty(item.getCode()), nullToEmpty(item.getName()), formatTags(item),
                    nullToEmpty(lot.getLotNumber()), formatDate(lot.getEffectiveExpirationDate()),
                    formatNumber(quantity), resolveLotLocation(lot, locationsByLotId), readable(lot.getStatus()),
                    readable(lot.getQcStatus())));
        }
        addTotalsRow(table, rows + " lots", Map.of(5, totalQuantity));
        return table;
    }

    /**
     * Sum of {@link InventoryLot#countsAsAvailableStock()} stock, the rule the
     * low-stock alert shares so tile and report cannot disagree.
     */
    private double availableQuantity(List<InventoryLot> lots) {
        return lots.stream().filter(InventoryLot::countsAsAvailableStock).mapToDouble(InventoryLot::getCurrentQuantity)
                .sum();
    }

    /**
     * Lots whose effective expiry falls in the chosen window, soonest first. The
     * effective date is the earlier of the printed expiry and any shortened
     * after-opening date, so a lot opened last week sorts by when it actually goes
     * off.
     */
    private ReportTable buildExpiringReport(InventoryReportRequest request) {
        List<InventoryItem> items = request.isIncludeInactive() ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();
        Map<Long, InventoryItem> itemsById = items.stream().filter(item -> matchesTags(item, request))
                .collect(Collectors.toMap(InventoryItem::getId, i -> i, (a, b) -> a));

        List<InventoryLot> allLots = inventoryLotService.getAll().stream()
                .filter(lot -> lot.getInventoryItem() != null && itemsById.containsKey(lot.getInventoryItem().getId()))
                .filter(lot -> lot.getEffectiveExpirationDate() != null)
                .filter(lot -> request.isIncludeExpired() || !lot.isExpired())
                .filter(lot -> request.getStartDate() == null
                        || !lot.getEffectiveExpirationDate().before(request.getStartDate()))
                .filter(lot -> request.getEndDate() == null
                        || lot.getEffectiveExpirationDate().before(request.getEndDate()))
                .sorted(Comparator.comparing(InventoryLot::getEffectiveExpirationDate)).collect(Collectors.toList());

        Map<String, Map<String, Object>> locationsByLotId = loadLocationsByLotId(allLots);

        ReportTable table = new ReportTable("Expiring", List.of("Item Code", "Item Name", "Tags", "Lot Number",
                "Location", "Expiration Date", "Days Until Expiration", "Urgency", "Quantity", "Status"), Set.of(6, 8));
        long now = System.currentTimeMillis();
        double totalQuantity = 0;
        for (InventoryLot lot : allLots) {
            InventoryItem item = itemsById.get(lot.getInventoryItem().getId());
            long daysUntil = Math.floorDiv(lot.getEffectiveExpirationDate().getTime() - now, 1000L * 60 * 60 * 24);
            double quantity = lot.getCurrentQuantity() != null ? lot.getCurrentQuantity() : 0.0;
            totalQuantity += quantity;
            table.addRow(List.of(nullToEmpty(item.getCode()), nullToEmpty(item.getName()), formatTags(item),
                    nullToEmpty(lot.getLotNumber()), resolveLotLocation(lot, locationsByLotId),
                    formatDate(lot.getEffectiveExpirationDate()), Long.toString(daysUntil),
                    expirationUrgency(daysUntil), formatNumber(quantity), readable(lot.getStatus())));
        }
        addTotalsRow(table, allLots.size() + " lots", Map.of(8, totalQuantity));
        return table;
    }

    /**
     * At-a-glance triage bucket so the reader doesn't have to do date math per row.
     */
    private String expirationUrgency(long daysUntil) {
        if (daysUntil < 0) {
            return "Expired";
        }
        if (daysUntil <= 7) {
            return "This week";
        }
        if (daysUntil <= 30) {
            return "This month";
        }
        return "Later";
    }

    private double totalQuantityUsed(List<InventoryUsage> usages) {
        return usages.stream().mapToDouble(u -> u.getQuantityUsed() != null ? u.getQuantityUsed() : 0.0).sum();
    }

    /**
     * An item passes when it carries any of the requested tags. No tags requested
     * means no filter at all, rather than "items with no tags".
     */
    private boolean matchesTags(InventoryItem item, InventoryReportRequest request) {
        if (request.getTags().isEmpty()) {
            return true;
        }
        if (item.getTags() == null) {
            return false;
        }
        return item.getTags().stream()
                .anyMatch(tag -> request.getTags().stream().anyMatch(wanted -> wanted.equalsIgnoreCase(tag)));
    }

    private String formatTags(InventoryItem item) {
        if (item.getTags() == null || item.getTags().isEmpty()) {
            return "";
        }
        return item.getTags().stream().sorted().collect(Collectors.joining(", "));
    }

    // --- shared helpers ---

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * An enum constant as a reader would write it: "In use", not IN_USE. These
     * reports are printed and read by people, and the Java name of a constant is an
     * implementation detail that happened to reach the page.
     */
    private String readable(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String words = value.name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /**
     * Appends a "TOTAL (...)" row with the given columns summed. Reports differ in
     * which columns carry a quantity, so the caller names them rather than the
     * helper guessing.
     */
    private void addTotalsRow(ReportTable table, String label, Map<Integer, Double> sumsByColumn) {
        if (table.getRows().isEmpty()) {
            // A totals row over nothing is not a total, and it leaves the caller unable
            // to tell an empty report from a one-row one.
            return;
        }
        List<String> row = new java.util.ArrayList<>(java.util.Collections.nCopies(table.getHeaders().size(), ""));
        row.set(0, "TOTAL (" + label + ")");
        sumsByColumn.forEach((column, sum) -> row.set(column, formatNumber(sum)));
        table.addRow(row);
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
