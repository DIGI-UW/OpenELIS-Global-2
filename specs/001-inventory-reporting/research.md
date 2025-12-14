# Research: Inventory Management Reporting Backend

**Feature**: 001-inventory-reporting
**Date**: 2025-12-14
**Status**: Complete

## Research Summary

This document consolidates research findings for implementing the inventory reporting backend using the existing OpenELIS reporting framework.

## Key Decisions

### 1. Report Generation Framework

**Decision**: Use existing JasperReports integration via Report.java base class

**Rationale**:
- Proven pattern used by FreezerReportServiceImpl and multiple report implementations
- Provides standard report parameters (directorName, siteName, headers)
- Integrates with Maven Jasper plugin for template compilation
- Supports internationalization via resource bundles

**Pattern** (from Report.java):
```java
byte[] pdfBytes = JasperRunManager.runReportToPdf(
    reportFile.getAbsolutePath(),  // Path to compiled .jasper file
    createReportParameters(),       // HashMap with standard + custom params
    getReportDataSource()          // JRBeanCollectionDataSource
);
```

### 2. Excel Export Format

**Decision**: Use Apache POI HSSF for .xls format (NOT .xlsx)

**Rationale**:
- Only `org.apache.poi:poi:5.4.0` is in dependencies (HSSF for .xls)
- `poi-ooxml` (required for .xlsx/XSSF) is NOT in pom.xml
- Adding poi-ooxml would require dependency approval and testing
- .xls format is sufficient for report data (max 65,536 rows, adequate for typical datasets)

**Alternative Considered**: Add poi-ooxml dependency for .xlsx support
**Rejected Because**: Adds dependency complexity, requires Maven build changes, .xls meets requirements

### 3. CSV Export Approach

**Decision**: Manual CSV building using StringBuilder (NOT JasperReports)

**Rationale**:
- CSVExportReport pattern extends Report but does NOT use JasperReports for data
- Simpler and faster than configuring Jasper for CSV export
- Full control over CSV formatting, escaping, and character encoding
- Proven pattern in HaitiLNSPExportReport implementation

**Pattern** (from HaitiLNSPExportReport.java):
```java
StringBuilder builder = new StringBuilder();
builder.append(CSVHeader).append("\n");
for (DataBean bean : dataList) {
    builder.append(bean.getAsCSVString()).append("\n");
}
return builder.toString().getBytes(StandardCharsets.UTF_8);
```

### 4. REST Controller Response Pattern

**Decision**: Write blob directly to HttpServletResponse (NOT ResponseEntity)

**Rationale**:
- Proven pattern in FreezerReportController and PrintWorkplanReportRestController
- Direct control over headers (Content-Type, Content-Disposition, Content-Length)
- Better error handling when response is partially written
- Supports large file streaming without buffering entire blob

**Pattern** (from FreezerReportController.java):
```java
@PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
public void generateReport(@RequestBody Request req, HttpServletResponse resp) {
    byte[] bytes = service.generate(...);
    resp.setContentType(MediaType.APPLICATION_PDF_VALUE);
    resp.setHeader("Content-Disposition", "attachment; filename=\"...\"");
    resp.setContentLength(bytes.length);
    resp.getOutputStream().write(bytes);
    resp.getOutputStream().flush();
}
```

### 5. Data Source Pattern

**Decision**: Use JRBeanCollectionDataSource with simple POJOs

**Rationale**:
- Standard JasperReports pattern for bean collections
- Simple POJOs with getters match JRXML field definitions
- No SQL in JRXML (data pre-aggregated by service layer)
- Type-safe compile-time checking of field names

**Pattern** (from FreezerReportServiceImpl.java):
```java
List<ReportDataBean> dataList = generateReportData(...);
JRBeanCollectionDataSource dataSource =
    new JRBeanCollectionDataSource(dataList);
```

### 6. Service Layer Transaction Management

**Decision**: Use `@Transactional(readOnly = true)` on service methods

**Rationale**:
- Reports are read-only operations (no data modifications)
- Ensures consistent data snapshot during report generation
- Prevents LazyInitializationException by keeping session open during data compilation
- Follows FreezerReportServiceImpl pattern

**Critical**: Controllers MUST NOT have `@Transactional` annotations (Constitution violation)

### 7. JRXML Template Structure

**Decision**: Create 6 separate JRXML templates (one per report type)

**Rationale**:
- Cleaner separation of concerns than conditional logic in single template
- Easier to maintain and customize per report type
- Follows existing OpenELIS pattern (separate templates per report)
- Simplifies parameter management (only relevant params per template)

**Templates** (to be created in src/main/resources/reports/):
- InventoryStockLevelsReport.jrxml
- InventoryLowStockReport.jrxml
- InventoryExpirationForecastReport.jrxml
- InventoryTransactionHistoryReport.jrxml
- InventoryUsageTrendsReport.jrxml
- InventoryLotTraceabilityReport.jrxml

## Alternatives Considered

### Alternative 1: Spring Boot @WebMvcTest for Controllers

**Rejected Because**: This repo uses Traditional Spring MVC (NOT Spring Boot). Use `BaseWebContextSensitiveTest` + `MockMvc` instead.

### Alternative 2: Apache POI Streaming API (SXSSF)

**Rejected Because**: Requires poi-ooxml dependency, adds complexity for large datasets. Current approach buffers data in memory, adequate for expected dataset sizes (<10,000 items).

