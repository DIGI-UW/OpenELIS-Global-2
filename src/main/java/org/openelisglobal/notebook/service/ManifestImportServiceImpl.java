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
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.notebook.form.ManifestImportForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ManifestImportService for CSV manifest processing.
 */
@Service
public class ManifestImportServiceImpl implements ManifestImportService {

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
    private IStatusService statusService;

    @Autowired
    private TestService testService;

    @Autowired
    private AnalysisService analysisService;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, ManifestImportForm columnMapping) {
        List<ManifestRow> rows = new ArrayList<>();
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

            // Get column indices from mapping
            Integer groupIdIdx = getColumnIndex(columnIndex, columnMapping.getGroupIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer volumeIdx = getColumnIndex(columnIndex, columnMapping.getVolumeColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract values
                String groupId = getValueAtIndex(values, groupIdIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String collectionDate = getValueAtIndex(values, collectionDateIdx);
                String volume = getValueAtIndex(values, volumeIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                if (groupId == null || groupId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "group_id", "Group ID is required"));
                    continue;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sample_type", "Sample type is required"));
                    continue;
                }

                // Parse num_of_samples
                int numOfSamples;
                try {
                    numOfSamples = numOfSamplesStr != null && !numOfSamplesStr.isBlank()
                            ? Integer.parseInt(numOfSamplesStr.trim())
                            : 1;
                } catch (NumberFormatException e) {
                    errors.add(
                            new ParseError(rowNumber, "num_of_samples", "Invalid number format: " + numOfSamplesStr));
                    continue;
                }

                rows.add(new ManifestRow(rowNumber, groupId.trim(), sampleType.trim(), collectionDate, volume,
                        numOfSamples, notes));
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

        for (ManifestRow row : manifest.rows()) {
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());

            TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
            if (found == null) {
                errors.add(new ParseError(row.rowNumber(), "sample_type", "Unknown sample type: " + row.sampleType()));
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public ManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            List<String> selectedTestIds, String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new ManifestImportResult(0, 0, createdSamples, errors);
        }

        NotebookEntry entry = optEntry.get();

        // Get the notebook ID from the entry - needed for linking samples to pages
        NoteBook notebook = entry.getNotebook();
        if (notebook == null) {
            errors.add(new ParseError(0, "entry", "Notebook entry has no associated notebook: " + entryId));
            return new ManifestImportResult(0, 0, createdSamples, errors);
        }
        Integer notebookId = notebook.getId();

        // Load selected tests
        List<Test> selectedTests = new ArrayList<>();
        if (selectedTestIds != null && !selectedTestIds.isEmpty()) {
            for (String testId : selectedTestIds) {
                Test test = testService.get(testId);
                if (test != null) {
                    selectedTests.add(test);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createSamplesForEntry",
                            "Test not found: " + testId);
                }
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createSamplesForEntry",
                "Creating samples with " + selectedTests.size() + " tests for entry " + entryId);

        int totalRequested = 0;
        int analysesCreated = 0;

        // Get the main accession number generator to generate proper accession numbers
        IAccessionNumberGenerator accessionGenerator = null;
        try {
            accessionGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "createSamplesForEntry",
                    "AccessionNumberUtil not available - using timestamp for accession numbers");
        }

        // Get status IDs
        String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
        if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
            sampleEnteredStatusId = "20";
        }

        String analysisNotStartedStatusId = statusService.getStatusID(AnalysisStatus.NotStarted);
        if (analysisNotStartedStatusId == null || "-1".equals(analysisNotStartedStatusId)) {
            analysisNotStartedStatusId = "1";
        }

        for (ManifestRow row : manifest.rows()) {
            if (row.numOfSamples() <= 0) {
                continue;
            }

            totalRequested += row.numOfSamples();

            // Look up sample type
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (sampleType == null) {
                errors.add(new ParseError(row.rowNumber(), "sample_type", "Unknown sample type: " + row.sampleType()));
                continue;
            }

            // Create individual Sample and SampleItem records - each with its own unique
            // accession number
            // This ensures each sample has a unique Lab Number for proper tracking through
            // the workflow
            for (int seq = 1; seq <= row.numOfSamples(); seq++) {
                // Generate a UNIQUE accession number for EACH sample
                String accessionNumber = accessionGenerator != null
                        ? accessionGenerator.getNextAccessionNumber(null, true)
                        : String.valueOf(System.currentTimeMillis()) + "-" + seq;

                Sample sample = new Sample();
                sample.setSysUserId(sysUserId);
                sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
                sample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
                sample.setAccessionNumber(accessionNumber);
                String sampleId = sampleService.insert(sample);
                sample.setId(sampleId);

                SampleItem item = new SampleItem();
                item.setSample(sample);
                item.setTypeOfSample(sampleType);
                item.setExternalId(generateExternalId(row.groupId(), seq));
                item.setSortOrder("1"); // Each sample has only one item now
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                // Set collection date from manifest row
                if (row.collectionDate() != null && !row.collectionDate().isBlank()) {
                    java.sql.Timestamp collectionTimestamp = parseCollectionDate(row.collectionDate());
                    if (collectionTimestamp != null) {
                        item.setCollectionDate(collectionTimestamp);
                    }
                }

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                createdSamples.add(item);

                // Create Analysis records for each selected test
                for (Test test : selectedTests) {
                    Analysis analysis = new Analysis();
                    analysis.setSampleItem(item);
                    analysis.setTest(test);
                    analysis.setAnalysisType("MANUAL");
                    analysis.setStatusId(analysisNotStartedStatusId);
                    analysis.setSysUserId(sysUserId);
                    analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
                    analysisService.insert(analysis);
                    analysesCreated++;
                }

                // Add sample to entry
                notebookEntryService.addSample(entryId, item, sysUserId);
            }
        }

        if (!createdSamples.isEmpty()) {
            List<Integer> createdIds = createdSamples.stream().map(s -> Integer.parseInt(s.getId()))
                    .collect(Collectors.toList());
            notebookSampleEntryService.linkSamplesToNotebook(notebookId, createdIds);
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createSamplesForEntry",
                "Created " + createdSamples.size() + " samples and " + analysesCreated + " analysis records");

        return new ManifestImportResult(totalRequested, createdSamples.size(), createdSamples, errors);
    }

    @Override
    public String generateExternalId(String groupId, int sequenceNumber) {
        return String.format("%s-%03d", groupId, sequenceNumber);
    }

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
     * Parse collection date from various formats. Supports: yyyy-MM-dd, dd/MM/yyyy,
     * MM/dd/yyyy, dd-MM-yyyy
     */
    private java.sql.Timestamp parseCollectionDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        String trimmed = dateStr.trim();
        java.time.LocalDate date = null;

        // Try different date formats
        String[] formats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "yyyy/MM/dd" };

        for (String format : formats) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
                date = java.time.LocalDate.parse(trimmed, formatter);
                break;
            } catch (java.time.format.DateTimeParseException e) {
                // Try next format
            }
        }

        if (date != null) {
            return java.sql.Timestamp.valueOf(date.atStartOfDay());
        }

        return null;
    }
}
