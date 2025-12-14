# Quickstart Guide: Inventory Management Reporting Backend

**Feature**: 001-inventory-reporting
**Estimated Time**: 7-8 days across 4 milestones
**Prerequisites**: OpenELIS development environment setup complete

## Overview

This guide provides step-by-step implementation instructions for the inventory reporting backend feature across 4 milestones. Follow the TDD (Red-Green-Refactor) workflow at each step.

## Milestone 1: Backend Foundation (Days 1-2)

**Goal**: Create report bean DTOs, service interface, and DAO queries for data aggregation.

**Branch**: `feat/001-inventory-reporting-m1-backend-foundation` (from `develop`)

### Step 1.1: Create Report Bean DTOs

1. Create directory: `src/main/java/org/openelisglobal/inventory/valueholder/reports/`
2. Create 6 bean classes (see data-model.md for field definitions):
   - `StockLevelReportData.java`
   - `LowStockReportData.java`
   - `ExpirationForecastReportData.java`
   - `TransactionHistoryReportData.java`
   - `UsageTrendReportData.java`
   - `LotTraceabilityReportData.java`

**TDD Cycle**:
- RED: Write unit test validating bean getters → FAIL (no class)
- GREEN: Create bean with all fields and getters → PASS
- REFACTOR: Add JavaDoc, ensure consistent field naming

### Step 1.2: Create Service Interface

1. Create `src/main/java/org/openelisglobal/inventory/service/InventoryReportService.java`
2. Define methods:
   ```java
   List<StockLevelReportData> generateStockLevelData(boolean includeInactive, 
       boolean groupByType, boolean groupByLocation);
   List<LowStockReportData> generateLowStockData(boolean includeInactive);
   List<ExpirationForecastReportData> generateExpirationForecastData(
       LocalDate startDate, LocalDate endDate, boolean includeExpired);
   // ... (all 6 report types)
   ```

### Step 1.3: Implement Service

1. Create `src/main/java/org/openelisglobal/inventory/service/InventoryReportServiceImpl.java`
2. Annotate with `@Service` and `@Transactional(readOnly = true)`
3. Inject existing DAOs: InventoryItemDAO, InventoryLotDAO, etc.

**TDD Cycle** (for each report type):
- RED: Write unit test for data aggregation → FAIL (no implementation)
- GREEN: Implement aggregation logic using DAO queries → PASS
- REFACTOR: Extract common patterns, optimize queries

**Example**: Stock Levels aggregation
```java
@Override
public List<StockLevelReportData> generateStockLevelData(
    boolean includeInactive, boolean groupByType, boolean groupByLocation) {
    
    List<InventoryItem> items = includeInactive 
        ? inventoryItemDAO.getAll() 
        : inventoryItemDAO.getAllActive();
    
    return items.stream().map(item -> {
        StockLevelReportData data = new StockLevelReportData();
        data.setItemId(item.getId());
        data.setItemName(item.getName());
        
        // Calculate total quantity across all active lots
        BigDecimal totalQty = inventoryLotDAO.getAvailableByItem(item.getId())
            .stream()
            .map(InventoryLot::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.setTotalQuantity(totalQty);
        
        // ... populate remaining fields
        return data;
    }).collect(Collectors.toList());
}
```

### Step 1.4: Write Unit Tests

1. Create `src/test/java/org/openelisglobal/inventory/service/InventoryReportServiceTest.java`
2. Use `@RunWith(MockitoJUnitRunner.class)` for isolated tests
3. Mock DAOs with `@Mock` annotations
4. Test each report type's data aggregation logic

**Verification**: `mvn test` - Unit tests pass (>80% coverage for service layer)

**Commit**: "M1: Add report beans, service interface/impl, unit tests"

---

## Milestone 2: PDF Generation (Days 3-4)

**Goal**: Implement JasperReports integration for all 6 report types with JRXML templates.

**Branch**: `feat/001-inventory-reporting-m2-pdf-generation` (from `develop`)

### Step 2.1: Create JRXML Templates

1. Use Jaspersoft Studio (or text editor) to create templates in `src/main/resources/reports/`
2. Start with simplest template (Stock Levels), then duplicate/modify for others

**Template Structure** (example: InventoryStockLevelsReport.jrxml):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jasperReport>
  <parameter name="facilityName" class="java.lang.String"/>
  <parameter name="reportDate" class="java.lang.String"/>
  
  <field name="itemName" class="java.lang.String"/>
  <field name="totalQuantity" class="java.math.BigDecimal"/>
  <field name="unitOfMeasure" class="java.lang.String"/>
  <field name="storageLocationPath" class="java.lang.String"/>
  
  <!-- Bands: Title, Column Headers, Detail, Page Footer -->