### Alternative 3: Asynchronous Report Generation

**Rejected Because**: Specification requires synchronous reports (user waits for download). Async queuing adds complexity, database tables for job tracking, and polling UI requirements.

### Alternative 4: Single REST Endpoint Per Report Type

**Rejected Because**: Creates 6 endpoints instead of 1 parameterized endpoint. Frontend already implemented with single endpoint expecting `reportType` parameter.

## Best Practices from Codebase

### Report Path Resolution

Use ClassPathResource for consistent resource loading:
```java
File reportFile = new ClassPathResource(
    "reports/" + reportFileName + ".jasper"
).getFile();
```

### Error Handling Pattern

Check response.isCommitted() before sending errors:
```java
try {
    // Generate report...
} catch (Exception e) {
    LogEvent.logError(this.getClass().getSimpleName(),
        "generateReport", e.getMessage());
    if (!response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            e.getMessage());
    }
}
```

### Content-Disposition Filename

Use format with timestamp for unique filenames:
```java
String filename = String.format("inventory-report-%s-%s.%s",
    reportType.toLowerCase(),
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
    extension);
response.setHeader("Content-Disposition",
    "attachment; filename=\"" + filename + "\"");
```

### CSV Character Escaping

Escape quotes and commas in CSV cells:
```java
private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

## Technical Constraints

1. **Java Version**: Java 21 LTS (MANDATORY) - Build fails with Java 8/11/17
2. **JUnit Version**: JUnit 4.13.1 (NOT JUnit 5) - Use org.junit.Test
3. **Jakarta EE**: Use jakarta.persistence (NOT javax.persistence)
4. **Apache POI**: Only HSSF available (5.4.0), NOT XSSF/poi-ooxml
5. **Transaction Management**: Service layer ONLY, NO @Transactional on controllers

## Implementation Risks & Mitigations

### Risk 1: Large Dataset Memory Consumption

**Risk**: Loading 100,000 transactions into memory for report generation could cause OutOfMemoryError

**Mitigation**:
- Implement pagination warning in service layer (warn if >10,000 records requested)
- Use streaming approach for very large datasets (iterator instead of List)
- Document recommended date range limits in user documentation

### Risk 2: JRXML Compilation Errors

**Risk**: JRXML syntax errors only discovered at Maven compile time, slowing TDD cycle

**Mitigation**:
- Use Jaspersoft Studio for JRXML design (validates syntax in real-time)
- Start with simple template, incrementally add complexity
- Keep JRXML parameters minimal (complex logic in service layer)

### Risk 3: Frontend-Backend Contract Mismatch

**Risk**: Backend returns different blob format than frontend expects

**Mitigation**:
- Match exact headers expected by frontend (Content-Type, Content-Disposition)
- Test with actual frontend (E2E tests validate full integration)
- Document exact API contract in openapi.yaml

### Risk 4: Timezone Handling Inconsistency

**Risk**: Date range filters use different timezone than user expects

**Mitigation**:
- Use server timezone consistently (document in spec.md assumptions)
- Format dates in reports using standard ISO 8601 with timezone indicator
- Test with various date ranges across month/year boundaries

## References

### Key Files Studied

**Service Layer Patterns**:
- `/src/main/java/org/openelisglobal/coldstorage/service/FreezerReportServiceImpl.java` - Best reference for report service implementation

**Controller Patterns**:
- `/src/main/java/org/openelisglobal/coldstorage/controller/FreezerReportController.java` - Blob response pattern
- `/src/main/java/org/openelisglobal/workplan/controller/rest/PrintWorkplanReportRestController.java` - POST endpoint pattern

**CSV Export Patterns**:
- `/src/main/java/org/openelisglobal/reports/action/implementation/HaitiLNSPExportReport.java` - Manual CSV building
- `/src/main/java/org/openelisglobal/reports/action/implementation/reportBeans/CSVColumnBuilder.java` - CSV column builder

**JasperReports Patterns**:
- `/src/main/java/org/openelisglobal/reports/action/implementation/Report.java` - Base report class
- `/src/main/resources/reports/FreezerTemperatureMonitoringReport.jrxml` - JRXML template example

**Excel Patterns**:
- `/src/main/java/org/openelisglobal/genericsample/service/GenericSampleOrderServiceImpl.java:1528-1573` - Excel reading with Apache POI HSSF

### External Documentation

- [JasperReports Documentation](https://community.jaspersoft.com/documentation)
- [Apache POI HSSF Guide](https://poi.apache.org/components/spreadsheet/quick-guide.html)
- [OpenELIS Testing Roadmap](../../.specify/guides/testing-roadmap.md)
- [OpenELIS Constitution v1.8.1](../../.specify/memory/constitution.md)

## Conclusion

All technical unknowns have been resolved. The implementation will:
1. Use existing JasperReports framework for PDF generation (6 templates)
2. Use Apache POI HSSF for Excel .xls export
3. Use manual StringBuilder for CSV export
4. Follow FreezerReportServiceImpl pattern for service layer
5. Follow FreezerReportController pattern for REST endpoint
6. Use JRBeanCollectionDataSource with simple POJO beans
7. Implement 4 milestones: M1 (foundation), M2 (PDF), M3 (CSV/Excel), M4 (integration)

No additional research or clarifications needed - ready to proceed to implementation.
