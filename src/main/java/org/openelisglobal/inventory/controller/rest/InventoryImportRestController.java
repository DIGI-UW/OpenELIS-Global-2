package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryImportService;
import org.openelisglobal.inventory.service.InventoryImportService.ColumnMapping;
import org.openelisglobal.inventory.service.InventoryImportService.ImportPreview;
import org.openelisglobal.inventory.service.InventoryImportService.ImportResult;
import org.openelisglobal.inventory.service.InventoryImportService.ParseResult;
import org.openelisglobal.systemuser.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for inventory import operations. Handles Excel/CSV file
 * upload, parsing, preview, and import execution.
 */
@RestController
@RequestMapping("/rest/inventory/import")
public class InventoryImportRestController extends BaseRestController {

    @Autowired
    private InventoryImportService importService;

    @Autowired
    private UserService userService;

    /**
     * Upload and parse an Excel/CSV file. Returns the parsed structure including
     * sheet names, headers, and row counts.
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            String fileName = file.getOriginalFilename();
            LogEvent.logInfo(this.getClass().getName(), "parseFile", "Parsing file: " + fileName);

            ParseResult parseResult = importService.parseFile(file.getInputStream(), fileName);

            // Build response with summary info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("format", parseResult.format().name());
            response.put("sheetNames", parseResult.sheetNames());
            response.put("totalRows", parseResult.totalRows());

            // Include headers for each sheet
            Map<String, Object> sheetsInfo = new HashMap<>();
            for (String sheetName : parseResult.sheetNames()) {
                List<String> headers = parseResult.headersBySheet().get(sheetName);
                List<Map<String, String>> rows = parseResult.rowsBySheet().get(sheetName);
                int rowCount = rows != null ? rows.size() : 0;

                Map<String, Object> sheetInfo = new HashMap<>();
                sheetInfo.put("headers", headers);
                sheetInfo.put("rowCount", rowCount);

                // Include suggested column mapping
                Map<String, String> suggestedMapping = importService.suggestColumnMapping(headers);
                sheetInfo.put("suggestedMapping", suggestedMapping);

                // Include sample rows (first 5)
                if (rows != null && !rows.isEmpty()) {
                    int sampleSize = Math.min(5, rows.size());
                    sheetInfo.put("sampleRows", rows.subList(0, sampleSize));
                }

                sheetsInfo.put(sheetName, sheetInfo);
            }
            response.put("sheets", sheetsInfo);

            if (!parseResult.parseErrors().isEmpty()) {
                response.put("warnings", parseResult.parseErrors());
            }

            // Store parse result in session for preview/import steps
            // In a production app, you'd want to use a more robust storage mechanism
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getName(), "parseFile", "Invalid file: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "parseFile", "Error parsing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to parse file: " + e.getMessage()));
        }
    }

    /**
     * Preview import with column mapping. Returns validation results and what will
     * be created.
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewImport(@RequestParam("file") MultipartFile file,
            @RequestParam("sheet") String selectedSheet,
            @RequestParam(value = "itemName", required = false) String itemNameColumn,
            @RequestParam(value = "lotNumber", required = false) String lotNumberColumn,
            @RequestParam(value = "quantity", required = false) String quantityColumn,
            @RequestParam(value = "unit", required = false) String unitColumn,
            @RequestParam(value = "expirationDate", required = false) String expirationDateColumn,
            @RequestParam(value = "manufacturingDate", required = false) String manufacturingDateColumn,
            @RequestParam(value = "openDate", required = false) String openDateColumn,
            @RequestParam(value = "storageCondition", required = false) String storageConditionColumn,
            @RequestParam(value = "storageLocation", required = false) String storageLocationColumn,
            @RequestParam(value = "concentration", required = false) String concentrationColumn,
            @RequestParam(value = "experimentType", required = false) String experimentTypeColumn,
            @RequestParam(value = "manufacturer", required = false) String manufacturerColumn,
            @RequestParam(value = "catalogNumber", required = false) String catalogNumberColumn,
            @RequestParam(value = "remarks", required = false) String remarksColumn) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            String fileName = file.getOriginalFilename();
            LogEvent.logInfo(this.getClass().getName(), "previewImport",
                    "Preview import for file: " + fileName + ", sheet: " + selectedSheet);

            // Parse the file
            ParseResult parseResult = importService.parseFile(file.getInputStream(), fileName);

            // Build column mapping
            ColumnMapping columnMapping = new ColumnMapping(itemNameColumn, lotNumberColumn, quantityColumn, unitColumn,
                    expirationDateColumn, manufacturingDateColumn, openDateColumn, storageConditionColumn,
                    storageLocationColumn, concentrationColumn, experimentTypeColumn, manufacturerColumn,
                    catalogNumberColumn, remarksColumn);

            // Generate preview
            ImportPreview preview = importService.previewImport(parseResult, selectedSheet, columnMapping);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRows", preview.totalRows());
            response.put("validRows", preview.validRows());
            response.put("invalidRows", preview.invalidRows());
            response.put("newItems", preview.newItems());
            response.put("existingItems", preview.existingItems());
            response.put("newLots", preview.newLots());
            response.put("warnings", preview.warnings());
            response.put("rows", preview.rows());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "previewImport", "Error previewing import: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to preview import: " + e.getMessage()));
        }
    }

    /**
     * Execute the import.
     */
    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> executeImport(@RequestParam("file") MultipartFile file,
            @RequestParam("sheet") String selectedSheet,
            @RequestParam(value = "itemName", required = false) String itemNameColumn,
            @RequestParam(value = "lotNumber", required = false) String lotNumberColumn,
            @RequestParam(value = "quantity", required = false) String quantityColumn,
            @RequestParam(value = "unit", required = false) String unitColumn,
            @RequestParam(value = "expirationDate", required = false) String expirationDateColumn,
            @RequestParam(value = "manufacturingDate", required = false) String manufacturingDateColumn,
            @RequestParam(value = "openDate", required = false) String openDateColumn,
            @RequestParam(value = "storageCondition", required = false) String storageConditionColumn,
            @RequestParam(value = "storageLocation", required = false) String storageLocationColumn,
            @RequestParam(value = "concentration", required = false) String concentrationColumn,
            @RequestParam(value = "experimentType", required = false) String experimentTypeColumn,
            @RequestParam(value = "manufacturer", required = false) String manufacturerColumn,
            @RequestParam(value = "catalogNumber", required = false) String catalogNumberColumn,
            @RequestParam(value = "remarks", required = false) String remarksColumn, HttpServletRequest request) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            String fileName = file.getOriginalFilename();
            String userId = getSysUserId(request);