</jasperReport>
```

### Step 2.2: Implement PDF Generation Methods

Add methods to `InventoryReportServiceImpl`:
```java
public byte[] generatePdfReport(String reportType, LocalDate startDate, 
    LocalDate endDate, boolean includeInactive, boolean includeExpired, 
    boolean groupByType, boolean groupByLocation) throws Exception {
    
    // 1. Generate data
    List<?> dataList = getReportData(reportType, ...);
    
    // 2. Build parameters
    Map<String, Object> params = buildJasperParameters(reportType);
    
    // 3. Create data source
    JRBeanCollectionDataSource dataSource = 
        new JRBeanCollectionDataSource(dataList);
    
    // 4. Load template and generate PDF
    String templatePath = "reports/Inventory" + reportType + "Report.jasper";
    File reportFile = new ClassPathResource(templatePath).getFile();
    
    return JasperRunManager.runReportToPdf(
        reportFile.getAbsolutePath(), params, dataSource);
}

private Map<String, Object> buildJasperParameters(String reportType) {
    Map<String, Object> params = new HashMap<>();
    params.put("facilityName", configService.getSiteName());
    params.put("reportDate", LocalDate.now().toString());
    params.put("reportType", reportType);
    // ... add standard parameters
    return params;
}
```

### Step 2.3: Write Integration Tests

1. Create `src/test/java/org/openelisglobal/inventory/service/InventoryReportServicePdfTest.java`
2. Use `BaseWebContextSensitiveTest` for full context
3. Test PDF generation for all 6 report types

**Example Test**:
```java
@Test
public void testGenerateStockLevelsPdfReport() throws Exception {
    byte[] pdfBytes = service.generatePdfReport(
        "STOCK_LEVELS", null, null, false, true, false, false);
    
    assertNotNull("PDF bytes should not be null", pdfBytes);
    assertTrue("PDF should have content", pdfBytes.length > 0);
    
    // Verify PDF header (starts with "%PDF-")
    String header = new String(pdfBytes, 0, 4, StandardCharsets.UTF_8);
    assertEquals("%PDF", header);
}
```

**Verification**: `mvn test -Dtest=InventoryReportServicePdfTest` - All PDF tests pass

**Commit**: "M2: Add JasperReports integration and JRXML templates for all report types"

---

## Milestone 3: CSV/Excel Generation (Days 5-6)

**Goal**: Implement CSV and Excel export services for all 6 report types.

**Branch**: `feat/001-inventory-reporting-m3-csv-excel-generation` (from `develop`)

**Note**: M3 can be developed in parallel with M2 after M1 completes.

### Step 3.1: Implement CSV Export

Add method to `InventoryReportServiceImpl`:
```java
public byte[] generateCsvReport(String reportType, ...) {
    List<?> dataList = getReportData(reportType, ...);
    
    StringBuilder csv = new StringBuilder();
    csv.append(getCsvHeader(reportType)).append("\n");
    
    for (Object data : dataList) {
        csv.append(toCsvRow(data, reportType)).append("\n");
    }
    
    return csv.toString().getBytes(StandardCharsets.UTF_8);
}

private String getCsvHeader(String reportType) {
    switch (reportType) {
        case "STOCK_LEVELS":
            return "Item Name,Type,Location,Quantity,Unit,Reorder Level,Status";
        // ... other report types
    }
}

private String toCsvRow(Object data, String reportType) {
    if (data instanceof StockLevelReportData) {
        StockLevelReportData stock = (StockLevelReportData) data;
        return String.format("%s,%s,%s,%s,%s,%s,%s",
            escapeCsv(stock.getItemName()),
            escapeCsv(stock.getItemType()),
            escapeCsv(stock.getStorageLocationPath()),
            stock.getTotalQuantity(),
            escapeCsv(stock.getUnitOfMeasure()),
            stock.getReorderLevel(),
            stock.getIsBelowReorderLevel() ? "LOW" : "OK");
    }
    // ... other bean types
}

private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

### Step 3.2: Implement Excel Export

Add method using Apache POI HSSF:
```java
public byte[] generateExcelReport(String reportType, ...) throws IOException {
    List<?> dataList = getReportData(reportType, ...);
    
    HSSFWorkbook workbook = new HSSFWorkbook();
    HSSFSheet sheet = workbook.createSheet(reportType);
    
    // Create header row
    Row headerRow = sheet.createRow(0);
    String[] headers = getExcelHeaders(reportType);
    for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
    }
    
    // Create data rows
    int rowNum = 1;
    for (Object data : dataList) {
        Row row = sheet.createRow(rowNum++);
        populateExcelRow(row, data, reportType);
    }
    
    // Auto-size columns
    for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
    }
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    workbook.close();
    
    return out.toByteArray();
}
```

### Step 3.3: Write Integration Tests

