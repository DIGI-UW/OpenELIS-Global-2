package org.openelisglobal.inventory.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LocationType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of InventoryImportService for importing inventory from Excel
 * files. Supports multi-sheet Excel workbooks with flexible column mapping.
 */
@Service
public class InventoryImportServiceImpl implements InventoryImportService {

    private static final int MAX_ROWS = 10000;

    // Common patterns for column name matching
    private static final Map<String, List<String>> COLUMN_PATTERNS = new LinkedHashMap<>();

    static {
        COLUMN_PATTERNS.put("itemName", List.of("items", "item", "name", "reagent", "product", "description"));
        COLUMN_PATTERNS.put("lotNumber", List.of("lot", "ref", "ref no", "ref/lot", "batch", "lot no", "lot number"));
        COLUMN_PATTERNS.put("quantity", List.of("quantity", "qty", "amount", "count"));
        COLUMN_PATTERNS.put("unit", List.of("unit", "units", "uom"));
        COLUMN_PATTERNS.put("expirationDate", List.of("expiry", "expiration", "exp date", "expiry date"));
        COLUMN_PATTERNS.put("manufacturingDate", List.of("manufacturing", "mfg", "manufacture date", "mfg date"));
        COLUMN_PATTERNS.put("openDate", List.of("open date", "opened", "date opened"));
        COLUMN_PATTERNS.put("storageCondition",
                List.of("storage", "storage condition", "temperature", "temp", "refrigerator"));
        COLUMN_PATTERNS.put("storageLocation", List.of("box", "box no", "box number", "location", "shelf"));
        COLUMN_PATTERNS.put("concentration", List.of("concentration", "conc", "concetration"));
        COLUMN_PATTERNS.put("experimentType", List.of("experiment", "experiment type", "type", "use", "purpose"));
        COLUMN_PATTERNS.put("manufacturer", List.of("manufacturer", "mfr", "vendor", "supplier", "brand"));
        COLUMN_PATTERNS.put("catalogNumber", List.of("catalog", "cat no", "cat#", "sku", "part no"));
        COLUMN_PATTERNS.put("remarks", List.of("remark", "remarks", "notes", "comment", "comments"));
    }

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryStorageLocationService storageLocationService;

