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
import org.openelisglobal.notebook.form.ViralVaccineManifestImportForm;
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

/**
 * Implementation of ViralVaccineManifestImportService for Viral & Vaccine Unit
 * Laboratory CSV manifest processing. Handles Viral & Vaccine-specific data
 * points: Sample ID, Sample Name, Sample Type, Batch ID, Source Organism,
 * Passage Number, Collection Date, Collected By, Origin Lab, Storage Location,
 * Storage Temperature, Volume, Intended Use, and Project Name.
 */
@Service
public class ViralVaccineManifestImportServiceImpl implements ViralVaccineManifestImportService {

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

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, ViralVaccineManifestImportForm columnMapping) {
        List<ViralVaccineManifestRow> rows = new ArrayList<>();
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

            // Get column indices from Viral & Vaccine mapping
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());
            Integer sampleNameIdx = getColumnIndex(columnIndex, columnMapping.getSampleNameColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer batchIdIdx = getColumnIndex(columnIndex, columnMapping.getBatchIdColumn());
            Integer sourceOrganismIdx = getColumnIndex(columnIndex, columnMapping.getSourceOrganismColumn());
            Integer passageNumberIdx = getColumnIndex(columnIndex, columnMapping.getPassageNumberColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer collectedByIdx = getColumnIndex(columnIndex, columnMapping.getCollectedByColumn());
            Integer originLabIdx = getColumnIndex(columnIndex, columnMapping.getOriginLabColumn());
            Integer storageLocationIdx = getColumnIndex(columnIndex, columnMapping.getStorageLocationColumn());
            Integer storageTemperatureIdx = getColumnIndex(columnIndex, columnMapping.getStorageTemperatureColumn());
            Integer volumeIdx = getColumnIndex(columnIndex, columnMapping.getVolumeColumn());
            Integer intendedUseIdx = getColumnIndex(columnIndex, columnMapping.getIntendedUseColumn());
            Integer projectNameIdx = getColumnIndex(columnIndex, columnMapping.getProjectNameColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract Viral & Vaccine values
                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String sampleName = getValueAtIndex(values, sampleNameIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String batchId = getValueAtIndex(values, batchIdIdx);
                String sourceOrganism = getValueAtIndex(values, sourceOrganismIdx);
                String passageNumber = getValueAtIndex(values, passageNumberIdx);
                String collectionDate = getValueAtIndex(values, collectionDateIdx);
                String collectedBy = getValueAtIndex(values, collectedByIdx);
                String originLab = getValueAtIndex(values, originLabIdx);
                String storageLocation = getValueAtIndex(values, storageLocationIdx);
                String storageTemperature = getValueAtIndex(values, storageTemperatureIdx);
                String volume = getValueAtIndex(values, volumeIdx);
                String intendedUse = getValueAtIndex(values, intendedUseIdx);
                String projectName = getValueAtIndex(values, projectNameIdx);
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

                // Parse num_of_samples
                int numOfSamples;
                try {
                    numOfSamples = numOfSamplesStr != null && !numOfSamplesStr.isBlank()
                            ? Integer.parseInt(numOfSamplesStr.trim())
                            : 1;
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(rowNumber, "numOfSamples", "Invalid number format: " + numOfSamplesStr));
                    continue;
                }

                rows.add(new ViralVaccineManifestRow(rowNumber, sampleId.trim(), sampleName, sampleType.trim(), batchId,
                        sourceOrganism, passageNumber, collectionDate, collectedBy, originLab, storageLocation,
                        storageTemperature, volume, intendedUse, projectName, numOfSamples));
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

        for (ViralVaccineManifestRow row : manifest.rows()) {
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
    public ViralVaccineManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new ViralVaccineManifestImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        NotebookEntry entry = optEntry.get();
        int totalRequested = 0;

        for (ViralVaccineManifestRow row : manifest.rows()) {
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
            String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
            parentSample.setId(sampleIdDb);

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
                if (row.collectionDate() != null && !row.collectionDate().isBlank()) {
                    java.sql.Timestamp collectionTimestamp = parseCollectionDate(row.collectionDate());
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
            notebookSampleEntryService.linkSamplesToNotebook(entryId, createdIds);
        }

        return new ViralVaccineManifestImportResult(totalRequested, createdSamples.size(), createdSamples,
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
