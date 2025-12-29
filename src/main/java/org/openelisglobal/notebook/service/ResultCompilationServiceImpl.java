package org.openelisglobal.notebook.service;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.openelisglobal.notebook.dao.NotebookDeliveryRecordDAO;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookDeliveryRecord;
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
    private NotebookDeliveryRecordDAO notebookDeliveryRecordDAO;

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

        // Note: Flagging a sample only updates the validationStatus in the data JSONB.
        // The pageStatus (PENDING/IN_PROGRESS/COMPLETED) is NOT changed by flagging.
        // The user must explicitly use "Send to Reporting" to mark samples as COMPLETED
        // and trigger the T150 flow to the next page.

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

        // Count validation statuses per UNIQUE sample (not per page-sample record)
        // A sample can appear on multiple pages, so we track by sampleItemId
        java.util.Map<String, ValidationStatus> uniqueSampleStatuses = new java.util.HashMap<>();
        for (NotebookPageSample sample : samples) {
            String sampleItemId = sample.getSampleItemId();
            if (sampleItemId != null) {
                ValidationStatus status = getValidationStatus(sample);
                // Use the most "definitive" status if sample appears on multiple pages
                // Priority: VALID/INVALID/INCONCLUSIVE > PENDING
                ValidationStatus existingStatus = uniqueSampleStatuses.get(sampleItemId);
                if (existingStatus == null || existingStatus == ValidationStatus.PENDING) {
                    uniqueSampleStatuses.put(sampleItemId, status);
                }
            }
        }

        // Count by status
        long valid = 0, invalid = 0, inconclusive = 0, pending = 0;
        for (ValidationStatus status : uniqueSampleStatuses.values()) {
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

        return new ValidationSummary(uniqueSampleStatuses.size(), valid, invalid, inconclusive, pending);
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
                "Starting comprehensive Excel export for notebook ID: " + notebookId);

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

        try (Workbook workbook = new XSSFWorkbook()) {
            // Create Summary sheet first
            createSummarySheet(workbook, notebook, samples);

            // Create main Results sheet
            Sheet sheet = workbook.createSheet(options.title() != null ? options.title() : "Sample Results");

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

            // Define comprehensive headers in logical order
            List<String> allHeaders = getComprehensiveHeaders();

            // Build header row
            Row headerRow = sheet.createRow(0);
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
                    Map<String, Object> data = pageSample.getData() != null ? pageSample.getData() : new HashMap<>();

                    // Get sample details
                    String externalId = "";
                    String sampleTypeDesc = "";
                    String accessionNumber = "";
                    String collectionDate = "";
                    String patientName = "";
                    String patientId = "";
                    try {
                        SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                        if (sampleItem != null) {
                            externalId = sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "";
                            if (sampleItem.getTypeOfSample() != null) {
                                sampleTypeDesc = sampleItem.getTypeOfSample().getDescription();
                            }
                            if (sampleItem.getSample() != null) {
                                accessionNumber = sampleItem.getSample().getAccessionNumber() != null
                                        ? sampleItem.getSample().getAccessionNumber()
                                        : "";
                                if (sampleItem.getSample().getCollectionDate() != null) {
                                    collectionDate = sampleItem.getSample().getCollectionDate().toString();
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogEvent.logDebug(this.getClass().getName(), "compileToExcel",
                                "Sample not found: " + pageSample.getSampleItemId());
                    }

                    // Populate row with comprehensive data - must match headers order
                    int colIdx = 0;

                    // Sample Identification
                    row.createCell(colIdx++)
                            .setCellValue(pageSample.getSampleItemId() != null ? pageSample.getSampleItemId() : "");
                    row.createCell(colIdx++).setCellValue(externalId);
                    row.createCell(colIdx++).setCellValue(accessionNumber);
                    row.createCell(colIdx++).setCellValue(sampleTypeDesc);
                    row.createCell(colIdx++).setCellValue(collectionDate);

                    // Status and Validation
                    row.createCell(colIdx++)
                            .setCellValue(pageSample.getStatus() != null ? pageSample.getStatus().name() : "");
                    Cell statusCell = row.createCell(colIdx++);
                    statusCell.setCellValue(validationStatus.getDisplayName());
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
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, VALIDATION_REASON_KEY));

                    // Reception Data (Page 1)
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "receptionDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "receptionTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "projectName"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "manifestReference"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "sourceFacility"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "transportTemperature"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageConditionOnArrival"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "receivingPersonnel"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "manifestVerified"));

                    // Initial Processing Data (Page 2)
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "volumeMeasured"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "volumeSufficient"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "volumeNotes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellCountMethod"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellConcentration"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellConcentrationUnit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "isolationRequired"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "isolationMethod"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellPopulationIsolated"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "centrifugationSpeed"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "centrifugationSpeedUnit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "centrifugationTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "centrifugationTimeUnit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "processingTemperature"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "temperatureUnit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "equipmentUsed"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellViabilityPercentage"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "finalCellYield"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "processingStartTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "processingEndTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "viabilityThresholdMet"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "cellCountAdequate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "visualInspectionPassed"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "contaminationObserved"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcResult"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "processingQcNotes"));

                    // Additional Assays Data (Page 3)
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "testType"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "testTypeOther"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayProtocol"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayPurpose"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "operatorName"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "operatorInitials"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayStartTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayEndTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "resultsSummary"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "resultsQuantitative"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "resultsUnit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assayResult"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "failAction"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "failureReason"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "deviationObserved"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "deviationDescription"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "deviationImpact"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "correctiveAction"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "generalNotes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "selectedReagents"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "selectedEquipment"));

                    // Test Results
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "testResult"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "ctValue"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "concentration"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "absorbance"));

                    // Test Assignment Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "experimentCategory"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "subcategory"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "specificAssay"));

                    // Test Execution Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "runId"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "runCompleted"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "runIssues"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "executionDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "executionTime"));

                    // Instrument Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "instrument"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "instrumentId"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "machineType"));

                    // Reagent Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "kitLot"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "selectedReagents"));

                    // Machine Scheduling
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "scheduledDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "timeSlot"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "startTime"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "endTime"));

                    // Operator Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "operator"));

                    // Sample Processing (Extraction) Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "extractionMethod"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "extractionKit"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "elutionVolume"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "inputVolume"));

                    // QC Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcStatus"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcConcentration"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcPurity260280"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcPurity260230"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "qcNotes"));

                    // Storage Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageLocation"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageBox"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageWell"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageCondition"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storageTemperature"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "storagePath"));

                    // Aliquoting / Child Samples Info
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "aliquotCount"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "aliquotVolume"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "parentSampleId"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "isParentSample"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "childSampleIds"));

                    // Archival Data
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "archiveLocation"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "archiveDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "retentionPeriod"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "disposalDate"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "disposedBy"));

                    // Notes
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "notes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "executionNotes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "assignmentNotes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "receptionNotes"));
                    row.createCell(colIdx++).setCellValue(getStringFromData(data, "processingNotes"));

                    // Completed info
                    Timestamp completedAt = pageSample.getCompletedAt();
                    row.createCell(colIdx++).setCellValue(completedAt != null ? completedAt.toString() : "");
                    String completedByName = "";
                    try {
                        SystemUser completedBy = pageSample.getCompletedBy();
                        if (completedBy != null) {
                            String firstName = completedBy.getFirstName() != null ? completedBy.getFirstName() : "";
                            String lastName = completedBy.getLastName() != null ? completedBy.getLastName() : "";
                            completedByName = (firstName + " " + lastName).trim();
                        }
                    } catch (Exception e) {
                        LogEvent.logDebug(this.getClass().getName(), "compileToExcel",
                                "Could not load completedBy for sample: " + pageSample.getSampleItemId());
                    }
                    row.createCell(colIdx++).setCellValue(completedByName);

                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getName(), "compileToExcel",
                            "Error processing sample " + pageSample.getSampleItemId() + ": " + e.getMessage());
                }
            }

            // Auto-size columns (limit to prevent very wide columns)
            for (int i = 0; i < allHeaders.size(); i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 10000) {
                    sheet.setColumnWidth(i, 10000);
                }
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

    /**
     * Get comprehensive headers for the export in logical order. Includes all
     * Immunology workflow data points.
     */
    private List<String> getComprehensiveHeaders() {
        List<String> headers = new ArrayList<>();

        // Sample Identification
        headers.add("Sample ID");
        headers.add("External ID");
        headers.add("Accession Number");
        headers.add("Sample Type");
        headers.add("Collection Date");

        // Status and Validation
        headers.add("Page Status");
        headers.add("Validation Status");
        headers.add("Validation Reason");

        // Reception Data (Page 1)
        headers.add("Reception Date");
        headers.add("Reception Time");
        headers.add("Project Name");
        headers.add("Manifest Reference");
        headers.add("Source Facility");
        headers.add("Transport Temperature");
        headers.add("Storage Condition on Arrival");
        headers.add("Receiving Personnel");
        headers.add("Manifest Verified");

        // Initial Processing Data (Page 2)
        headers.add("Volume Measured (mL)");
        headers.add("Volume Sufficient");
        headers.add("Volume Notes");
        headers.add("Cell Count Method");
        headers.add("Cell Concentration");
        headers.add("Cell Concentration Unit");
        headers.add("Isolation Required");
        headers.add("Isolation Method");
        headers.add("Cell Population Isolated");
        headers.add("Centrifugation Speed");
        headers.add("Centrifugation Speed Unit");
        headers.add("Centrifugation Time");
        headers.add("Centrifugation Time Unit");
        headers.add("Processing Temperature");
        headers.add("Temperature Unit");
        headers.add("Equipment Used");
        headers.add("Cell Viability (%)");
        headers.add("Final Cell Yield");
        headers.add("Processing Start Time");
        headers.add("Processing End Time");
        headers.add("Viability Threshold Met");
        headers.add("Cell Count Adequate");
        headers.add("Visual Inspection Passed");
        headers.add("Contamination Observed");
        headers.add("Processing QC Result");
        headers.add("Processing QC Notes");

        // Additional Assays Data (Page 3)
        headers.add("Assay Test Type");
        headers.add("Assay Test Type Other");
        headers.add("Assay Protocol");
        headers.add("Assay Purpose");
        headers.add("Assay Operator Name");
        headers.add("Assay Operator Initials");
        headers.add("Assay Date");
        headers.add("Assay Start Time");
        headers.add("Assay End Time");
        headers.add("Assay Results Summary");
        headers.add("Assay Results Quantitative");
        headers.add("Assay Results Unit");
        headers.add("Assay Result (Pass/Fail)");
        headers.add("Assay Fail Action");
        headers.add("Assay Failure Reason");
        headers.add("Assay Deviation Observed");
        headers.add("Assay Deviation Description");
        headers.add("Assay Deviation Impact");
        headers.add("Assay Corrective Action");
        headers.add("Assay General Notes");
        headers.add("Assay Selected Reagents");
        headers.add("Assay Selected Equipment");

        // Test Results
        headers.add("Test Result");
        headers.add("CT Value");
        headers.add("Concentration");
        headers.add("Absorbance");

        // Test Assignment
        headers.add("Experiment Category");
        headers.add("Subcategory");
        headers.add("Specific Assay");

        // Test Execution
        headers.add("Run ID");
        headers.add("Run Completed");
        headers.add("Run Issues");
        headers.add("Execution Date");
        headers.add("Execution Time");

        // Instrument
        headers.add("Instrument");
        headers.add("Instrument ID");
        headers.add("Machine Type");

        // Reagents
        headers.add("Kit Lot Number");
        headers.add("Selected Reagents");

        // Machine Scheduling
        headers.add("Scheduled Date");
        headers.add("Time Slot");
        headers.add("Start Time");
        headers.add("End Time");

        // Operator
        headers.add("Operator");

        // Sample Processing (Extraction)
        headers.add("Extraction Method");
        headers.add("Extraction Kit");
        headers.add("Elution Volume");
        headers.add("Input Volume");

        // QC
        headers.add("QC Status");
        headers.add("QC Concentration");
        headers.add("QC Purity 260/280");
        headers.add("QC Purity 260/230");
        headers.add("QC Notes");

        // Storage
        headers.add("Storage Location");
        headers.add("Storage Box");
        headers.add("Storage Well");
        headers.add("Storage Condition");
        headers.add("Storage Temperature");
        headers.add("Storage Path");

        // Aliquoting / Child Samples
        headers.add("Aliquot Count");
        headers.add("Aliquot Volume");
        headers.add("Parent Sample ID");
        headers.add("Is Parent Sample");
        headers.add("Child Sample IDs");

        // Archival Data
        headers.add("Archive Location");
        headers.add("Archive Date");
        headers.add("Retention Period");
        headers.add("Disposal Date");
        headers.add("Disposed By");

        // Notes
        headers.add("Notes");
        headers.add("Execution Notes");
        headers.add("Assignment Notes");
        headers.add("Reception Notes");
        headers.add("Processing Notes");

        // Completion
        headers.add("Completed At");
        headers.add("Completed By");

        return headers;
    }

    /**
     * Create a summary sheet with notebook metadata and statistics.
     */
    private void createSummarySheet(Workbook workbook, NoteBook notebook, List<NotebookPageSample> samples) {
        Sheet summarySheet = workbook.createSheet("Summary");

        // Create styles
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowNum = 0;

        // Title - use notebook title if available
        Row titleRow = summarySheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        String reportTitle = notebook.getTitle() != null ? notebook.getTitle() + " Export Report"
                : "Notebook Export Report";
        titleCell.setCellValue(reportTitle);
        titleCell.setCellStyle(titleStyle);

        rowNum++; // Empty row

        // Notebook Info
        Row notebookHeaderRow = summarySheet.createRow(rowNum++);
        Cell nbHeaderCell = notebookHeaderRow.createCell(0);
        nbHeaderCell.setCellValue("Notebook Information");
        nbHeaderCell.setCellStyle(headerStyle);

        addSummaryRow(summarySheet, rowNum++, "Notebook ID:", String.valueOf(notebook.getId()));
        addSummaryRow(summarySheet, rowNum++, "Notebook Title:",
                notebook.getTitle() != null ? notebook.getTitle() : "");
        addSummaryRow(summarySheet, rowNum++, "Objective:",
                notebook.getObjective() != null ? notebook.getObjective() : "");
        addSummaryRow(summarySheet, rowNum++, "Export Date:",
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        rowNum++; // Empty row

        // Sample Statistics
        Row statsHeaderRow = summarySheet.createRow(rowNum++);
        Cell statsHeaderCell = statsHeaderRow.createCell(0);
        statsHeaderCell.setCellValue("Sample Statistics");
        statsHeaderCell.setCellStyle(headerStyle);

        // Count validation statuses per UNIQUE sample (not per page-sample record)
        // A sample can appear on multiple pages, so we track by sampleItemId
        java.util.Map<String, ValidationStatus> uniqueSampleStatuses = new java.util.HashMap<>();
        for (NotebookPageSample sample : samples) {
            String sampleItemId = sample.getSampleItemId();
            if (sampleItemId != null) {
                ValidationStatus status = getValidationStatus(sample);
                // Use the most "definitive" status if sample appears on multiple pages
                // Priority: VALID/INVALID/INCONCLUSIVE > PENDING
                ValidationStatus existingStatus = uniqueSampleStatuses.get(sampleItemId);
                if (existingStatus == null || existingStatus == ValidationStatus.PENDING) {
                    uniqueSampleStatuses.put(sampleItemId, status);
                }
            }
        }

        // Count by status
        long valid = 0, invalid = 0, inconclusive = 0, pending = 0;
        for (ValidationStatus status : uniqueSampleStatuses.values()) {
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

        int uniqueSampleCount = uniqueSampleStatuses.size();
        addSummaryRow(summarySheet, rowNum++, "Total Samples:", String.valueOf(uniqueSampleCount));
        addSummaryRow(summarySheet, rowNum++, "Valid Samples:", String.valueOf(valid));
        addSummaryRow(summarySheet, rowNum++, "Invalid Samples:", String.valueOf(invalid));
        addSummaryRow(summarySheet, rowNum++, "Inconclusive Samples:", String.valueOf(inconclusive));
        addSummaryRow(summarySheet, rowNum++, "Pending Validation:", String.valueOf(pending));

        // Collect unique instruments and reagents
        java.util.Set<String> instruments = new java.util.HashSet<>();
        java.util.Set<String> reagents = new java.util.HashSet<>();
        java.util.Set<String> assays = new java.util.HashSet<>();

        for (NotebookPageSample sample : samples) {
            Map<String, Object> data = sample.getData();
            if (data != null) {
                String instrument = getStringFromData(data, "instrument");
                if (!instrument.isEmpty()) {
                    instruments.add(instrument);
                }
                String kitLot = getStringFromData(data, "kitLot");
                if (!kitLot.isEmpty()) {
                    reagents.add(kitLot);
                }
                String assay = getStringFromData(data, "specificAssay");
                if (!assay.isEmpty()) {
                    assays.add(assay);
                }
            }
        }

        rowNum++; // Empty row

        // Instruments Used
        Row instrumentsHeaderRow = summarySheet.createRow(rowNum++);
        Cell instrumentsHeaderCell = instrumentsHeaderRow.createCell(0);
        instrumentsHeaderCell.setCellValue("Instruments Used");
        instrumentsHeaderCell.setCellStyle(headerStyle);
        addSummaryRow(summarySheet, rowNum++, "Count:", String.valueOf(instruments.size()));
        if (!instruments.isEmpty()) {
            addSummaryRow(summarySheet, rowNum++, "List:", String.join(", ", instruments));
        }

        rowNum++; // Empty row

        // Reagents Used
        Row reagentsHeaderRow = summarySheet.createRow(rowNum++);
        Cell reagentsHeaderCell = reagentsHeaderRow.createCell(0);
        reagentsHeaderCell.setCellValue("Reagent Lots Used");
        reagentsHeaderCell.setCellStyle(headerStyle);
        addSummaryRow(summarySheet, rowNum++, "Count:", String.valueOf(reagents.size()));
        if (!reagents.isEmpty()) {
            addSummaryRow(summarySheet, rowNum++, "List:", String.join(", ", reagents));
        }

        rowNum++; // Empty row

        // Assays Performed
        Row assaysHeaderRow = summarySheet.createRow(rowNum++);
        Cell assaysHeaderCell = assaysHeaderRow.createCell(0);
        assaysHeaderCell.setCellValue("Assays Performed");
        assaysHeaderCell.setCellStyle(headerStyle);
        addSummaryRow(summarySheet, rowNum++, "Count:", String.valueOf(assays.size()));
        if (!assays.isEmpty()) {
            addSummaryRow(summarySheet, rowNum++, "List:", String.join(", ", assays));
        }

        // Auto-size columns
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    /**
     * Safely get a string value from the data map.
     */
    private String getStringFromData(Map<String, Object> data, String key) {
        if (data == null || data.get(key) == null) {
            return "";
        }
        Object val = data.get(key);
        if (val instanceof List) {
            return String.join(", ", ((List<?>) val).stream().map(Object::toString).toList());
        }
        return val.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] compileToCsv(Integer notebookId, ExportOptions options) {
        LogEvent.logInfo(this.getClass().getName(), "compileToCsv",
                "Starting comprehensive CSV export for notebook ID: " + notebookId);

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        List<NotebookPageSample> samples = notebookPageSampleDAO.getByNotebookId(notebookId);
        LogEvent.logInfo(this.getClass().getName(), "compileToCsv",
                "Found " + samples.size() + " samples for notebook " + notebookId);

        StringBuilder csv = new StringBuilder();

        // Use the same comprehensive headers as Excel
        List<String> allHeaders = getComprehensiveHeaders();
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

                Map<String, Object> data = pageSample.getData() != null ? pageSample.getData() : new HashMap<>();

                // Get sample details
                String externalId = "";
                String sampleTypeDesc = "";
                String accessionNumber = "";
                String collectionDate = "";
                try {
                    SampleItem sampleItem = sampleItemService.get(pageSample.getSampleItemId());
                    if (sampleItem != null) {
                        externalId = sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "";
                        if (sampleItem.getTypeOfSample() != null) {
                            sampleTypeDesc = sampleItem.getTypeOfSample().getDescription();
                        }
                        if (sampleItem.getSample() != null) {
                            accessionNumber = sampleItem.getSample().getAccessionNumber() != null
                                    ? sampleItem.getSample().getAccessionNumber()
                                    : "";
                            if (sampleItem.getSample().getCollectionDate() != null) {
                                collectionDate = sampleItem.getSample().getCollectionDate().toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    // Sample not found - use empty strings
                }

                List<String> rowValues = new ArrayList<>();

                // Sample Identification
                rowValues.add(escapeCsv(pageSample.getSampleItemId() != null ? pageSample.getSampleItemId() : ""));
                rowValues.add(escapeCsv(externalId));
                rowValues.add(escapeCsv(accessionNumber));
                rowValues.add(escapeCsv(sampleTypeDesc));
                rowValues.add(escapeCsv(collectionDate));

                // Status and Validation
                rowValues.add(escapeCsv(pageSample.getStatus() != null ? pageSample.getStatus().name() : ""));
                rowValues.add(escapeCsv(validationStatus.getDisplayName()));
                rowValues.add(escapeCsv(getStringFromData(data, VALIDATION_REASON_KEY)));

                // Reception Data (Page 1)
                rowValues.add(escapeCsv(getStringFromData(data, "receptionDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "receptionTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "projectName")));
                rowValues.add(escapeCsv(getStringFromData(data, "manifestReference")));
                rowValues.add(escapeCsv(getStringFromData(data, "sourceFacility")));
                rowValues.add(escapeCsv(getStringFromData(data, "transportTemperature")));
                rowValues.add(escapeCsv(getStringFromData(data, "storageConditionOnArrival")));
                rowValues.add(escapeCsv(getStringFromData(data, "receivingPersonnel")));
                rowValues.add(escapeCsv(getStringFromData(data, "manifestVerified")));

                // Initial Processing Data (Page 2)
                rowValues.add(escapeCsv(getStringFromData(data, "volumeMeasured")));
                rowValues.add(escapeCsv(getStringFromData(data, "volumeSufficient")));
                rowValues.add(escapeCsv(getStringFromData(data, "volumeNotes")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellCountMethod")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellConcentration")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellConcentrationUnit")));
                rowValues.add(escapeCsv(getStringFromData(data, "isolationRequired")));
                rowValues.add(escapeCsv(getStringFromData(data, "isolationMethod")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellPopulationIsolated")));
                rowValues.add(escapeCsv(getStringFromData(data, "centrifugationSpeed")));
                rowValues.add(escapeCsv(getStringFromData(data, "centrifugationSpeedUnit")));
                rowValues.add(escapeCsv(getStringFromData(data, "centrifugationTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "centrifugationTimeUnit")));
                rowValues.add(escapeCsv(getStringFromData(data, "processingTemperature")));
                rowValues.add(escapeCsv(getStringFromData(data, "temperatureUnit")));
                rowValues.add(escapeCsv(getStringFromData(data, "equipmentUsed")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellViabilityPercentage")));
                rowValues.add(escapeCsv(getStringFromData(data, "finalCellYield")));
                rowValues.add(escapeCsv(getStringFromData(data, "processingStartTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "processingEndTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "viabilityThresholdMet")));
                rowValues.add(escapeCsv(getStringFromData(data, "cellCountAdequate")));
                rowValues.add(escapeCsv(getStringFromData(data, "visualInspectionPassed")));
                rowValues.add(escapeCsv(getStringFromData(data, "contaminationObserved")));
                rowValues.add(escapeCsv(getStringFromData(data, "qcResult")));
                rowValues.add(escapeCsv(getStringFromData(data, "processingQcNotes")));

                // Additional Assays Data (Page 3)
                rowValues.add(escapeCsv(getStringFromData(data, "testType")));
                rowValues.add(escapeCsv(getStringFromData(data, "testTypeOther")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayProtocol")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayPurpose")));
                rowValues.add(escapeCsv(getStringFromData(data, "operatorName")));
                rowValues.add(escapeCsv(getStringFromData(data, "operatorInitials")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayStartTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayEndTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "resultsSummary")));
                rowValues.add(escapeCsv(getStringFromData(data, "resultsQuantitative")));
                rowValues.add(escapeCsv(getStringFromData(data, "resultsUnit")));
                rowValues.add(escapeCsv(getStringFromData(data, "assayResult")));
                rowValues.add(escapeCsv(getStringFromData(data, "failAction")));
                rowValues.add(escapeCsv(getStringFromData(data, "failureReason")));
                rowValues.add(escapeCsv(getStringFromData(data, "deviationObserved")));
                rowValues.add(escapeCsv(getStringFromData(data, "deviationDescription")));
                rowValues.add(escapeCsv(getStringFromData(data, "deviationImpact")));
                rowValues.add(escapeCsv(getStringFromData(data, "correctiveAction")));
                rowValues.add(escapeCsv(getStringFromData(data, "generalNotes")));
                rowValues.add(escapeCsv(getStringFromData(data, "selectedReagents")));
                rowValues.add(escapeCsv(getStringFromData(data, "selectedEquipment")));

                // Test Results
                rowValues.add(escapeCsv(getStringFromData(data, "testResult")));
                rowValues.add(escapeCsv(getStringFromData(data, "ctValue")));
                rowValues.add(escapeCsv(getStringFromData(data, "concentration")));
                rowValues.add(escapeCsv(getStringFromData(data, "absorbance")));

                // Test Assignment
                rowValues.add(escapeCsv(getStringFromData(data, "experimentCategory")));
                rowValues.add(escapeCsv(getStringFromData(data, "subcategory")));
                rowValues.add(escapeCsv(getStringFromData(data, "specificAssay")));

                // Test Execution
                rowValues.add(escapeCsv(getStringFromData(data, "runId")));
                rowValues.add(escapeCsv(getStringFromData(data, "runCompleted")));
                rowValues.add(escapeCsv(getStringFromData(data, "runIssues")));
                rowValues.add(escapeCsv(getStringFromData(data, "executionDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "executionTime")));

                // Instrument
                rowValues.add(escapeCsv(getStringFromData(data, "instrument")));
                rowValues.add(escapeCsv(getStringFromData(data, "instrumentId")));
                rowValues.add(escapeCsv(getStringFromData(data, "machineType")));

                // Reagents
                rowValues.add(escapeCsv(getStringFromData(data, "kitLot")));
                rowValues.add(escapeCsv(getStringFromData(data, "selectedReagents")));

                // Machine Scheduling
                rowValues.add(escapeCsv(getStringFromData(data, "scheduledDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "timeSlot")));
                rowValues.add(escapeCsv(getStringFromData(data, "startTime")));
                rowValues.add(escapeCsv(getStringFromData(data, "endTime")));

                // Operator
                rowValues.add(escapeCsv(getStringFromData(data, "operator")));

                // Sample Processing (Extraction)
                rowValues.add(escapeCsv(getStringFromData(data, "extractionMethod")));
                rowValues.add(escapeCsv(getStringFromData(data, "extractionKit")));
                rowValues.add(escapeCsv(getStringFromData(data, "elutionVolume")));
                rowValues.add(escapeCsv(getStringFromData(data, "inputVolume")));

                // QC
                rowValues.add(escapeCsv(getStringFromData(data, "qcStatus")));
                rowValues.add(escapeCsv(getStringFromData(data, "qcConcentration")));
                rowValues.add(escapeCsv(getStringFromData(data, "qcPurity260280")));
                rowValues.add(escapeCsv(getStringFromData(data, "qcPurity260230")));
                rowValues.add(escapeCsv(getStringFromData(data, "qcNotes")));

                // Storage
                rowValues.add(escapeCsv(getStringFromData(data, "storageLocation")));
                rowValues.add(escapeCsv(getStringFromData(data, "storageBox")));
                rowValues.add(escapeCsv(getStringFromData(data, "storageWell")));
                rowValues.add(escapeCsv(getStringFromData(data, "storageCondition")));
                rowValues.add(escapeCsv(getStringFromData(data, "storageTemperature")));
                rowValues.add(escapeCsv(getStringFromData(data, "storagePath")));

                // Aliquoting / Child Samples
                rowValues.add(escapeCsv(getStringFromData(data, "aliquotCount")));
                rowValues.add(escapeCsv(getStringFromData(data, "aliquotVolume")));
                rowValues.add(escapeCsv(getStringFromData(data, "parentSampleId")));
                rowValues.add(escapeCsv(getStringFromData(data, "isParentSample")));
                rowValues.add(escapeCsv(getStringFromData(data, "childSampleIds")));

                // Archival Data
                rowValues.add(escapeCsv(getStringFromData(data, "archiveLocation")));
                rowValues.add(escapeCsv(getStringFromData(data, "archiveDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "retentionPeriod")));
                rowValues.add(escapeCsv(getStringFromData(data, "disposalDate")));
                rowValues.add(escapeCsv(getStringFromData(data, "disposedBy")));

                // Notes
                rowValues.add(escapeCsv(getStringFromData(data, "notes")));
                rowValues.add(escapeCsv(getStringFromData(data, "executionNotes")));
                rowValues.add(escapeCsv(getStringFromData(data, "assignmentNotes")));
                rowValues.add(escapeCsv(getStringFromData(data, "receptionNotes")));
                rowValues.add(escapeCsv(getStringFromData(data, "processingNotes")));

                // Completion
                rowValues.add(
                        escapeCsv(pageSample.getCompletedAt() != null ? pageSample.getCompletedAt().toString() : ""));
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
                rowValues.add(escapeCsv(completedByName));

                csv.append(String.join(",", rowValues)).append("\n");

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
            String deliveryType, String regulatoryBody, String notes, String userId) {
        // Get file name (fileId is optional, may be null for direct delivery)
        String fileName = fileId != null ? "File_" + fileId : "Direct Delivery";

        // Get user name
        String deliveredBy = userId;
        SystemUser user = systemUserService.get(userId);
        if (user != null) {
            deliveredBy = user.getFirstName() + " " + user.getLastName();
        }

        // Get notebook entity
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        // Create and persist delivery record
        NotebookDeliveryRecord record = new NotebookDeliveryRecord(notebook, recipientName, recipientEmail, fileName,
                deliveryType, regulatoryBody, notes, new Timestamp(System.currentTimeMillis()), deliveredBy);

        notebookDeliveryRecordDAO.insert(record);

        LogEvent.logInfo(this.getClass().getName(), "recordDelivery", "Recorded delivery for notebook " + notebookId
                + " to " + recipientName + " (type: " + deliveryType + ", regulatory: " + regulatoryBody + ")");

        return record.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryRecord> getDeliveryHistory(Integer notebookId) {
        List<NotebookDeliveryRecord> records = notebookDeliveryRecordDAO.getByNotebookId(notebookId);

        // Convert entity to DTO record - format date as ISO string for JavaScript
        // parsing
        return records.stream()
                .map(r -> new DeliveryRecord(r.getId(), r.getRecipientName(), r.getRecipientEmail(), r.getFileName(),
                        r.getDeliveryType(), r.getRegulatoryBody(), r.getNotes(),
                        r.getDeliveredAt() != null ? r.getDeliveredAt().toInstant().toString() : null,
                        r.getDeliveredBy()))
                .collect(Collectors.toList());
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
