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
import org.openelisglobal.notebook.form.PathologyManifestImportForm;
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
 * Implementation of PathologyManifestImportService for Pathology-specific CSV
 * manifest processing. Handles pathology data points including clinical
 * metadata (patient ID, requesting clinician, clinical details) and research
 * metadata (study ID, PI name, participant/animal ID, ethical approval
 * reference).
 */
@Service
public class PathologyManifestImportServiceImpl implements PathologyManifestImportService {

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
    public ParsedManifest parseManifestCsv(InputStream csvInput, PathologyManifestImportForm columnMapping) {
        List<PathologyManifestRow> rows = new ArrayList<>();
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

            // Get column indices from Pathology mapping
            Integer groupIdIdx = getColumnIndex(columnIndex, columnMapping.getGroupIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer sampleCategoryIdx = getColumnIndex(columnIndex, columnMapping.getSampleCategoryColumn());
            Integer sourceFacilityIdx = getColumnIndex(columnIndex, columnMapping.getSourceFacilityColumn());
            Integer specimenSiteIdx = getColumnIndex(columnIndex, columnMapping.getSpecimenSiteColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer requestingClinicianIdx = getColumnIndex(columnIndex, columnMapping.getRequestingClinicianColumn());
            Integer clinicalDetailsIdx = getColumnIndex(columnIndex, columnMapping.getClinicalDetailsColumn());
            Integer studyIdIdx = getColumnIndex(columnIndex, columnMapping.getStudyIdColumn());
            Integer piNameIdx = getColumnIndex(columnIndex, columnMapping.getPiNameColumn());
            Integer participantAnimalIdIdx = getColumnIndex(columnIndex, columnMapping.getParticipantAnimalIdColumn());
            Integer ethicalApprovalRefIdx = getColumnIndex(columnIndex, columnMapping.getEthicalApprovalRefColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract Pathology values
                String groupId = getValueAtIndex(values, groupIdIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);
                String collectionDate = getValueAtIndex(values, collectionDateIdx);
                String sampleCategory = getValueAtIndex(values, sampleCategoryIdx);
                String sourceFacility = getValueAtIndex(values, sourceFacilityIdx);
                String specimenSite = getValueAtIndex(values, specimenSiteIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String requestingClinician = getValueAtIndex(values, requestingClinicianIdx);
                String clinicalDetails = getValueAtIndex(values, clinicalDetailsIdx);
                String studyId = getValueAtIndex(values, studyIdIdx);
                String piName = getValueAtIndex(values, piNameIdx);
                String participantAnimalId = getValueAtIndex(values, participantAnimalIdIdx);
                String ethicalApprovalRef = getValueAtIndex(values, ethicalApprovalRefIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                if (groupId == null || groupId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "groupId", "Group ID is required"));
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

                rows.add(new PathologyManifestRow(rowNumber, groupId.trim(), sampleType.trim(), numOfSamples,
                        collectionDate, sampleCategory, sourceFacility, specimenSite, patientId, requestingClinician,
                        clinicalDetails, studyId, piName, participantAnimalId, ethicalApprovalRef, notes));
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

        for (PathologyManifestRow row : manifest.rows()) {
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
    public List<ParseError> validateCategoryMetadata(ParsedManifest manifest) {
        List<ParseError> warnings = new ArrayList<>();

        for (PathologyManifestRow row : manifest.rows()) {
            if (row.isClinical()) {
                // Clinical samples should have patient ID
                if (row.patientId() == null || row.patientId().isBlank()) {
                    warnings.add(new ParseError(row.rowNumber(), "patientId",
                            "Clinical sample missing Patient ID (recommended)"));
                }
            } else if (row.isResearch()) {
                // Research samples should have study ID and PI name
                if (row.studyId() == null || row.studyId().isBlank()) {
                    warnings.add(new ParseError(row.rowNumber(), "studyId",
                            "Research sample missing Study ID (recommended)"));
                }
            }
        }

        return warnings;
    }

    @Override
    @Transactional
    public PathologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new PathologyManifestImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        NotebookEntry entry = optEntry.get();
        int totalRequested = 0;

        for (PathologyManifestRow row : manifest.rows()) {
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

            // Note: clinicalOrderId is a NUMERIC column and cannot store text metadata
            // Pathology-specific metadata will be stored in the NotebookPageSample's JSONB
            // data field

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
                item.setExternalId(generateExternalId(row.groupId(), seq));
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

                // Note: Specimen site is stored in the clinical history metadata
                // sourceOfSampleId is a foreign key and requires a valid ID from
                // source_of_sample table

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

        return new PathologyManifestImportResult(totalRequested, createdSamples.size(), createdSamples,
                createdAccessionNumbers.stream().distinct().collect(Collectors.toList()), errors);
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
