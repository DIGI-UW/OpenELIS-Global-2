package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.AnalyticsManifestImportForm;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of AnalyticsWorkflowService for Analytics Laboratory workflow.
 * Handles all five pages of the Analytics workflow.
 */
@Service
public class AnalyticsWorkflowServiceImpl implements AnalyticsWorkflowService {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private IStatusService statusService;

    // ========================================
    // Page 1: Sample Creation & Full Metadata Capture
    // ========================================

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, AnalyticsManifestImportForm columnMapping) {
        List<AnalyticsManifestRow> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new ParsedManifest(rows, errors);
            }

            // Parse header and build column index map
            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Get column indices from Analytics mapping
            Integer sampleIdentifierIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdentifierColumn());
            Integer barcodeIdx = getColumnIndex(columnIndex, columnMapping.getBarcodeColumn());
            Integer sampleCategoryIdx = getColumnIndex(columnIndex, columnMapping.getSampleCategoryColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer sampleSourceIdx = getColumnIndex(columnIndex, columnMapping.getSampleSourceColumn());
            Integer requestingUnitIdx = getColumnIndex(columnIndex, columnMapping.getRequestingUnitColumn());
            Integer requestedTestsIdx = getColumnIndex(columnIndex, columnMapping.getRequestedTestsColumn());
            Integer studyProjectIdIdx = getColumnIndex(columnIndex, columnMapping.getStudyProjectIdColumn());
            Integer storageConditionIdx = getColumnIndex(columnIndex, columnMapping.getStorageConditionColumn());
            Integer receivedDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceivedDateTimeColumn());
            Integer receivedByIdx = getColumnIndex(columnIndex, columnMapping.getReceivedByColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract Analytics values
                String sampleIdentifier = getValueAtIndex(values, sampleIdentifierIdx);
                String barcode = getValueAtIndex(values, barcodeIdx);
                String sampleCategory = getValueAtIndex(values, sampleCategoryIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String sampleSource = getValueAtIndex(values, sampleSourceIdx);
                String requestingUnit = getValueAtIndex(values, requestingUnitIdx);
                String requestedTests = getValueAtIndex(values, requestedTestsIdx);
                String studyProjectId = getValueAtIndex(values, studyProjectIdIdx);
                String storageCondition = getValueAtIndex(values, storageConditionIdx);
                String receivedDateTime = getValueAtIndex(values, receivedDateTimeIdx);
                String receivedBy = getValueAtIndex(values, receivedByIdx);

                // Validate required fields
                if (sampleIdentifier == null || sampleIdentifier.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleIdentifier", "Sample Identifier is required"));
                    continue;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleType", "Sample type is required"));
                    continue;
                }

                rows.add(new AnalyticsManifestRow(rowNumber, sampleIdentifier.trim(), barcode, sampleCategory,
                        sampleType.trim(), sampleSource, requestingUnit, requestedTests, studyProjectId,
                        storageCondition, receivedDateTime, receivedBy));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateSampleTypes(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (AnalyticsManifestRow row : manifest.rows()) {
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());

            TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
            if (found == null) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Unknown sample type: " + row.sampleType()));
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public AnalyticsImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new AnalyticsImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        int totalRequested = manifest.rows().size();

        for (AnalyticsManifestRow row : manifest.rows()) {
            // Look up sample type
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (sampleType == null) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Unknown sample type: " + row.sampleType()));
                continue;
            }

            // Create a parent Sample record with generated accession number
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
            parentSample.setId(sampleIdDb);

            // Get status ID for SampleEntered
            String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
            if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
                sampleEnteredStatusId = "20";
            }

            // Create SampleItem record
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleType);
            item.setExternalId(row.sampleIdentifier());
            item.setSortOrder("1");
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            // Set received date from manifest row
            if (row.receivedDateTime() != null && !row.receivedDateTime().isBlank()) {
                java.sql.Timestamp receivedTimestamp = parseDateTime(row.receivedDateTime());
                if (receivedTimestamp != null) {
                    item.setCollectionDate(receivedTimestamp);
                }
            }

            String itemId = sampleItemService.insert(item);
            item.setId(itemId);
            createdSamples.add(item);
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

            // Add sample to entry
            notebookEntryService.addSample(entryId, item, sysUserId);

            // Store Analytics-specific metadata in NotebookPageSample
            storeAnalyticsMetadata(entryId, Integer.parseInt(itemId), row, sysUserId);
        }

        if (!createdSamples.isEmpty()) {
            List<Integer> createdIds = createdSamples.stream().map(s -> Integer.parseInt(s.getId()))
                    .collect(Collectors.toList());
            notebookSampleEntryService.linkSamplesToNotebook(entryId, createdIds);
        }

        return new AnalyticsImportResult(totalRequested, createdSamples.size(), createdSamples,
                createdAccessionNumbers.stream().distinct().collect(Collectors.toList()), errors);
    }

    @Override
    @Transactional
    public SampleItem createSample(Integer entryId, String sampleIdentifier, String barcode, String sampleCategory,
            String sampleType, String sampleSource, String requestingUnit, String requestedTests, String studyProjectId,
            String storageCondition, String receivedBy, String sysUserId) {

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            throw new IllegalArgumentException("Notebook entry not found: " + entryId);
        }

        // Look up sample type
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription(sampleType);
        TypeOfSample foundSampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

        if (foundSampleType == null) {
            throw new IllegalArgumentException("Unknown sample type: " + sampleType);
        }

        // Create a parent Sample record with generated accession number
        Sample parentSample = new Sample();
        parentSample.setSysUserId(sysUserId);
        parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
        parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
        parentSample.setId(sampleIdDb);

        // Get status ID for SampleEntered
        String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
        if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
            sampleEnteredStatusId = "20";
        }

        // Create SampleItem record
        SampleItem item = new SampleItem();
        item.setSample(parentSample);
        item.setTypeOfSample(foundSampleType);
        item.setExternalId(sampleIdentifier);
        item.setSortOrder("1");
        item.setStatusId(sampleEnteredStatusId);
        item.setSysUserId(sysUserId);
        item.setCollectionDate(new java.sql.Timestamp(System.currentTimeMillis()));

        String itemId = sampleItemService.insert(item);
        item.setId(itemId);

        // Add sample to entry
        notebookEntryService.addSample(entryId, item, sysUserId);

        // Store Analytics-specific metadata
        AnalyticsManifestRow row = new AnalyticsManifestRow(0, sampleIdentifier, barcode, sampleCategory, sampleType,
                sampleSource, requestingUnit, requestedTests, studyProjectId, storageCondition, null, receivedBy);
        storeAnalyticsMetadata(entryId, Integer.parseInt(itemId), row, sysUserId);

        // Link sample to notebook
        notebookSampleEntryService.linkSamplesToNotebook(entryId, List.of(Integer.parseInt(itemId)));

        return item;
    }

    private void storeAnalyticsMetadata(Integer entryId, Integer sampleItemId, AnalyticsManifestRow row,
            String sysUserId) {
        // Get the first page for this entry's notebook
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return;
        }

        NotebookEntry entry = optEntry.get();
        if (entry.getNotebook() == null || entry.getNotebook().getPages() == null
                || entry.getNotebook().getPages().isEmpty()) {
            return;
        }

        // Get first page (Page 1: Sample Creation)
        var firstPage = entry.getNotebook().getPages().stream().min((p1, p2) -> {
            int o1 = p1.getOrder() != null ? p1.getOrder() : 0;
            int o2 = p2.getOrder() != null ? p2.getOrder() : 0;
            return Integer.compare(o1, o2);
        }).orElse(null);

        if (firstPage == null) {
            return;
        }

        // Create page sample record with Analytics metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sampleIdentifier", row.sampleIdentifier());
        metadata.put("barcode", row.barcode());
        metadata.put("sampleCategory", row.sampleCategory());
        metadata.put("sampleType", row.sampleType());
        metadata.put("sampleSource", row.sampleSource());
        metadata.put("requestingUnit", row.requestingUnit());
        metadata.put("requestedTests", row.requestedTests());
        metadata.put("studyProjectId", row.studyProjectId());
        metadata.put("storageCondition", row.storageCondition());
        metadata.put("receivedDateTime", row.receivedDateTime());
        metadata.put("receivedBy", row.receivedBy());
        metadata.put("status", "Created – Pending Test Assignment");

        notebookPageSampleService.bulkApplyData(firstPage.getId(), List.of(sampleItemId), metadata, sysUserId);
    }

    // ========================================
    // Page 2: Test Assignment & Preparation
    // ========================================

    @Override
    @Transactional
    public int assignTestsToSamples(Integer pageId, List<Integer> sampleIds, TestAssignment assignment, String userId) {
        Map<String, Object> assignmentData = new HashMap<>();
        assignmentData.put("analystRole", assignment.analystRole());
        assignmentData.put("analystName", assignment.analystName());
        assignmentData.put("assignmentDate", assignment.assignmentDate());
        assignmentData.put("analyticalMethodology", assignment.analyticalMethodology());
        assignmentData.put("status", "Assigned – Ready for Analysis");

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, assignmentData, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestAssignment getTestAssignment(Integer pageId, Integer sampleItemId) {
        NotebookPageSample pageSample = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (pageSample == null || pageSample.getData() == null) {
            return null;
        }

        Map<String, Object> data = pageSample.getData();

        String analystRole = (String) data.get("analystRole");
        String analystName = (String) data.get("analystName");
        String assignmentDate = (String) data.get("assignmentDate");
        @SuppressWarnings("unchecked")
        List<String> methodology = (List<String>) data.get("analyticalMethodology");

        if (analystRole == null && analystName == null) {
            return null;
        }

        return new TestAssignment(analystRole, analystName, assignmentDate, methodology);
    }

    // ========================================
    // Page 3: Analysis / Test Execution
    // ========================================

    @Override
    @Transactional
    public boolean recordTestExecution(Integer pageId, Integer sampleItemId, TestExecutionData executionData,
            String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("testType", executionData.testType());
        data.put("instrumentId", executionData.instrumentId());
        data.put("analystName", executionData.analystName());
        data.put("runDate", executionData.runDate());
        data.put("instrumentParameters", executionData.instrumentParameters());
        data.put("rawResults", executionData.rawResults());
        data.put("executionNotes", executionData.notes());
        data.put("status", "Analysis Completed");

        int updated = notebookPageSampleService.bulkApplyData(pageId, List.of(sampleItemId), data, userId);
        return updated > 0;
    }

    @Override
    @Transactional
    public int bulkRecordTestExecution(Integer pageId, List<Integer> sampleIds, TestExecutionData executionData,
            String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("testType", executionData.testType());
        data.put("instrumentId", executionData.instrumentId());
        data.put("analystName", executionData.analystName());
        data.put("runDate", executionData.runDate());
        data.put("instrumentParameters", executionData.instrumentParameters());
        data.put("rawResults", executionData.rawResults());
        data.put("executionNotes", executionData.notes());
        data.put("status", "Analysis Completed");

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestExecutionData getTestExecution(Integer pageId, Integer sampleItemId) {
        NotebookPageSample pageSample = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (pageSample == null || pageSample.getData() == null) {
            return null;
        }

        Map<String, Object> data = pageSample.getData();

        String testType = (String) data.get("testType");
        String instrumentId = (String) data.get("instrumentId");
        String analystName = (String) data.get("analystName");
        String runDate = (String) data.get("runDate");
        @SuppressWarnings("unchecked")
        Map<String, Object> instrumentParameters = (Map<String, Object>) data.get("instrumentParameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> rawResults = (Map<String, Object>) data.get("rawResults");
        String notes = (String) data.get("executionNotes");

        if (testType == null && instrumentId == null) {
            return null;
        }

        return new TestExecutionData(testType, instrumentId, analystName, runDate, instrumentParameters, rawResults,
                notes);
    }

    // ========================================
    // Page 4: Result Review, Reporting & Release
    // ========================================

    @Override
    @Transactional
    public boolean reviewResults(Integer pageId, Integer sampleItemId, ResultReview review, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("complianceStatus", review.complianceStatus());
        data.put("reviewerName", review.reviewerName());
        data.put("reviewDate", review.reviewDate());
        data.put("reviewNotes", review.reviewNotes());
        data.put("releaseRecipients", review.releaseRecipients());
        data.put("resultsReleased", review.resultsReleased());
        data.put("status", review.resultsReleased() ? "Results Released" : "Reviewed – Pending Release");

        int updated = notebookPageSampleService.bulkApplyData(pageId, List.of(sampleItemId), data, userId);
        return updated > 0;
    }

    @Override
    @Transactional
    public int bulkReviewResults(Integer pageId, List<Integer> sampleIds, ResultReview review, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("complianceStatus", review.complianceStatus());
        data.put("reviewerName", review.reviewerName());
        data.put("reviewDate", review.reviewDate());
        data.put("reviewNotes", review.reviewNotes());
        data.put("releaseRecipients", review.releaseRecipients());
        data.put("resultsReleased", review.resultsReleased());
        data.put("status", review.resultsReleased() ? "Results Released" : "Reviewed – Pending Release");

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);
    }

    @Override
    @Transactional
    public int releaseResults(Integer pageId, List<Integer> sampleIds, List<String> recipients, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("releaseRecipients", recipients);
        data.put("resultsReleased", true);
        data.put("releaseDate", java.time.LocalDate.now().toString());
        data.put("releasedBy", userId);
        data.put("status", "Results Released");

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultReview getResultReview(Integer pageId, Integer sampleItemId) {
        NotebookPageSample pageSample = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (pageSample == null || pageSample.getData() == null) {
            return null;
        }

        Map<String, Object> data = pageSample.getData();

        String complianceStatus = (String) data.get("complianceStatus");
        String reviewerName = (String) data.get("reviewerName");
        String reviewDate = (String) data.get("reviewDate");
        String reviewNotes = (String) data.get("reviewNotes");
        @SuppressWarnings("unchecked")
        List<String> releaseRecipients = (List<String>) data.get("releaseRecipients");
        Boolean resultsReleased = (Boolean) data.get("resultsReleased");

        if (complianceStatus == null && reviewerName == null) {
            return null;
        }

        return new ResultReview(complianceStatus, reviewerName, reviewDate, reviewNotes, releaseRecipients,
                resultsReleased != null && resultsReleased);
    }

    // ========================================
    // Page 5: Post-Test Sample & Data Handling
    // ========================================

    @Override
    @Transactional
    public boolean setSampleRetention(Integer pageId, Integer sampleItemId, SampleRetention retention, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("retentionReason", retention.retentionReason());
        data.put("retentionYears", retention.retentionYears());
        data.put("biorepositoryTransferStatus", retention.biorepositoryTransferStatus());
        data.put("transferDate", retention.transferDate());
        data.put("transferNotes", retention.transferNotes());

        int updated = notebookPageSampleService.bulkApplyData(pageId, List.of(sampleItemId), data, userId);
        return updated > 0;
    }

    @Override
    @Transactional
    public int bulkSetSampleRetention(Integer pageId, List<Integer> sampleIds, SampleRetention retention,
            String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("retentionReason", retention.retentionReason());
        data.put("retentionYears", retention.retentionYears());
        data.put("biorepositoryTransferStatus", retention.biorepositoryTransferStatus());
        data.put("transferDate", retention.transferDate());
        data.put("transferNotes", retention.transferNotes());

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);
    }

    @Override
    @Transactional
    public int archiveData(Integer pageId, List<Integer> sampleIds, DataArchiving archiving, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("rawDataArchived", archiving.rawDataArchived());
        data.put("processedResultsArchived", archiving.processedResultsArchived());
        data.put("metadataExported", archiving.metadataExported());
        data.put("archiveLocation", archiving.archiveLocation());
        data.put("archiveDate", archiving.archiveDate());

        return notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);
    }

    @Override
    @Transactional
    public int finalizeSampleLifecycle(Integer pageId, List<Integer> sampleIds, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("lifecycleCompleted", true);
        data.put("completedDate", java.time.LocalDate.now().toString());
        data.put("completedBy", userId);
        data.put("status", "Lifecycle Completed");

        // Update page sample data
        notebookPageSampleService.bulkApplyData(pageId, sampleIds, data, userId);

        // Mark samples as completed on this page
        return notebookPageSampleService.bulkUpdateStatus(pageId, sampleIds, Status.COMPLETED, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleRetention getSampleRetention(Integer pageId, Integer sampleItemId) {
        NotebookPageSample pageSample = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (pageSample == null || pageSample.getData() == null) {
            return null;
        }

        Map<String, Object> data = pageSample.getData();

        String retentionReason = (String) data.get("retentionReason");
        Integer retentionYears = data.get("retentionYears") != null
                ? ((Number) data.get("retentionYears")).intValue()
                : null;
        String biorepositoryTransferStatus = (String) data.get("biorepositoryTransferStatus");
        String transferDate = (String) data.get("transferDate");
        String transferNotes = (String) data.get("transferNotes");

        if (retentionReason == null && biorepositoryTransferStatus == null) {
            return null;
        }

        return new SampleRetention(retentionReason, retentionYears, biorepositoryTransferStatus, transferDate,
                transferNotes);
    }

    @Override
    @Transactional(readOnly = true)
    public DataArchiving getDataArchiving(Integer pageId, Integer sampleItemId) {
        NotebookPageSample pageSample = notebookPageSampleService.getByPageIdAndSampleItemId(pageId, sampleItemId);
        if (pageSample == null || pageSample.getData() == null) {
            return null;
        }

        Map<String, Object> data = pageSample.getData();

        Boolean rawDataArchived = (Boolean) data.get("rawDataArchived");
        Boolean processedResultsArchived = (Boolean) data.get("processedResultsArchived");
        Boolean metadataExported = (Boolean) data.get("metadataExported");
        String archiveLocation = (String) data.get("archiveLocation");
        String archiveDate = (String) data.get("archiveDate");

        if (rawDataArchived == null && processedResultsArchived == null) {
            return null;
        }

        return new DataArchiving(rawDataArchived != null && rawDataArchived,
                processedResultsArchived != null && processedResultsArchived,
                metadataExported != null && metadataExported, archiveLocation, archiveDate);
    }

    // ========================================
    // Utility Methods
    // ========================================

    /**
     * Parse a CSV line handling quoted values.
     */
    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    /**
     * Get column index from mapping, case-insensitive.
     */
    private Integer getColumnIndex(Map<String, Integer> columnIndex, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        return columnIndex.get(columnName.trim().toLowerCase());
    }

    /**
     * Safely get value at index from array.
     */
    private String getValueAtIndex(String[] values, Integer index) {
        if (index == null || index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    /**
     * Parse date/time from various formats.
     */
    private java.sql.Timestamp parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        String trimmed = dateStr.trim();

        // Try datetime formats first
        String[] dateTimeFormats = { "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy HH:mm",
                "MM/dd/yyyy HH:mm" };

        for (String format : dateTimeFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(trimmed, formatter);
                return java.sql.Timestamp.valueOf(dateTime);
            } catch (java.time.format.DateTimeParseException e) {
                // Try next format
            }
        }

        // Try date-only formats
        String[] dateFormats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd" };

        for (String format : dateFormats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                java.time.LocalDate date = java.time.LocalDate.parse(trimmed, formatter);
                return java.sql.Timestamp.valueOf(date.atStartOfDay());
            } catch (java.time.format.DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }
}
