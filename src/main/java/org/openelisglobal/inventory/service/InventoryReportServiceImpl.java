package org.openelisglobal.inventory.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReportServiceImpl implements InventoryReportService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<DateTimeFormatter> INPUT_DATE_FORMATS = Arrays.asList(DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MM/dd/yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("#,##0.##");

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Override
    @Transactional(readOnly = true)
    public GeneratedReport generateReport(String reportType, String exportFormat, String startDate, String endDate,
            boolean includeInactive, boolean includeExpired, boolean groupByType, boolean groupByLocation) {
        String normalizedReportType = normalizeRequired(reportType, "reportType");
        String normalizedExportFormat = normalizeRequired(exportFormat, "exportFormat");

        ReportTable table = buildTable(normalizedReportType, startDate, endDate, includeInactive, includeExpired,
                groupByType, groupByLocation);

        byte[] content;
        String contentType;
        String extension;

        switch (normalizedExportFormat) {
        case "PDF":
            content = buildPdf(table);
            contentType = "application/pdf";
            extension = "pdf";
            break;
        case "EXCEL":
            content = buildWorkbook(table);
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = "xlsx";
            break;
        case "CSV":
            content = buildCsv(table).getBytes(StandardCharsets.UTF_8);
            contentType = "text/csv";
            extension = "csv";
            break;
        default:
            throw new IllegalArgumentException("Unsupported export format: " + exportFormat);
        }

        String fileName = String.format("inventory_%s_%s.%s", normalizedReportType.toLowerCase(Locale.ROOT),
                LocalDate.now().format(FILE_DATE_FORMAT), extension);
        return new GeneratedReport(content, contentType, fileName);
    }

    private ReportTable buildTable(String reportType, String startDate, String endDate, boolean includeInactive,
            boolean includeExpired, boolean groupByType, boolean groupByLocation) {
        switch (reportType) {
        case "STOCK_LEVELS":
            return buildStockLevelsTable(includeInactive, includeExpired, groupByType, groupByLocation, false);
        case "LOW_STOCK":
            return buildStockLevelsTable(includeInactive, includeExpired, groupByType, groupByLocation, true);
        case "EXPIRATION_FORECAST":
            return buildExpirationForecastTable(includeInactive, includeExpired, groupByType, groupByLocation);
        case "TRANSACTION_HISTORY":
            return buildTransactionHistoryTable(includeInactive, startDate, endDate);
        case "USAGE_TRENDS":
            return buildUsageTrendsTable(includeInactive, startDate, endDate, groupByType, groupByLocation);
        case "LOT_TRACEABILITY":
            return buildLotTraceabilityTable(includeInactive, includeExpired, groupByType, groupByLocation);
        default:
            throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }
    }

    private ReportTable buildStockLevelsTable(boolean includeInactive, boolean includeExpired, boolean groupByType,
            boolean groupByLocation, boolean lowStockOnly) {
        List<InventoryItem> items = includeInactive ? inventoryItemService.getAll()
                : inventoryItemService.getAllActive();
        List<InventoryLot> lots = filterLots(loadLots(), includeInactive, includeExpired);
        Map<Long, List<InventoryLot>> lotsByItem = lots.stream()
                .filter(lot -> lot.getInventoryItem() != null && lot.getInventoryItem().getId() != null)
                .collect(Collectors.groupingBy(lot -> lot.getInventoryItem().getId(), LinkedHashMap::new,
                        Collectors.toList()));

        List<List<String>> rows = new ArrayList<>();
        for (InventoryItem item : items) {
            List<InventoryLot> itemLots = lotsByItem.getOrDefault(item.getId(), List.of());
            double totalQuantity = itemLots.stream().map(InventoryLot::getCurrentQuantity).filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue).sum();
            int activeLots = (int) itemLots.stream().filter(lot -> lot.getCurrentQuantity() != null
                    && lot.getCurrentQuantity() > 0 && !"DISPOSED".equals(String.valueOf(lot.getStatus()))).count();
            int threshold = item.getLowStockThreshold() != null ? item.getLowStockThreshold() : 0;
            String stockStatus = stockStatus(totalQuantity, threshold);

            if (lowStockOnly && "In Stock".equals(stockStatus)) {
                continue;
            }

            LinkedHashSet<String> locations = itemLots.stream().map(this::displayLocation)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            rows.add(List.of(safe(item.getName()), safe(String.valueOf(item.getItemType())),
                    formatQuantity(totalQuantity), safe(item.getUnits()), String.valueOf(threshold), stockStatus,
                    String.valueOf(activeLots), String.join("; ", locations)));
        }

        sortRows(rows, groupByType ? 1 : null, groupByLocation ? 7 : null, 0);
        return new ReportTable(
                lowStockOnly ? "Low Stock Report" : "Stock Levels Report", List.of("Item Name", "Item Type",
                        "Total Quantity", "Units", "Low Stock Threshold", "Status", "Active Lots", "Storage Locations"),
                rows);
    }

    private ReportTable buildExpirationForecastTable(boolean includeInactive, boolean includeExpired,
            boolean groupByType, boolean groupByLocation) {
        List<List<String>> rows = filterLots(loadLots(), includeInactive, includeExpired).stream()
                .filter(lot -> lot.getEffectiveExpirationDate() != null)
                .sorted(Comparator.comparing(InventoryLot::getEffectiveExpirationDate)).map(lot -> {
                    LocalDate expirationDate = toLocalDate(lot.getEffectiveExpirationDate());
                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
                    return List.of(safe(itemName(lot)), safe(String.valueOf(lot.getInventoryItem().getItemType())),
                            safe(lot.getLotNumber()), formatQuantity(lot.getCurrentQuantity()),
                            safe(lot.getInventoryItem().getUnits()), safe(expirationDate.format(DISPLAY_DATE_FORMAT)),
                            String.valueOf(daysRemaining), safe(String.valueOf(lot.getQcStatus())),
                            safe(String.valueOf(lot.getStatus())), displayLocation(lot));
                }).collect(Collectors.toCollection(ArrayList::new));

        sortRows(rows, groupByType ? 1 : null, groupByLocation ? 9 : null, 5);
        return new ReportTable(
                "Expiration Forecast Report", List.of("Item Name", "Item Type", "Lot Number", "Current Quantity",
                        "Units", "Effective Expiry", "Days Remaining", "QC Status", "Lot Status", "Storage Location"),
                rows);
    }

    private ReportTable buildTransactionHistoryTable(boolean includeInactive, String startDate, String endDate) {
        DateRange range = requireDateRange(startDate, endDate, "Transaction history report");
        List<List<String>> rows = inventoryTransactionService.getByDateRange(range.start(), range.end()).stream()
                .filter(tx -> includeInactive || isActiveItem(tx.getLot()))
                .sorted(Comparator.comparing(InventoryTransaction::getTransactionDate).reversed())
                .map(tx -> List.of(safe(itemName(tx.getLot())), safe(lotNumber(tx)),
                        safe(String.valueOf(tx.getTransactionType())), formatQuantity(tx.getQuantityChange()),
                        formatQuantity(tx.getQuantityAfter()),
                        safe(toLocalDateTime(tx.getTransactionDate())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                        safe(String.valueOf(tx.getReferenceType())), safe(tx.getNotes()), displayLocation(tx.getLot())))
                .collect(Collectors.toCollection(ArrayList::new));

        return new ReportTable("Transaction History Report", List.of("Item Name", "Lot Number", "Transaction Type",
                "Quantity Change", "Quantity After", "Transaction Date", "Reference Type", "Notes", "Storage Location"),
                rows);
    }

    private ReportTable buildUsageTrendsTable(boolean includeInactive, String startDate, String endDate,
            boolean groupByType, boolean groupByLocation) {
        DateRange range = requireDateRange(startDate, endDate, "Usage trends report");
        Map<String, UsageAggregate> aggregates = new LinkedHashMap<>();

        for (InventoryTransaction tx : inventoryTransactionService.getByDateRange(range.start(), range.end())) {
            if (!"CONSUMPTION".equals(String.valueOf(tx.getTransactionType())) || tx.getLot() == null) {
                continue;
            }
            if (!includeInactive && !isActiveItem(tx.getLot())) {
                continue;
            }

            String itemType = safe(String.valueOf(tx.getLot().getInventoryItem().getItemType()));
            String location = displayLocation(tx.getLot());
            String key = itemName(tx.getLot()) + "|" + (groupByType ? itemType : "") + "|"
                    + (groupByLocation ? location : "");
            UsageAggregate aggregate = aggregates.computeIfAbsent(key,
                    ignored -> new UsageAggregate(itemName(tx.getLot()), itemType,
                            safe(tx.getLot().getInventoryItem().getUnits()), location));
            aggregate.events++;
            aggregate.totalConsumed += Math.abs(tx.getQuantityChange() != null ? tx.getQuantityChange() : 0.0);
            LocalDateTime txDate = toLocalDateTime(tx.getTransactionDate());
            if (aggregate.firstUsage == null || txDate.isBefore(aggregate.firstUsage)) {
                aggregate.firstUsage = txDate;
            }
            if (aggregate.lastUsage == null || txDate.isAfter(aggregate.lastUsage)) {
                aggregate.lastUsage = txDate;
            }
        }

        List<List<String>> rows = aggregates.values().stream().map(aggregate -> List.of(safe(aggregate.itemName),
                safe(aggregate.itemType), safe(aggregate.units), String.valueOf(aggregate.events),
                formatQuantity(aggregate.totalConsumed),
                aggregate.firstUsage != null ? aggregate.firstUsage.toLocalDate().format(DISPLAY_DATE_FORMAT) : "",
                aggregate.lastUsage != null ? aggregate.lastUsage.toLocalDate().format(DISPLAY_DATE_FORMAT) : "",
                safe(aggregate.location))).collect(Collectors.toCollection(ArrayList::new));

        sortRows(rows, groupByType ? 1 : null, groupByLocation ? 7 : null, 0);
        return new ReportTable("Usage Trends Report", List.of("Item Name", "Item Type", "Units", "Consumption Events",
                "Total Consumed", "First Usage", "Last Usage", "Storage Location"), rows);
    }

    private ReportTable buildLotTraceabilityTable(boolean includeInactive, boolean includeExpired, boolean groupByType,
            boolean groupByLocation) {
        List<List<String>> rows = new ArrayList<>();
        for (InventoryLot lot : filterLots(loadLots(), includeInactive, includeExpired)) {
            List<InventoryTransaction> transactions = inventoryTransactionService.getByLotId(lot.getId());
            InventoryTransaction latest = transactions.stream()
                    .max(Comparator.comparing(InventoryTransaction::getTransactionDate)).orElse(null);
            rows.add(List.of(safe(itemName(lot)), safe(String.valueOf(lot.getInventoryItem().getItemType())),
                    safe(lot.getLotNumber()), safe(lot.getBarcode()),
                    lot.getReceiptDate() != null ? toLocalDate(lot.getReceiptDate()).format(DISPLAY_DATE_FORMAT) : "",
                    formatQuantity(lot.getCurrentQuantity()), safe(String.valueOf(lot.getQcStatus())),
                    safe(String.valueOf(lot.getStatus())), displayLocation(lot),
                    latest != null ? safe(String.valueOf(latest.getTransactionType())) : "",
                    latest != null ? safe(toLocalDateTime(latest.getTransactionDate())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) : "",
                    latest != null ? safe(latest.getNotes()) : ""));
        }

        sortRows(rows, groupByType ? 1 : null, groupByLocation ? 8 : null, 0);
        return new ReportTable("Lot Traceability Report",
                List.of("Item Name", "Item Type", "Lot Number", "Barcode", "Receipt Date", "Current Quantity",
                        "QC Status", "Lot Status", "Storage Location", "Latest Transaction", "Latest Transaction Date",
                        "Latest Notes"),
                rows);
    }

    private List<InventoryLot> loadLots() {
        List<InventoryLot> lots = inventoryLotService.getAll();
        lots.forEach(lot -> {
            if (lot.getInventoryItem() != null) {
                lot.getInventoryItem().getName();
            }
        });
        return lots;
    }

    private List<InventoryLot> filterLots(List<InventoryLot> lots, boolean includeInactive, boolean includeExpired) {
        return lots.stream().filter(lot -> lot.getInventoryItem() != null)
                .filter(lot -> includeInactive || lot.getInventoryItem().isActive())
                .filter(lot -> includeExpired || !lot.isExpired()).collect(Collectors.toList());
    }

    private String buildCsv(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(csvLine(table.headers));
        for (List<String> row : table.rows) {
            sb.append(csvLine(row));
        }
        return sb.toString();
    }

    private byte[] buildWorkbook(ReportTable table) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(table.title);

            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < table.headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 3;
            for (List<String> row : table.rows) {
                Row excelRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < row.size(); i++) {
                    excelRow.createCell(i).setCellValue(row.get(i));
                }
            }

            for (int i = 0; i < table.headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Excel report", e);
        }
    }

    private byte[] buildPdf(ReportTable table) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(table.headers.size() > 7 ? PageSize.A4.rotate() : PageSize.A4, 24, 24, 24,
                    24);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph title = new Paragraph(table.title, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(12f);
            document.add(title);

            PdfPTable pdfTable = new PdfPTable(table.headers.size());
            pdfTable.setWidthPercentage(100f);
            pdfTable.setSpacingBefore(8f);

            for (String header : table.headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4f);
                pdfTable.addCell(cell);
            }

            for (List<String> row : table.rows) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
                    cell.setPadding(3f);
                    pdfTable.addCell(cell);
                }
            }

            document.add(pdfTable);
            document.close();
            return output.toByteArray();
        } catch (DocumentException | java.io.IOException e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        }
    }

    private String csvLine(List<String> values) {
        return values.stream().map(this::escapeCsv).collect(Collectors.joining(",")) + "\n";
    }

    private String escapeCsv(String value) {
        String safeValue = safe(value);
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private void sortRows(List<List<String>> rows, Integer primaryIndex, Integer secondaryIndex,
            Integer fallbackIndex) {
        List<Integer> order = new ArrayList<>();
        if (primaryIndex != null) {
            order.add(primaryIndex);
        }
        if (secondaryIndex != null && !order.contains(secondaryIndex)) {
            order.add(secondaryIndex);
        }
        if (fallbackIndex != null && !order.contains(fallbackIndex)) {
            order.add(fallbackIndex);
        }
        if (order.isEmpty()) {
            return;
        }

        rows.sort((left, right) -> {
            for (Integer index : order) {
                int result = safe(left.get(index)).compareToIgnoreCase(safe(right.get(index)));
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        });
    }

    private DateRange requireDateRange(String startDate, String endDate, String reportName) {
        if (isBlank(startDate) || isBlank(endDate)) {
            throw new IllegalArgumentException(reportName + " requires startDate and endDate");
        }

        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate cannot be earlier than startDate");
        }

        return new DateRange(Timestamp.valueOf(start.atStartOfDay()),
                Timestamp.valueOf(end.plusDays(1).atStartOfDay().minusNanos(1)));
    }

    private LocalDate parseDate(String rawValue, String fieldName) {
        for (DateTimeFormatter formatter : INPUT_DATE_FORMATS) {
            try {
                return LocalDate.parse(rawValue, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException("Invalid " + fieldName + " format: " + rawValue);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isActiveItem(InventoryLot lot) {
        return lot != null && lot.getInventoryItem() != null && lot.getInventoryItem().isActive();
    }

    private String itemName(InventoryLot lot) {
        return lot != null && lot.getInventoryItem() != null ? safe(lot.getInventoryItem().getName()) : "";
    }

    private String lotNumber(InventoryTransaction tx) {
        return tx.getLot() != null ? safe(tx.getLot().getLotNumber()) : "";
    }

    private String displayLocation(InventoryLot lot) {
        if (!isBlank(lot.getStoragePath())) {
            return lot.getStoragePath();
        }
        if (!isBlank(lot.getSpecificStorageLocation())) {
            return lot.getSpecificStorageLocation();
        }
        if (!isBlank(lot.getLocationType())) {
            return lot.getLocationType() + (lot.getLocationId() != null ? (" #" + lot.getLocationId()) : "");
        }
        return "Unassigned";
    }

    private LocalDate toLocalDate(Timestamp timestamp) {
        return timestamp.toLocalDateTime().toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }

    private String stockStatus(double totalQuantity, int threshold) {
        if (totalQuantity <= 0) {
            return "Out of Stock";
        }
        if (threshold > 0 && totalQuantity <= threshold) {
            return "Low Stock";
        }
        return "In Stock";
    }

    private String formatQuantity(Double value) {
        return value == null ? "" : QUANTITY_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static class ReportTable {
        private final String title;
        private final List<String> headers;
        private final List<List<String>> rows;

        private ReportTable(String title, List<String> headers, List<List<String>> rows) {
            this.title = title;
            this.headers = headers;
            this.rows = rows;
        }
    }

    private static class UsageAggregate {
        private final String itemName;
        private final String itemType;
        private final String units;
        private final String location;
        private int events;
        private double totalConsumed;
        private LocalDateTime firstUsage;
        private LocalDateTime lastUsage;

        private UsageAggregate(String itemName, String itemType, String units, String location) {
            this.itemName = itemName;
            this.itemType = itemType;
            this.units = units;
            this.location = location;
        }
    }

    private record DateRange(Timestamp start, Timestamp end) {
    }
}
