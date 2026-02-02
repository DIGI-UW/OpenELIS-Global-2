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
import org.openelisglobal.notebook.form.BacteriologyManifestImportForm;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.util.AccessionNumberHandler;

/**
 * Implementation of BacteriologyManifestImportService for Bacteriology-specific
 * CSV manifest processing. Handles Bacteriology data points for clinical,
 * environmental, food/beverage, and veterinary samples.
 */
@Service
public class BacteriologyManifestImportServiceImpl implements BacteriologyManifestImportService {

    /**
     * Valid Bacteriology sample types. These are the sample types recognized for
     * the Bacteriology laboratory workflow. Includes clinical, environmental, food,
     * and veterinary samples.
     */
    private static final java.util.Set<String> VALID_BACTERIOLOGY_SAMPLE_TYPES = java.util.Set.of(
            // Clinical Samples (Human)
            "blood", "urine", "stool",
            // Body Fluids
            "csf (cerebrospinal fluid)", "pleural fluid", "peritoneal fluid", "pericardial fluid", "amniotic fluid",
            "synovial fluid", "other body fluid",
            // Isolates, Swabs, Tissue
            "bacterial isolate", "swab", "tissue sample",
            // Environmental Samples
            "wastewater", "tap water", "farm water", "soil", "other environmental sample",
            // Food and Beverage Samples
            "vegetables", "dry foods", "dairy products", "poultry products", "canned foods", "fruits",
            "packed/processed foods", "other food sample",
            // Veterinary Samples
            "animal stool", "animal tissue", "other animal specimen");

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private IStatusService statusService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, BacteriologyManifestImportForm columnMapping) {
        List<BacteriologyManifestRow> rows = new ArrayList<>();
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

            // Get column indices from Bacteriology mapping
            Integer projectNameIdx = getColumnIndex(columnIndex, columnMapping.getProjectNameColumn());
            Integer studyIdIdx = getColumnIndex(columnIndex, columnMapping.getStudyIdColumn());
            Integer participantIdIdx = getColumnIndex(columnIndex, columnMapping.getParticipantIdColumn());
            Integer barcodeIdx = getColumnIndex(columnIndex, columnMapping.getBarcodeColumn());
            Integer collectionSiteIdx = getColumnIndex(columnIndex, columnMapping.getCollectionSiteColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer collectionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateTimeColumn());
            Integer sampleReceivedDateIdx = getColumnIndex(columnIndex, columnMapping.getSampleReceivedDateColumn());
            Integer sampleArrivalTimeIdx = getColumnIndex(columnIndex, columnMapping.getSampleArrivalTimeColumn());
            Integer receivedByIdx = getColumnIndex(columnIndex, columnMapping.getReceivedByColumn());
            Integer storageContainerTypeIdx = getColumnIndex(columnIndex,
                    columnMapping.getStorageContainerTypeColumn());
            Integer storageTemperatureIdx = getColumnIndex(columnIndex,
                    columnMapping.getStorageTemperatureOnArrivalColumn());
            Integer consentStatusIdx = getColumnIndex(columnIndex, columnMapping.getConsentStatusColumn());
            Integer crfStatusIdx = getColumnIndex(columnIndex, columnMapping.getCrfStatusColumn());
            Integer sampleOriginIdx = getColumnIndex(columnIndex, columnMapping.getSampleOriginColumn());
            Integer sourceLocationIdx = getColumnIndex(columnIndex, columnMapping.getSourceLocationFacilityColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract Bacteriology values
                String projectName = getValueAtIndex(values, projectNameIdx);
                String studyId = getValueAtIndex(values, studyIdIdx);
                String participantId = getValueAtIndex(values, participantIdIdx);
                String barcode = getValueAtIndex(values, barcodeIdx);
                String collectionSite = getValueAtIndex(values, collectionSiteIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String collectionDateTime = getValueAtIndex(values, collectionDateTimeIdx);
                String sampleReceivedDate = getValueAtIndex(values, sampleReceivedDateIdx);
                String sampleArrivalTime = getValueAtIndex(values, sampleArrivalTimeIdx);
                String receivedBy = getValueAtIndex(values, receivedByIdx);
                String storageContainerType = getValueAtIndex(values, storageContainerTypeIdx);
                String storageTemperature = getValueAtIndex(values, storageTemperatureIdx);
                String consentStatus = getValueAtIndex(values, consentStatusIdx);
                String crfStatus = getValueAtIndex(values, crfStatusIdx);
                String sampleOrigin = getValueAtIndex(values, sampleOriginIdx);
                String sourceLocation = getValueAtIndex(values, sourceLocationIdx);

                // Validate required fields
                if (barcode == null || barcode.isBlank()) {
                    errors.add(new ParseError(rowNumber, "barcode", "Barcode is required"));
                    continue;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleType", "Sample type is required"));
                    continue;
                }

                if (sampleOrigin == null || sampleOrigin.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleOrigin", "Sample origin is required"));
                    continue;
                }

                rows.add(new BacteriologyManifestRow(rowNumber, projectName, studyId, participantId, barcode.trim(),
                        collectionSite, sampleType.trim(), collectionDateTime, sampleReceivedDate, sampleArrivalTime,
                        receivedBy, storageContainerType, storageTemperature, consentStatus, crfStatus,
                        sampleOrigin.trim(), sourceLocation));
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

        for (BacteriologyManifestRow row : manifest.rows()) {
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
    public BacteriologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new BacteriologyManifestImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        NotebookEntry entry = optEntry.get();
        int totalRequested = manifest.rows().size();
        int sequenceNumber = 1;

        for (BacteriologyManifestRow row : manifest.rows()) {
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
            String sampleIdDb;
            try {
                AccessionNumberHandler handler = new AccessionNumberHandler(sampleService, sampleDAO,
                        entityManager, this.getClass());
                sampleIdDb = handler.generateAndInsertWithUniqueAccessionNumber(parentSample);
                parentSample.setId(sampleIdDb);
            } catch (DuplicateAccessionNumberException e) {
                errors.add(new ParseError(row.rowNumber(), "sample",
                        "Failed to generate unique accession number: " + e.getMessage()));
                LogEvent.logError("Duplicate accession number error for row " + row.rowNumber(), e);
                continue;
            }

            // Get status ID for SampleEntered
            String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
            if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
                sampleEnteredStatusId = "20";
            }

            // Create SampleItem record
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleType);
            item.setExternalId(generateExternalId(row.barcode(), sequenceNumber));
            item.setSortOrder(Integer.toString(sequenceNumber));
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            // Set collection date from manifest row
            if (row.collectionDateTime() != null && !row.collectionDateTime().isBlank()) {
                java.sql.Timestamp collectionTimestamp = parseCollectionDate(row.collectionDateTime());
                if (collectionTimestamp != null) {
                    item.setCollectionDate(collectionTimestamp);
                }
            }

            String itemId = sampleItemService.insert(item);
            item.setId(itemId);
            createdSamples.add(item);
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

            // Add sample to entry
            notebookEntryService.addSample(entryId, item, sysUserId);
            sequenceNumber++;
        }

        if (!createdSamples.isEmpty()) {
            List<Integer> createdIds = createdSamples.stream().map(s -> Integer.parseInt(s.getId()))
                    .collect(Collectors.toList());
            // Use notebook ID from entry, not entry ID
            Integer notebookId = entry.getNotebook() != null ? entry.getNotebook().getId() : null;
            if (notebookId != null) {
                notebookSampleEntryService.linkSamplesToNotebook(notebookId, createdIds);
            }
        }

        return new BacteriologyManifestImportResult(totalRequested, createdSamples.size(), createdSamples,
                createdAccessionNumbers.stream().distinct().collect(Collectors.toList()), errors);
    }

    @Override
    public String generateExternalId(String barcode, int sequenceNumber) {
        return String.format("%s-%03d", barcode, sequenceNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getValidBacteriologySampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_BACTERIOLOGY_SAMPLE_TYPES) {
            // Check if this type exists in the database
            TypeOfSample searchType = new TypeOfSample();
            // Use title case for database lookup since descriptions are stored that way
            String titleCaseType = toTitleCase(validType);
            searchType.setDescription(titleCaseType);
            TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (found != null) {
                Map<String, String> typeMap = new HashMap<>();
                typeMap.put("id", found.getId());
                typeMap.put("description", found.getDescription());
                result.add(typeMap);
            }
        }

        // Sort by description for consistent ordering
        result.sort((a, b) -> a.get("description").compareToIgnoreCase(b.get("description")));

        return result;
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
     * Convert a string to title case (first letter of each word capitalized).
     */
    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '/' || c == '-' || c == '(') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Parse collection date from various formats. Supports: yyyy-MM-dd, dd/MM/yyyy,
     * MM/dd/yyyy, dd-MM-yyyy, yyyy-MM-dd HH:mm
     */
    private java.sql.Timestamp parseCollectionDate(String dateStr) {
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