    @Override
    public FileFormat detectFileFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".csv")) {
            return FileFormat.CSV;
        } else if (lowerName.endsWith(".xlsx")) {
            return FileFormat.XLSX;
        } else if (lowerName.endsWith(".xls")) {
            return FileFormat.XLS;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file format. Supported formats: CSV, XLSX, XLS. File: " + fileName);
        }
    }

    @Override
    public ParseResult parseFile(InputStream inputStream, String fileName) {
        FileFormat format = detectFileFormat(fileName);
        return parseExcelFile(inputStream, format);
    }

    private ParseResult parseExcelFile(InputStream inputStream, FileFormat format) {
        List<String> sheetNames = new ArrayList<>();
        Map<String, List<String>> headersBySheet = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> rowsBySheet = new LinkedHashMap<>();
        List<String> parseErrors = new ArrayList<>();
        int totalRows = 0;

        try (Workbook workbook = format == FileFormat.XLSX ? new XSSFWorkbook(inputStream)
                : new HSSFWorkbook(inputStream)) {

            int numSheets = workbook.getNumberOfSheets();

            for (int sheetIdx = 0; sheetIdx < numSheets; sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                String sheetName = sheet.getSheetName();
                sheetNames.add(sheetName);

                List<String> headers = new ArrayList<>();
                List<Map<String, String>> rows = new ArrayList<>();

                Iterator<Row> rowIterator = sheet.iterator();
                boolean isFirstRow = true;
                int sheetRowCount = 0;

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    sheetRowCount++;
                    if (sheetRowCount > MAX_ROWS) {
                        parseErrors.add("Sheet '" + sheetName + "' exceeds maximum row limit of " + MAX_ROWS);
                        break;
                    }

                    if (isFirstRow) {
                        // Header row - extract column names
                        for (Cell cell : row) {
                            String headerValue = getCellValueAsString(cell);
                            headers.add(headerValue != null ? headerValue.trim() : "");
                        }
                        isFirstRow = false;
                    } else {
                        // Data row
                        Map<String, String> rowData = new LinkedHashMap<>();
                        boolean hasData = false;

                        for (int i = 0; i < headers.size(); i++) {
                            Cell cell = row.getCell(i);
                            String value = cell != null ? getCellValueAsString(cell) : "";
                            if (value != null && !value.trim().isEmpty()) {
                                hasData = true;
                            }
                            String header = headers.get(i);
                            if (header != null && !header.isEmpty()) {
                                rowData.put(header, value != null ? value.trim() : "");
                            }
                        }

                        // Only add rows that have at least some data
                        if (hasData) {
                            rows.add(rowData);
                            totalRows++;
                        }
                    }
                }

                headersBySheet.put(sheetName, headers);
                rowsBySheet.put(sheetName, rows);
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "parseExcelFile", "Error parsing Excel: " + e.getMessage());
            parseErrors.add("Failed to parse Excel file: " + e.getMessage());
        }

        return new ParseResult(sheetNames, headersBySheet, rowsBySheet, format, totalRows, parseErrors);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        return switch (cellType) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                if (date != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    yield sdf.format(date);
                }
                yield "";
            }
            double value = cell.getNumericCellValue();
            // Check if it's a whole number
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                yield String.valueOf((long) value);
            }
            yield String.valueOf(value);
        }
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case BLANK -> "";
        default -> "";
        };
    }

    @Override
    public Map<String, String> suggestColumnMapping(List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();

        for (String header : headers) {
            if (header == null || header.isEmpty()) {
                continue;
            }
            String lowerHeader = header.toLowerCase().trim();

            for (Map.Entry<String, List<String>> entry : COLUMN_PATTERNS.entrySet()) {
                String fieldName = entry.getKey();
                List<String> patterns = entry.getValue();

                for (String pattern : patterns) {
                    if (lowerHeader.contains(pattern) || pattern.contains(lowerHeader)) {
                        // Only set if not already mapped
                        if (!mapping.containsKey(fieldName)) {
                            mapping.put(fieldName, header);
                        }
                        break;
                    }
                }
            }
        }

        return mapping;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportPreview previewImport(ParseResult parseResult, String selectedSheets, ColumnMapping columnMapping) {
        List<ImportPreviewRow> previewRows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;
        int newItems = 0;
        int existingItems = 0;
        int newLots = 0;
        int totalRows = 0;

        // Support multiple sheets (comma-separated)
        String[] sheetNames = selectedSheets.split(",");

        // Track items we've seen to avoid duplicate counting across sheets
        Map<String, Boolean> seenItems = new HashMap<>();
        // Track lots we've seen
        Map<String, Boolean> seenLots = new HashMap<>();

        for (String sheetName : sheetNames) {
            sheetName = sheetName.trim();
            if (sheetName.isEmpty()) {
                continue;
            }

            // Get rows for this sheet
            List<Map<String, String>> rows = parseResult.rowsBySheet().get(sheetName);
            if (rows == null || rows.isEmpty()) {
                warnings.add("No data rows found in sheet: " + sheetName);
                continue;
            }

            // Use sheet name as category if not specified elsewhere
            String defaultCategory = sheetName;

            int rowNumber = 0;
            for (Map<String, String> rowData : rows) {
                rowNumber++;
                totalRows++;
                List<String> errors = new ArrayList<>();
                List<String> rowWarnings = new ArrayList<>();

                // Map data to standard fields
                Map<String, String> mappedData = mapRowData(rowData, columnMapping);

                String itemName = mappedData.get("itemName");
                String lotNumber = mappedData.get("lotNumber");

                // Validate required fields
                if (itemName == null || itemName.isEmpty()) {
                    errors.add("Item name is required");
                }

                // Check if item exists
                boolean isNewItem = false;
                boolean isNewLot = true;

                if (itemName != null && !itemName.isEmpty()) {
                    String itemKey = itemName.toLowerCase();
                    List<InventoryItem> foundItems = inventoryItemService.searchByName(itemName);
                    InventoryItem matchingItem = findExactMatch(foundItems, itemName);

                    if (matchingItem != null) {
                        if (!seenItems.containsKey(itemKey)) {
                            existingItems++;
                            seenItems.put(itemKey, false);
                        }

                        // Check if lot exists
                        if (lotNumber != null && !lotNumber.isEmpty()) {
                            String lotKey = lotNumber.toLowerCase();
                            if (seenLots.containsKey(lotKey)) {
                                isNewLot = false;
                                rowWarnings.add("Duplicate lot number in file");
                            } else {
                                InventoryLot existingLot = inventoryLotService.getByLotNumber(lotNumber);
                                if (existingLot != null) {
                                    isNewLot = false;
                                    rowWarnings.add("Lot already exists in database");
                                }
                                seenLots.put(lotKey, existingLot == null);
                            }
                        }
                    } else {
                        isNewItem = true;
                        if (!seenItems.containsKey(itemKey)) {
                            newItems++;
                            seenItems.put(itemKey, true);
                        }

                        // For new items, also check lot
                        if (lotNumber != null && !lotNumber.isEmpty()) {
                            String lotKey = lotNumber.toLowerCase();
                            if (seenLots.containsKey(lotKey)) {
                                isNewLot = false;
                                rowWarnings.add("Duplicate lot number in file");
                            } else {
                                InventoryLot existingLot = inventoryLotService.getByLotNumber(lotNumber);
                                if (existingLot != null) {
                                    isNewLot = false;
                                    rowWarnings.add("Lot already exists in database");
                                }
                                seenLots.put(lotKey, existingLot == null);
                            }
                        }
                    }
                }

                // Validate quantity
                String quantityStr = mappedData.get("quantity");
                if (quantityStr != null && !quantityStr.isEmpty()) {
                    try {
                        Double qty = parseQuantity(quantityStr);
                        if (qty != null && qty <= 0) {
                            errors.add("Quantity must be positive");
                        }
                    } catch (Exception e) {
                        errors.add("Invalid quantity format: " + quantityStr);
                    }
                }

                // Validate expiration date
                String expiryStr = mappedData.get("expirationDate");
                if (expiryStr != null && !expiryStr.isEmpty()) {
                    Timestamp expiry = parseDate(expiryStr);
                    if (expiry == null) {
                        rowWarnings.add("Could not parse expiration date: " + expiryStr);
                    } else if (expiry.before(new Timestamp(System.currentTimeMillis()))) {
                        rowWarnings.add("Item is already expired");
                    }
                }

                String status = errors.isEmpty() ? "VALID" : "INVALID";
                if (errors.isEmpty()) {
                    validRows++;
                    if (isNewLot && lotNumber != null && !lotNumber.isEmpty()) {
                        newLots++;
                    }
                } else {
                    invalidRows++;
                }

                previewRows.add(new ImportPreviewRow(rowNumber, sheetName, rowData, mappedData, itemName, lotNumber,
                        status, errors, rowWarnings, isNewItem, isNewLot));
            }
        }

        // Generate suggested mapping from first sheet
        String firstSheet = sheetNames[0].trim();
        List<String> headers = parseResult.headersBySheet().get(firstSheet);
        Map<String, String> suggestedMapping = headers != null ? suggestColumnMapping(headers) : Map.of();

        return new ImportPreview(previewRows, totalRows, validRows, invalidRows, newItems, existingItems, newLots,
                warnings, suggestedMapping);
    }

    private Map<String, String> mapRowData(Map<String, String> rowData, ColumnMapping mapping) {
        Map<String, String> mapped = new LinkedHashMap<>();

        if (mapping.itemName() != null) {
            mapped.put("itemName", rowData.getOrDefault(mapping.itemName(), ""));
        }
        if (mapping.lotNumber() != null) {
            mapped.put("lotNumber", rowData.getOrDefault(mapping.lotNumber(), ""));
        }
        if (mapping.quantity() != null) {
            mapped.put("quantity", rowData.getOrDefault(mapping.quantity(), ""));
        }
        if (mapping.unit() != null) {
            mapped.put("unit", rowData.getOrDefault(mapping.unit(), ""));
        }
        if (mapping.expirationDate() != null) {
            mapped.put("expirationDate", rowData.getOrDefault(mapping.expirationDate(), ""));
        }
        if (mapping.manufacturingDate() != null) {
            mapped.put("manufacturingDate", rowData.getOrDefault(mapping.manufacturingDate(), ""));
        }
        if (mapping.openDate() != null) {
            mapped.put("openDate", rowData.getOrDefault(mapping.openDate(), ""));
        }
        if (mapping.storageCondition() != null) {
            mapped.put("storageCondition", rowData.getOrDefault(mapping.storageCondition(), ""));
        }
        if (mapping.storageLocation() != null) {
            mapped.put("storageLocation", rowData.getOrDefault(mapping.storageLocation(), ""));
        }
        if (mapping.concentration() != null) {
            mapped.put("concentration", rowData.getOrDefault(mapping.concentration(), ""));
        }
        if (mapping.experimentType() != null) {
            mapped.put("experimentType", rowData.getOrDefault(mapping.experimentType(), ""));
        }
        if (mapping.manufacturer() != null) {
            mapped.put("manufacturer", rowData.getOrDefault(mapping.manufacturer(), ""));
        }
        if (mapping.catalogNumber() != null) {
            mapped.put("catalogNumber", rowData.getOrDefault(mapping.catalogNumber(), ""));
        }
        if (mapping.remarks() != null) {
            mapped.put("remarks", rowData.getOrDefault(mapping.remarks(), ""));
        }

        return mapped;
    }

    private InventoryItem findExactMatch(List<InventoryItem> items, String name) {
        if (items == null || name == null) {
            return null;
        }
        String normalizedName = name.trim().toLowerCase();
        for (InventoryItem item : items) {
            if (item.getName() != null && item.getName().trim().toLowerCase().equals(normalizedName)) {
                return item;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public ImportResult executeImport(ParseResult parseResult, String selectedSheets, ColumnMapping columnMapping,
            String userId) {

        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> createdItems = new ArrayList<>();
        List<Map<String, Object>> createdLots = new ArrayList<>();
        int successfulItems = 0;
        int successfulLots = 0;
        int failedRows = 0;
        int totalRows = 0;

        // Support multiple sheets (comma-separated)
        String[] sheetNames = selectedSheets.split(",");

        // Cache for items we create during import (shared across sheets)
        Map<String, InventoryItem> itemCache = new HashMap<>();

        // Cache for storage locations (shared across sheets)
        Map<String, InventoryStorageLocation> locationCache = new HashMap<>();

        // Track created items by name to avoid counting duplicates
        Map<String, Long> createdItemIds = new HashMap<>();

        // Track lot numbers we've already processed
        Map<String, Boolean> processedLots = new HashMap<>();

        for (String sheetName : sheetNames) {
            sheetName = sheetName.trim();
            if (sheetName.isEmpty()) {
                continue;
            }

            // Get rows for this sheet
            List<Map<String, String>> rows = parseResult.rowsBySheet().get(sheetName);
            if (rows == null || rows.isEmpty()) {
                continue;
            }

            // Use sheet name as category
            String defaultCategory = sheetName;

            int rowNumber = 0;
            for (Map<String, String> rowData : rows) {
                rowNumber++;
                totalRows++;

                try {
                    // Map data to standard fields
                    Map<String, String> mappedData = mapRowData(rowData, columnMapping);

                    String itemName = mappedData.get("itemName");
                    String lotNumber = mappedData.get("lotNumber");

                    // Skip rows without item name
                    if (itemName == null || itemName.isEmpty()) {
                        failedRows++;
                        errors.add(Map.of("row", rowNumber, "sheet", sheetName, "error", "Missing item name"));
                        continue;
                    }

                    // Find or create item
                    InventoryItem item = findOrCreateItem(itemName, mappedData, defaultCategory, itemCache, userId);
                    if (item == null) {
                        failedRows++;
                        errors.add(Map.of("row", rowNumber, "sheet", sheetName, "error",
                                "Failed to create/find item: " + itemName));
                        continue;
                    }

                    // Track newly created items (only count once per unique item)
                    String itemKey = itemName.toLowerCase();
                    if (!createdItemIds.containsKey(itemKey) && item.getId() != null) {
                        // Check if this item existed before or was just created
                        if (!itemCache.containsKey(itemKey) || itemCache.get(itemKey).getId() == null) {
                            createdItems.add(Map.of("id", item.getId(), "name", item.getName(), "sheet", sheetName));
                            successfulItems++;
                        }
                        createdItemIds.put(itemKey, item.getId());
                    }
                    itemCache.put(itemKey, item);

                    // Create lot if lot number is provided
                    if (lotNumber != null && !lotNumber.isEmpty()) {
                        String lotKey = lotNumber.toLowerCase();

                        // Skip if we've already processed this lot in this import
                        if (processedLots.containsKey(lotKey)) {
                            errors.add(Map.of("row", rowNumber, "sheet", sheetName, "error",
                                    "Duplicate lot number: " + lotNumber));
                            continue;
                        }
                        processedLots.put(lotKey, true);

                        // Check if lot already exists in database
                        InventoryLot existingLot = inventoryLotService.getByLotNumber(lotNumber);
                        if (existingLot != null) {
                            errors.add(Map.of("row", rowNumber, "sheet", sheetName, "error",
                                    "Lot already exists: " + lotNumber));
                            continue;
                        }

                        // Parse quantity
                        Double quantity = parseQuantity(mappedData.get("quantity"));
                        if (quantity == null) {
                            quantity = 1.0; // Default quantity
                        }

                        // Parse dates
                        Timestamp expirationDate = parseDate(mappedData.get("expirationDate"));
                        Timestamp openDate = parseDate(mappedData.get("openDate"));
                        Timestamp receiptDate = parseDate(mappedData.get("manufacturingDate"));
                        if (receiptDate == null) {
                            receiptDate = new Timestamp(System.currentTimeMillis());
                        }

                        // Find or create storage location
                        InventoryStorageLocation location = null;
                        String locationName = mappedData.get("storageLocation");
                        if (locationName != null && !locationName.isEmpty()) {
                            location = findOrCreateLocation(locationName, locationCache, userId);
                        }

                        // Create lot
                        InventoryLot lot = new InventoryLot();
                        lot.setFhirUuid(UUID.randomUUID());
                        lot.setInventoryItem(item);
                        lot.setLotNumber(lotNumber);
                        lot.setInitialQuantity(quantity);
                        lot.setCurrentQuantity(quantity);
                        lot.setExpirationDate(expirationDate);
                        lot.setReceiptDate(receiptDate);
                        lot.setStorageLocation(location);
                        lot.setSysUserId(userId);

                        // Set status based on expiration
                        if (expirationDate != null
                                && expirationDate.before(new Timestamp(System.currentTimeMillis()))) {
                            lot.setStatus(LotStatus.EXPIRED);
                        } else {
                            lot.setStatus(LotStatus.ACTIVE);
                        }

                        // Default QC status to PASSED for imported items
                        lot.setQcStatus(QCStatus.PASSED);

                        // Handle open date
                        if (openDate != null) {
                            lot.setDateOpened(openDate);
                            lot.setStatus(LotStatus.IN_USE);
                        }

                        Long lotId = inventoryLotService.insert(lot);
                        lot.setId(lotId);

                        createdLots.add(Map.of("id", lotId, "lotNumber", lotNumber, "itemName", item.getName(), "sheet",
                                sheetName));
                        successfulLots++;
                    }

                } catch (Exception e) {
                    failedRows++;
                    errors.add(Map.of("row", rowNumber, "sheet", sheetName, "error",
                            "Error processing row: " + e.getMessage()));
                    LogEvent.logError(this.getClass().getName(), "executeImport",
                            "Error processing row " + rowNumber + " in sheet " + sheetName + ": " + e.getMessage());
                }
            }
        }

        return new ImportResult(totalRows, successfulItems, successfulLots, failedRows, errors, createdItems,
                createdLots);
    }

    private InventoryItem findOrCreateItem(String itemName, Map<String, String> mappedData, String defaultCategory,
            Map<String, InventoryItem> cache, String userId) {

        String cacheKey = itemName.toLowerCase();

        // Check cache first
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // Search in database
        List<InventoryItem> existingItems = inventoryItemService.searchByName(itemName);
        InventoryItem matchingItem = findExactMatch(existingItems, itemName);

        if (matchingItem != null) {
            cache.put(cacheKey, matchingItem);
            return matchingItem;
        }

        // Create new item
        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName(itemName);
        item.setItemType(ItemType.REAGENT);
        item.setCategory(mappedData.getOrDefault("experimentType", defaultCategory));

        // Set unit - default to "vial" if not specified
        String unit = mappedData.get("unit");
        if (unit == null || unit.isEmpty()) {
            unit = "vial";
        }
        item.setUnits(normalizeUnit(unit));

        // Set storage requirements
        String storageCondition = mappedData.get("storageCondition");
        if (storageCondition != null && !storageCondition.isEmpty()) {
            item.setStorageRequirements(normalizeStorageCondition(storageCondition));
        }

        // Set manufacturer and catalog number
        String manufacturer = mappedData.get("manufacturer");
        if (manufacturer != null && !manufacturer.isEmpty()) {
            item.setManufacturer(manufacturer);
        }

        String catalogNumber = mappedData.get("catalogNumber");
        if (catalogNumber != null && !catalogNumber.isEmpty()) {
            item.setCatalogNumber(catalogNumber);
        }

        // Set concentration as dilution notes
        String concentration = mappedData.get("concentration");
        if (concentration != null && !concentration.isEmpty()) {
            item.setDilutionNotes("Concentration: " + concentration);
        }

        // Set description from remarks
        String remarks = mappedData.get("remarks");
        if (remarks != null && !remarks.isEmpty()) {
            item.setDescription(remarks);
        }

        // Set defaults
        item.setLowStockThreshold(5);
        item.setExpirationAlertDays(30);
        item.setStabilityAfterOpening(30); // Default 30 days stability after opening
        item.setIsActive("Y");
        item.setSysUserId(userId);

        Long itemId = inventoryItemService.insert(item);
        item.setId(itemId);

        cache.put(cacheKey, item);
        return item;
    }

    private InventoryStorageLocation findOrCreateLocation(String locationName,
            Map<String, InventoryStorageLocation> cache, String userId) {
        String cacheKey = locationName.toLowerCase();

        // Check cache
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        // Search in database by name
        List<InventoryStorageLocation> allLocations = storageLocationService.getAll();
        for (InventoryStorageLocation loc : allLocations) {
            if (loc.getName() != null && loc.getName().equalsIgnoreCase(locationName)) {
                cache.put(cacheKey, loc);
                return loc;
            }
        }

        // Create new location
        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setFhirUuid(UUID.randomUUID());
        location.setName(locationName);
        location.setLocationType(LocationType.CABINET); // Default type for boxes
        location.setIsActive(true);
        location.setSysUserId(userId);

        Long locationId = storageLocationService.insert(location);
        location.setId(locationId);

        cache.put(cacheKey, location);
        return location;
    }

    private Double parseQuantity(String quantityStr) {
        if (quantityStr == null || quantityStr.isEmpty()) {
            return null;
        }

        // Clean up the string
        String cleaned = quantityStr.trim();

        // Handle cases like "4+1-opened" -> take the first number
        Pattern pattern = Pattern.compile("^(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(cleaned);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Try direct parse
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Timestamp parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "No".equalsIgnoreCase(dateStr)) {
            return null;
        }

        // List of date formats to try
        String[] formats = { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd",
                "dd-MM-yyyy", "yyyy/MM", "yyyy-MM" };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr.trim());
                return new Timestamp(date.getTime());
            } catch (ParseException e) {
                // Try next format
            }
        }

        // Try to extract year-month pattern like "2022/10/"
        Pattern pattern = Pattern.compile("(\\d{4})/(\\d{1,2})/?");
        Matcher matcher = pattern.matcher(dateStr);
        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date date = sdf.parse(year + "-" + String.format("%02d", month) + "-01");
                return new Timestamp(date.getTime());
            } catch (Exception e) {
                // Ignore
            }
        }

        return null;
    }

    private String normalizeUnit(String unit) {
        if (unit == null) {
            return "vial";
        }

        String lower = unit.toLowerCase().trim();

        if (lower.contains("vial")) {
            return "vials";
        }
        if (lower.contains("ml") || lower.contains("milliliter")) {
            return "ml";
        }
        if (lower.contains("ul") || lower.contains("microliter")) {
            return "ul";
        }
        if (lower.contains("l") || lower.contains("liter")) {
            return "liters";
        }
        if (lower.contains("pack") || lower.contains("pkg")) {
            return "packs";
        }
        if (lower.contains("kit")) {
            return "kits";
        }
        if (lower.contains("tube")) {
            return "tubes";
        }
        if (lower.contains("bottle")) {
            return "bottles";
        }
        if (lower.contains("test")) {
            return "tests";
        }
        if (lower.contains("reaction")) {
            return "reactions";
        }

        // Default to the original if no match
        return unit;
    }

    private String normalizeStorageCondition(String condition) {
        if (condition == null) {
            return null;
        }

        String lower = condition.toLowerCase().trim();

        // Check for temperature patterns
        if (lower.contains("-80") || lower.contains("-80°")) {
            return "-80°C (Ultra-Low Freezer)";
        }
        if (lower.contains("-20") || lower.contains("-20°") || lower.contains("frozen") || lower.contains("freezer")) {
            return "-20°C (Frozen)";
        }
        if (lower.contains("2-8") || lower.contains("4°") || lower.contains("refrigerat") || lower.contains("fridge")) {
            return "2-8°C (Refrigerated)";
        }
        if (lower.contains("15-25") || lower.contains("15-30") || lower.contains("room") || lower.contains("ambient")) {
            return "15-25°C (Room Temperature)";
        }

        // Return original if no pattern match
        return condition;
    }
}