            LogEvent.logInfo(this.getClass().getName(), "executeImport",
                    "Executing import for file: " + fileName + ", sheet: " + selectedSheet + ", user: " + userId);

            // Parse the file
            ParseResult parseResult = importService.parseFile(file.getInputStream(), fileName);

            // Build column mapping
            ColumnMapping columnMapping = new ColumnMapping(itemNameColumn, lotNumberColumn, quantityColumn, unitColumn,
                    expirationDateColumn, manufacturingDateColumn, openDateColumn, storageConditionColumn,
                    storageLocationColumn, concentrationColumn, experimentTypeColumn, manufacturerColumn,
                    catalogNumberColumn, remarksColumn);

            // Execute import
            ImportResult result = importService.executeImport(parseResult, selectedSheet, columnMapping, userId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRows", result.totalRows());
            response.put("successfulItems", result.successfulItems());
            response.put("successfulLots", result.successfulLots());
            response.put("failedRows", result.failedRows());
            response.put("createdItems", result.createdItems());
            response.put("createdLots", result.createdLots());

            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors());
            }

            LogEvent.logInfo(this.getClass().getName(), "executeImport",
                    "Import completed. Items: " + result.successfulItems() + ", Lots: " + result.successfulLots()
                            + ", Failed: " + result.failedRows());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "executeImport", "Error executing import: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to execute import: " + e.getMessage()));
        }
    }

    /**
     * DTO for column mapping in JSON requests
     */
    public static class ColumnMappingDTO {

        private String itemName;
        private String lotNumber;
        private String quantity;
        private String unit;
        private String expirationDate;
        private String manufacturingDate;
        private String openDate;
        private String storageCondition;
        private String storageLocation;
        private String concentration;
        private String experimentType;
        private String manufacturer;
        private String catalogNumber;
        private String remarks;

        // Getters and setters
        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getLotNumber() {
            return lotNumber;
        }

        public void setLotNumber(String lotNumber) {
            this.lotNumber = lotNumber;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(String expirationDate) {
            this.expirationDate = expirationDate;
        }

        public String getManufacturingDate() {
            return manufacturingDate;
        }

        public void setManufacturingDate(String manufacturingDate) {
            this.manufacturingDate = manufacturingDate;
        }

        public String getOpenDate() {
            return openDate;
        }

        public void setOpenDate(String openDate) {
            this.openDate = openDate;
        }

        public String getStorageCondition() {
            return storageCondition;
        }

        public void setStorageCondition(String storageCondition) {
            this.storageCondition = storageCondition;
        }

        public String getStorageLocation() {
            return storageLocation;
        }

        public void setStorageLocation(String storageLocation) {
            this.storageLocation = storageLocation;
        }

        public String getConcentration() {
            return concentration;
        }

        public void setConcentration(String concentration) {
            this.concentration = concentration;
        }

        public String getExperimentType() {
            return experimentType;
        }

        public void setExperimentType(String experimentType) {
            this.experimentType = experimentType;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getCatalogNumber() {
            return catalogNumber;
        }

        public void setCatalogNumber(String catalogNumber) {
            this.catalogNumber = catalogNumber;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public ColumnMapping toColumnMapping() {
            return new ColumnMapping(itemName, lotNumber, quantity, unit, expirationDate, manufacturingDate, openDate,
                    storageCondition, storageLocation, concentration, experimentType, manufacturer, catalogNumber,
                    remarks);
        }
    }
}
