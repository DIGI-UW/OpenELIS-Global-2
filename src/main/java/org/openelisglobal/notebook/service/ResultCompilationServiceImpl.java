package org.openelisglobal.notebook.service;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.ValidationStatus;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for result compilation and dissemination per US7.
 */
@Service
public class ResultCompilationServiceImpl implements ResultCompilationService {

    private static final String VALIDATION_STATUS_KEY = "validationStatus";
    private static final String VALIDATION_REASON_KEY = "validationReason";
    private static final String VALIDATED_BY_KEY = "validatedBy";
    private static final String VALIDATED_AT_KEY = "validatedAt";

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    // In-memory delivery records (in production, use database table)
    private final List<DeliveryRecord> deliveryRecords = new ArrayList<>();
    private int nextDeliveryId = 1;

    @Override
    @Transactional
    public boolean flagSample(Integer pageId, String sampleItemId, ValidationStatus status, String reason,
            String userId) {
        // Validate inputs
        if (status == ValidationStatus.INVALID || status == ValidationStatus.INCONCLUSIVE) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Reason is required for INVALID or INCONCLUSIVE status");
            }
        }

        NotebookPageSample pageSample = notebookPageSampleDAO.getBySampleItemIdAndPageId(sampleItemId, pageId);
        if (pageSample == null) {
            LogEvent.logWarn(this.getClass().getName(), "flagSample",
                    "Sample not found: pageId=" + pageId + ", sampleItemId=" + sampleItemId);
            return false;
        }

        // Update data JSONB with validation info
        Map<String, Object> data = pageSample.getData();
        if (data == null) {
            data = new HashMap<>();
        }

        data.put(VALIDATION_STATUS_KEY, status.name());
        data.put(VALIDATION_REASON_KEY, reason);
        data.put(VALIDATED_BY_KEY, userId);
        data.put(VALIDATED_AT_KEY, System.currentTimeMillis());

        pageSample.setData(data);

        // When a sample is validated (any status except PENDING), mark page status as
        // COMPLETED
        if (status != ValidationStatus.PENDING) {
            pageSample.setStatus(NotebookPageSample.Status.COMPLETED);
            pageSample.setCompletedAt(new Timestamp(System.currentTimeMillis()));
            // Set completed by user if possible
            if (userId != null) {
                try {
                    SystemUser user = systemUserService.get(userId);
                    if (user != null) {
                        pageSample.setCompletedBy(user);
                    }
                } catch (Exception e) {
                    LogEvent.logWarn(this.getClass().getName(), "flagSample",
                            "Could not set completedBy user: " + e.getMessage());
                }
            }
        }

        notebookPageSampleDAO.update(pageSample);

        return true;
    }

    @Override
    @Transactional
    public int bulkFlagSamples(Integer pageId, List<String> sampleItemIds, ValidationStatus status, String reason,
            String userId) {
        int flagged = 0;
        for (String sampleItemId : sampleItemIds) {
            try {
                if (flagSample(pageId, sampleItemId, status, reason, userId)) {
                    flagged++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "bulkFlagSamples",
                        "Failed to flag sample " + sampleItemId + ": " + e.getMessage());
            }
        }
        return flagged;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationSummary getValidationSummary(Integer pageId) {
        List<NotebookPageSample> samples = notebookPageSampleDAO.getByPageId(pageId);

        long valid = 0, invalid = 0, inconclusive = 0, pending = 0;

        for (NotebookPageSample sample : samples) {
            ValidationStatus status = getValidationStatus(sample);
            switch (status) {
            case VALID:
                valid++;
                break;
            case INVALID:
                invalid++;
                break;
            case INCONCLUSIVE:
                inconclusive++;
                break;
            default:
                pending++;
            }
        }

        return new ValidationSummary(samples.size(), valid, invalid, inconclusive, pending);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationSummary getNotebookValidationSummary(Integer notebookId) {
        List<NotebookPageSample> samples = notebookPageSampleDAO.getByNotebookId(notebookId);

        long valid = 0, invalid = 0, inconclusive = 0, pending = 0;

        for (NotebookPageSample sample : samples) {
            ValidationStatus status = getValidationStatus(sample);
            switch (status) {
            case VALID:
                valid++;
                break;
            case INVALID:
                invalid++;
                break;
            case INCONCLUSIVE:
                inconclusive++;
                break;
            default:
                pending++;
            }
        }

        return new ValidationSummary(samples.size(), valid, invalid, inconclusive, pending);
    }

    private ValidationStatus getValidationStatus(NotebookPageSample sample) {
        if (sample.getData() == null) {
            return ValidationStatus.PENDING;
        }
        Object statusObj = sample.getData().get(VALIDATION_STATUS_KEY);
        if (statusObj == null) {
            return ValidationStatus.PENDING;
        }
        return ValidationStatus.fromString(statusObj.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] compileToExcel(Integer notebookId, ExportOptions options) {
        LogEvent.logInfo(this.getClass().getName(), "compileToExcel",
                "Starting Excel export for notebook ID: " + notebookId);

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            LogEvent.logError(this.getClass().getName(), "compileToExcel", "Notebook not found: " + notebookId);
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }
        LogEvent.logInfo(this.getClass().getName(), "compileToExcel",
                "Found notebook: " + notebook.getTitle() + " (ID: " + notebookId + ")");

        List<NotebookPageSample> samples = notebookPageSampleDAO.getByNotebookId(notebookId);
        LogEvent.logInfo(this.getClass().getName(), "compileToExcel",
                "Found " + samples.size() + " samples for notebook " + notebookId);

        // Collect all unique result column keys from all samples' data
        java.util.Set<String> resultColumnKeys = new java.util.LinkedHashSet<>();
        for (NotebookPageSample pageSample : samples) {
            Map<String, Object> data = pageSample.getData();
            if (data != null) {
                for (String key : data.keySet()) {
                    // Skip internal keys (validation fields)
                    if (!key.equals(VALIDATION_STATUS_KEY) && !key.equals(VALIDATION_REASON_KEY)
                            && !key.equals(VALIDATED_BY_KEY) && !key.equals(VALIDATED_AT_KEY)) {
                        resultColumnKeys.add(key);
                    }
                }
            }
        }
        List<String> dynamicColumns = new ArrayList<>(resultColumnKeys);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(options.title() != null ? options.title() : "Results");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create status styles
            CellStyle validStyle = workbook.createCellStyle();
            validStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            validStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle invalidStyle = workbook.createCellStyle();
            invalidStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            invalidStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle inconclusiveStyle = workbook.createCellStyle();
            inconclusiveStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            inconclusiveStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Build header row with fixed columns + dynamic result columns
            Row headerRow = sheet.createRow(0);
            List<String> allHeaders = new ArrayList<>();
            allHeaders.add("Sample ID");
            allHeaders.add("External ID");
            allHeaders.add("Sample Type");
            allHeaders.add("Status");
            allHeaders.add("Validation Status");
            allHeaders.add("Reason");
            // Add dynamic result columns
            allHeaders.addAll(dynamicColumns);
            allHeaders.add("Completed At");
            allHeaders.add("Completed By");

            for (int i = 0; i < allHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(allHeaders.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (NotebookPageSample pageSample : samples) {
                try {
                    ValidationStatus validationStatus = getValidationStatus(pageSample);

                    // Filter based on options
                    if (!options.includeInvalid() && validationStatus == ValidationStatus.INVALID) {
                        continue;
                    }
                    if (!options.includeInconclusive() && validationStatus == ValidationStatus.INCONCLUSIVE) {
                        continue;
                    }

                    Row row = sheet.createRow(rowNum++);
                    int colIdx = 0;

                    // Get sample details - handle potential null or missing samples
                    String externalId = "";
                    String sampleTypeDesc = "";
                    try {
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem != null) {
                            externalId = sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "";
                            if (sampleItem.getTypeOfSample() != null) {
                                sampleTypeDesc = sampleItem.getTypeOfSample().getDescription();
                            }
                        }
                    } catch (Exception e) {
                        // Sample not found - use empty strings
                        LogEvent.logDebug(this.getClass().getName(), "compileToExcel",
                                "Sample not found: " + pageSample.getSampleItemId());
                    }

                    row.createCell(colIdx++)
                            .setCellValue(pageSample.getSampleItemId() != null ? pageSample.getSampleItemId() : "");
                    row.createCell(colIdx++).setCellValue(externalId);
                    row.createCell(colIdx++).setCellValue(sampleTypeDesc);
                    row.createCell(colIdx++)
                            .setCellValue(pageSample.getStatus() != null ? pageSample.getStatus().name() : "");

                    Cell statusCell = row.createCell(colIdx++);
                    statusCell.setCellValue(validationStatus.getDisplayName());

                    // Apply color based on status
                    switch (validationStatus) {
                    case VALID:
                        statusCell.setCellStyle(validStyle);
                        break;
                    case INVALID:
                        statusCell.setCellStyle(invalidStyle);
                        break;
                    case INCONCLUSIVE:
                        statusCell.setCellStyle(inconclusiveStyle);
                        break;
                    default:
                        break;
                    }

                    // Reason - safely access nested map
                    String reason = "";
                    Map<String, Object> data = pageSample.getData();
                    try {
                        if (data != null && data.get(VALIDATION_REASON_KEY) != null) {
                            reason = data.get(VALIDATION_REASON_KEY).toString();
                        }
                    } catch (Exception e) {
                        // Ignore JSONB access errors
                    }
                    row.createCell(colIdx++).setCellValue(reason);

                    // Add dynamic result columns values
                    for (String columnKey : dynamicColumns) {
                        String value = "";
                        try {
                            if (data != null && data.get(columnKey) != null) {
                                Object val = data.get(columnKey);
                                value = val.toString();
                            }
                        } catch (Exception e) {
                            // Ignore errors accessing data fields
                        }
                        row.createCell(colIdx++).setCellValue(value);
                    }

                    // Completed info - avoid lazy loading issues
                    Timestamp completedAt = pageSample.getCompletedAt();
                    row.createCell(colIdx++).setCellValue(completedAt != null ? completedAt.toString() : "");

                    // For completedBy, we need to handle potential lazy loading exception
                    String completedByName = "";
                    try {
                        SystemUser completedBy = pageSample.getCompletedBy();
                        if (completedBy != null) {
                            String firstName = completedBy.getFirstName() != null ? completedBy.getFirstName() : "";
                            String lastName = completedBy.getLastName() != null ? completedBy.getLastName() : "";
                            completedByName = (firstName + " " + lastName).trim();
                        }
                    } catch (Exception e) {
                        // Lazy loading failed - leave empty
                        LogEvent.logDebug(this.getClass().getName(), "compileToExcel",
                                "Could not load completedBy for sample: " + pageSample.getSampleItemId());
                    }
                    row.createCell(colIdx++).setCellValue(completedByName);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getName(), "compileToExcel",
                            "Error processing sample " + pageSample.getSampleItemId() + ": " + e.getMessage());
                    // Continue to next sample
                }
            }

            // Auto-size columns
            for (int i = 0; i < allHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "compileToExcel",
                    "Failed to generate Excel: " + e.getMessage());
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] compileToCsv(Integer notebookId, ExportOptions options) {
        LogEvent.logInfo(this.getClass().getName(), "compileToCsv",
                "Starting CSV export for notebook ID: " + notebookId);

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        List<NotebookPageSample> samples = notebookPageSampleDAO.getByNotebookId(notebookId);
        LogEvent.logInfo(this.getClass().getName(), "compileToCsv",
                "Found " + samples.size() + " samples for notebook " + notebookId);

        // Collect all unique result column keys from all samples' data
        java.util.Set<String> resultColumnKeys = new java.util.LinkedHashSet<>();
        for (NotebookPageSample pageSample : samples) {
            Map<String, Object> data = pageSample.getData();
            if (data != null) {
                for (String key : data.keySet()) {
                    // Skip internal keys (validation fields)
                    if (!key.equals(VALIDATION_STATUS_KEY) && !key.equals(VALIDATION_REASON_KEY)
                            && !key.equals(VALIDATED_BY_KEY) && !key.equals(VALIDATED_AT_KEY)) {
                        resultColumnKeys.add(key);
                    }
                }
            }
        }
        List<String> dynamicColumns = new ArrayList<>(resultColumnKeys);

        StringBuilder csv = new StringBuilder();

        // Build header with fixed columns + dynamic result columns
        List<String> allHeaders = new ArrayList<>();
        allHeaders.add("Sample ID");
        allHeaders.add("External ID");
        allHeaders.add("Sample Type");
        allHeaders.add("Status");
        allHeaders.add("Validation Status");
        allHeaders.add("Reason");
        allHeaders.addAll(dynamicColumns);
        allHeaders.add("Completed At");
        allHeaders.add("Completed By");

        csv.append(String.join(",", allHeaders)).append("\n");

        // Data rows
        for (NotebookPageSample pageSample : samples) {
            try {
                ValidationStatus validationStatus = getValidationStatus(pageSample);

                // Filter based on options
                if (!options.includeInvalid() && validationStatus == ValidationStatus.INVALID) {
                    continue;
                }
                if (!options.includeInconclusive() && validationStatus == ValidationStatus.INCONCLUSIVE) {
                    continue;
                }

                // Get sample details
                String externalId = "";
                String sampleTypeDesc = "";
                try {
                    SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                    if (sampleItem != null) {
                        externalId = sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "";
                        if (sampleItem.getTypeOfSample() != null) {
                            sampleTypeDesc = sampleItem.getTypeOfSample().getDescription();
                        }
                    }
                } catch (Exception e) {
                    // Sample not found - use empty strings
                }

                csv.append(escapeCsv(pageSample.getSampleItemId() != null ? pageSample.getSampleItemId() : ""))
                        .append(",");
                csv.append(escapeCsv(externalId)).append(",");
                csv.append(escapeCsv(sampleTypeDesc)).append(",");
                csv.append(escapeCsv(pageSample.getStatus() != null ? pageSample.getStatus().name() : "")).append(",");
                csv.append(escapeCsv(validationStatus.getDisplayName())).append(",");

                // Reason - safely access nested map
                String reason = "";
                Map<String, Object> data = pageSample.getData();
                try {
                    if (data != null && data.get(VALIDATION_REASON_KEY) != null) {
                        reason = data.get(VALIDATION_REASON_KEY).toString();
                    }
                } catch (Exception e) {
                    // Ignore JSONB access errors
                }
                csv.append(escapeCsv(reason)).append(",");

                // Add dynamic result columns values
                for (String columnKey : dynamicColumns) {
                    String value = "";
                    try {
                        if (data != null && data.get(columnKey) != null) {
                            Object val = data.get(columnKey);
                            value = val.toString();
                        }
                    } catch (Exception e) {
                        // Ignore errors accessing data fields
                    }
                    csv.append(escapeCsv(value)).append(",");
                }

                csv.append(escapeCsv(pageSample.getCompletedAt() != null ? pageSample.getCompletedAt().toString() : ""))
                        .append(",");

                // Completed by - handle lazy loading
                String completedByName = "";
                try {
                    SystemUser completedBy = pageSample.getCompletedBy();
                    if (completedBy != null) {
                        String firstName = completedBy.getFirstName() != null ? completedBy.getFirstName() : "";
                        String lastName = completedBy.getLastName() != null ? completedBy.getLastName() : "";
                        completedByName = (firstName + " " + lastName).trim();
                    }
                } catch (Exception e) {
                    // Lazy loading failed
                }
                csv.append(escapeCsv(completedByName));

                csv.append("\n");
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "compileToCsv",
                        "Error processing sample " + pageSample.getSampleItemId() + ": " + e.getMessage());
            }
        }

        return csv.toString().getBytes();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public byte[] generatePdfReport(Integer notebookId, ExportOptions options) {
        // PDF generation would require a library like iText or Apache PDFBox
        // For now, return empty - implement when PDF library is added
        throw new UnsupportedOperationException("PDF generation not yet implemented");
    }

    @Override
    @Transactional
    public Integer recordDelivery(Integer notebookId, String recipientName, String recipientEmail, Integer fileId,
            String userId) {
        // Get file name (fileId is optional, may be null for direct delivery)
        String fileName = fileId != null ? "File_" + fileId : "Direct Delivery";

        // Get user name
        String deliveredBy = userId;
        SystemUser user = systemUserService.get(userId);
        if (user != null) {
            deliveredBy = user.getFirstName() + " " + user.getLastName();
        }

        DeliveryRecord record = new DeliveryRecord(nextDeliveryId++, recipientName, recipientEmail, fileName,
                LocalDateTime.now(), deliveredBy);

        deliveryRecords.add(record);

        return record.id();
    }

    @Override
    public List<DeliveryRecord> getDeliveryHistory(Integer notebookId) {
        // In production, filter by notebookId from database
        return new ArrayList<>(deliveryRecords);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSamplesWithValidation(Integer pageId) {
        List<NotebookPageSample> samples = notebookPageSampleDAO.getByPageId(pageId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (NotebookPageSample pageSample : samples) {
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("id", pageSample.getSampleItemId());
            sampleData.put("pageStatus", pageSample.getStatus().name());

            ValidationStatus validationStatus = getValidationStatus(pageSample);
            String validationReason = null;
            boolean inheritedFromParent = false;

            // Build the combined data map (JSONB data + result info)
            Map<String, Object> combinedData = new HashMap<>();
            if (pageSample.getData() != null) {
                combinedData.putAll(pageSample.getData());
                Object reasonObj = pageSample.getData().get(VALIDATION_REASON_KEY);
                validationReason = reasonObj != null ? reasonObj.toString() : null;
            }

            // Get sample details and result data from Result/Analysis tables
            SampleItem sampleItem = null;
            try {
                String sampleItemId = pageSample.getSampleItemId();
                sampleItem = sampleItemService.get(sampleItemId);
                if (sampleItem != null) {
                    sampleData.put("externalId", sampleItem.getExternalId());
                    if (sampleItem.getTypeOfSample() != null) {
                        sampleData.put("sampleType", sampleItem.getTypeOfSample().getDescription());
                    }

                    // Always get result data from Analysis/Result tables (analyzer results)
                    List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
                    LogEvent.logDebug(this.getClass().getName(), "getSamplesWithValidation", "Sample " + sampleItemId
                            + " has " + (analyses != null ? analyses.size() : 0) + " analyses");

                    if (analyses != null && !analyses.isEmpty()) {
                        StringBuilder resultSummary = new StringBuilder();
                        for (Analysis analysis : analyses) {
                            List<Result> results = resultService.getResultsByAnalysis(analysis);
                            for (Result res : results) {
                                String testName = analysis.getTest() != null ? analysis.getTest().getLocalizedName()
                                        : "Unknown";
                                String value = resultService.getResultValue(res, true);
                                if (value != null && !value.isEmpty()) {
                                    if (resultSummary.length() > 0) {
                                        resultSummary.append("; ");
                                    }
                                    resultSummary.append(testName).append(": ").append(value);
                                }
                            }
                        }
                        if (resultSummary.length() > 0) {
                            combinedData.put("result", resultSummary.toString());
                        }
                    }
                } else {
                    LogEvent.logDebug(this.getClass().getName(), "getSamplesWithValidation",
                            "SampleItem not found for ID: " + sampleItemId);
                }
            } catch (Exception e) {
                // Sample not found or error getting results
                LogEvent.logWarn(this.getClass().getName(), "getSamplesWithValidation",
                        "Error getting sample/result data for " + pageSample.getSampleItemId() + ": " + e.getMessage());
            }

            // If this is a child sample with PENDING validation, check parent's validation
            if (validationStatus == ValidationStatus.PENDING && sampleItem != null) {
                SampleItem parentSampleItem = sampleItem.getParentSampleItem();
                if (parentSampleItem != null) {
                    // Find parent's NotebookPageSample record on this page
                    NotebookPageSample parentPageSample = notebookPageSampleDAO.getByPageIdAndSampleItemId(pageId,
                            Integer.parseInt(parentSampleItem.getId()));

                    if (parentPageSample != null) {
                        ValidationStatus parentValidation = getValidationStatus(parentPageSample);
                        if (parentValidation != ValidationStatus.PENDING) {
                            // Inherit parent's validation status
                            validationStatus = parentValidation;
                            inheritedFromParent = true;

                            // Get parent's validation reason
                            if (parentPageSample.getData() != null) {
                                Object parentReasonObj = parentPageSample.getData().get(VALIDATION_REASON_KEY);
                                if (parentReasonObj != null) {
                                    validationReason = parentReasonObj.toString() + " (inherited from parent)";
                                }
                            }

                            LogEvent.logDebug(this.getClass().getName(), "getSamplesWithValidation",
                                    "Child sample " + pageSample.getSampleItemId()
                                            + " inherited validation status from parent " + parentSampleItem.getId()
                                            + ": " + parentValidation.name());
                        }
                    }
                }
            }

            sampleData.put("validationStatus", validationStatus.name());
            sampleData.put("validationDisplayName", validationStatus.getDisplayName());
            sampleData.put("validationColor", validationStatus.getTagColor());
            sampleData.put("validationReason", validationReason);
            sampleData.put("inheritedFromParent", inheritedFromParent);
            sampleData.put("data", combinedData);
            result.add(sampleData);
        }

        return result;
    }

    @Override
    @Transactional
    public Integer attachReportToNotebook(Integer notebookId, ExportOptions options, String userId) {
        LogEvent.logInfo(this.getClass().getName(), "attachReportToNotebook",
                "Generating and attaching report for notebook ID: " + notebookId);

        // Generate the Excel report
        byte[] excelData = compileToExcel(notebookId, options);

        // Create filename with timestamp
        String timestamp = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String fileName = String.format("Results_%s_%s.xlsx", notebookId, timestamp);

        // Attach to notebook using NoteBookService
        Integer fileId = noteBookService.attachFile(notebookId, excelData, fileName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", userId);

        LogEvent.logInfo(this.getClass().getName(), "attachReportToNotebook",
                "Attached report " + fileName + " to notebook " + notebookId + " with file ID: " + fileId);

        return fileId;
    }
}