Test CSV and Excel generation for all 6 report types.

**Verification**: `mvn test -Dtest=InventoryReportServiceCsvExcelTest` - All tests pass

**Commit**: "M3: Add CSV and Excel export for all report types"

---

## Milestone 4: REST API Integration (Days 7-8)

**Goal**: Create REST controller, integrate with frontend, add E2E tests.

**Branch**: `feat/001-inventory-reporting-m4-rest-api-integration` (from `develop`)

### Step 4.1: Create REST Controller

1. Create `src/main/java/org/openelisglobal/inventory/controller/rest/InventoryReportRestController.java`

```java
@RestController
@RequestMapping("/rest/inventory/reports")
public class InventoryReportRestController extends BaseRestController {
    
    @Autowired
    private InventoryReportService reportService;
    
    @PostMapping("/generate")
    public void generateReport(
        @RequestParam String reportType,
        @RequestParam String exportFormat,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "false") boolean includeInactive,
        @RequestParam(defaultValue = "true") includeExpired,
        @RequestParam(defaultValue = "false") boolean groupByType,
        @RequestParam(defaultValue = "false") boolean groupByLocation,
        HttpServletResponse response
    ) {
        try {
            // Validation
            validateRequest(reportType, exportFormat, startDate, endDate);
            
            // Generate report
            byte[] reportBytes = reportService.generateReport(
                reportType, exportFormat, startDate, endDate,
                includeInactive, includeExpired, groupByType, groupByLocation);
            
            // Set headers
            String contentType = getContentType(exportFormat);
            String filename = generateFilename(reportType, exportFormat);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + filename + "\"");
            response.setContentLength(reportBytes.length);
            
            // Write bytes
            response.getOutputStream().write(reportBytes);
            response.getOutputStream().flush();
            
            // Audit logging
            auditReportGeneration(reportType, exportFormat);
            
        } catch (IllegalArgumentException e) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateReport", e.getMessage());
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to generate report");
        }
    }
}
```

### Step 4.2: Write Controller Integration Tests

Test REST endpoints with `MockMvc`:
```java
@Test
public void testGenerateStockLevelsReport() throws Exception {
    mockMvc.perform(post("/rest/inventory/reports/generate")
            .param("reportType", "STOCK_LEVELS")
            .param("exportFormat", "PDF"))
        .andExpect(status().isOk())
        .andExpect(header().exists("Content-Disposition"))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE));
}
```

### Step 4.3: Write E2E Tests (Cypress)

1. Create `frontend/cypress/e2e/inventoryReports.cy.js`
2. Test all 18 workflows (6 reports × 3 formats)

**Example**:
```javascript
describe('Inventory Reports', () => {
  before(() => {
    cy.session('admin', () => {
      cy.login('admin', 'password');
    }, { cacheAcrossSpecs: true });
  });

  it('should generate Stock Levels PDF report', () => {
    cy.visit('/inventory-reports');
    cy.get('[data-testid="report-type-dropdown"]').select('Stock Levels');
    cy.get('[data-testid="export-format-dropdown"]').select('PDF');
    cy.get('[data-testid="generate-button"]').click();
    
    // Verify download (file appears in downloads folder)
    cy.wait('@generateReport').its('response.statusCode').should('eq', 200);
    
    // Verify no console errors
    cy.window().then((win) => {
      expect(win.console.error).to.not.be.called;
    });
  });
  
  // Repeat for all 18 combinations...
});
```

### Step 4.4: Frontend Integration Verification

1. Start backend: `docker compose -f dev.docker-compose.yml up -d`
2. Navigate to https://localhost/ 
3. Go to Inventory Reports page
4. Test each report type × format combination manually
5. Verify files download correctly with proper filenames

**Verification**: All E2E tests pass, manual testing confirms functionality

**Commit**: "M4: Add REST controller, E2E tests, complete frontend integration"

---

## Final Checklist

Before creating PR for each milestone:

- [ ] All unit tests pass (`mvn test`)
- [ ] Code coverage >80% (run `mvn jacoco:report`, check `target/site/jacoco/index.html`)
- [ ] Code formatted (`mvn spotless:apply`)
- [ ] No `@Transactional` on controllers (Constitution violation check)
- [ ] Services compile all data within transaction
- [ ] E2E tests pass (M4 only): `npm run cy:run -- --spec "cypress/e2e/inventoryReports.cy.js"`
- [ ] Browser console logs reviewed (no JavaScript errors)
- [ ] Manual testing completed for all report types

## Next Steps

1. Create PR for M1: `feat/001-inventory-reporting-m1-backend-foundation` → `develop`
2. After M1 approved, create M2 and M3 PRs (can be parallel)
3. After M2 and M3 approved, create M4 PR
4. Final integration testing across all milestones
