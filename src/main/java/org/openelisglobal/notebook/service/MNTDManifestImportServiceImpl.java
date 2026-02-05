package org.openelisglobal.notebook.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.MNTDManifestImportForm;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberHandler;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of MNTDManifestImportService for MNTD-specific CSV manifest
 * processing. Handles MNTD data points: Sample ID, Sample Source, Project Name,
 * Collection Site, Collection DateTime, Collected By.
 */
@Service
public class MNTDManifestImportServiceImpl implements MNTDManifestImportService {

    /**
     * Valid MNTD sample types. These are the sample types recognized for the MNTD
     * (Malaria and Neglected Tropical Diseases) laboratory workflow. Includes
     * parasite samples and vector samples.
     */
    private static final java.util.Set<String> VALID_MNTD_SAMPLE_TYPES = java.util.Set.of(
            // Parasite Samples (Human/Animal)
            "whole blood", "serum", "plasma", "dried blood spots (dbs)", "cell pellets", "cultured parasites",
            "skin slit smear", "biopsy", "microbiopsy", "tissue aspirate", "dnashield", "rna protect", "cell culture",
            "tissue in saline",
            // Vector Samples
            "colony mosquito", "wild mosquito", "mosquito head and thorax", "mosquito abdomen", "sandfly", "tsetse fly",
            // Legacy/existing types that may be used
            "cell pellet", "culture", "dna/rna preservative", "mosquito / sandfly / tsetsefly", "biopsy / microbiopsy");

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
    public ParsedManifest parseManifestCsv(InputStream csvInput, MNTDManifestImportForm columnMapping) {
        List<MNTDManifestRow> rows = new ArrayList<>();
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

            // Get column indices from MNTD mapping (using new field names)
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdTagColumn());
            Integer sampleSourceIdx = getColumnIndex(columnIndex, columnMapping.getSampleSourceLocationColumn());
            Integer projectNameIdx = getColumnIndex(columnIndex, columnMapping.getProjectNameColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer collectionSiteIdx = getColumnIndex(columnIndex, columnMapping.getSampleSourceLocationColumn()); // Reuse
                                                                                                                    // source
                                                                                                                    // location
            Integer collectionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceivedDateTimeColumn());
            Integer collectedByIdx = getColumnIndex(columnIndex, columnMapping.getBroughtByColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumberOfSamplesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract MNTD values
                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String sampleSource = getValueAtIndex(values, sampleSourceIdx);
                String projectName = getValueAtIndex(values, projectNameIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String collectionSite = getValueAtIndex(values, collectionSiteIdx);
                String collectionDateTime = getValueAtIndex(values, collectionDateTimeIdx);
                String collectedBy = getValueAtIndex(values, collectedByIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);

                // Validate required fields
                if (sampleId == null || sampleId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleId", "Sample ID is required"));
                    continue;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleType", "Sample type is required"));
                    continue;
                }

                // Parse num_of_samples - extract leading number from strings like "10 tubes",
                // "2 boxes"
                int numOfSamples = parseNumberOfSamples(numOfSamplesStr);
                if (numOfSamples <= 0) {
                    errors.add(new ParseError(rowNumber, "numOfSamples",
                            "Invalid number format (expected number or 'N units'): " + numOfSamplesStr));
                    continue;
                }

                rows.add(new MNTDManifestRow(rowNumber, sampleId.trim(), sampleSource, projectName, sampleType.trim(),
                        collectionSite, collectionDateTime, collectedBy, numOfSamples));
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

        for (MNTDManifestRow row : manifest.rows()) {
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
    public MNTDManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId,
            String manifestDescription) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new MNTDManifestImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        NotebookEntry entry = optEntry.get();

        // Store manifest description if provided
        if (manifestDescription != null && !manifestDescription.isBlank()) {
            entry.setManifestDescription(manifestDescription.trim());
            entry.setSysUserId(sysUserId);
            notebookEntryService.update(entry);
        }
        int totalRequested = 0;

        for (MNTDManifestRow row : manifest.rows()) {
            if (row.numOfSamples() <= 0) {
                continue;
            }

            totalRequested += row.numOfSamples();

            // Look up sample type
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (sampleType == null) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Unknown sample type: " + row.sampleType()));
                continue;
            }

            // Create a parent Sample record for this batch with generated accession number
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            String sampleIdDb;
            try {
                AccessionNumberHandler handler = new AccessionNumberHandler(sampleService, sampleDAO, entityManager,
                        this.getClass());
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

            // Create individual SampleItem records
            for (int seq = 1; seq <= row.numOfSamples(); seq++) {
                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setTypeOfSample(sampleType);
                item.setExternalId(generateExternalId(row.sampleId(), seq));
                item.setSortOrder(Integer.toString(seq));
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
            }
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

        return new MNTDManifestImportResult(totalRequested, createdSamples.size(), createdSamples,
                createdAccessionNumbers.stream().distinct().collect(Collectors.toList()), errors);
    }

    @Override
    public String generateExternalId(String sampleId, int sequenceNumber) {
        return String.format("%s-%03d", sampleId, sequenceNumber);
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
     * Parse number of samples from various formats. Accepts: "10", "10 tubes", "2
     * boxes", "1 bag (50 specimens)", etc. Extracts the leading integer value.
     *
     * @param value the string to parse
     * @return the extracted number, or 1 if null/blank, or -1 if invalid
     */
    private int parseNumberOfSamples(String value) {
        if (value == null || value.isBlank()) {
            return 1; // Default to 1 if not specified
        }

        String trimmed = value.trim();

        // Try direct integer parse first
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // Not a plain integer, try to extract leading number
        }

        // Extract leading digits using regex
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1; // No valid number found
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getValidMntdSampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_MNTD_SAMPLE_TYPES) {
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
