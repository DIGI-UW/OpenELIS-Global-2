package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.ImmunologyManifestImportForm;
import org.openelisglobal.notebook.service.ImmunologyManifestImportService.ImmunologyManifestImportResult;
import org.openelisglobal.notebook.service.ImmunologyManifestImportService.ImmunologyManifestRow;
import org.openelisglobal.notebook.service.ImmunologyManifestImportService.ParseError;
import org.openelisglobal.notebook.service.ImmunologyManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
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
 * Immunology laboratory manifest import: parses immunology-specific CSV,
 * creates sample + sample items, links to notebook entry, and stores reception
 * metadata on page 1 NotebookPageSample data.
 *
 * Reception Metadata: - Required: uniqueParentSampleId, projectNameId,
 * deliveryManifestReference, collectionDateTime, receptionDateTime,
 * sourceOrigin - Sample Type: validated against Immunology lab types -
 * Optional: sampleVolume, storageConditionOnArrival, transportTemperature,
 * receivingPersonnelName, manifestVerificationStatus, patientId, notes -
 * Auto-generated: accessionNumber, barcodeQrCode
 */
@Service
public class ImmunologyManifestImportServiceImpl implements ImmunologyManifestImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Valid sample types for the Immunology laboratory. Must match descriptions in
     * type_of_sample table from Liquibase scripts.
     */
    private static final java.util.Set<String> VALID_IMMUNOLOGY_SAMPLE_TYPES = java.util.Set.of(
            // Primary immunology sample types
            "whole blood", "pbmc", "serum", "plasma", "buffy coat",
            // Blood derivatives
            "dried blood spot", "cord blood", "peripheral blood",
            // Cellular samples
            "lymphocytes", "monocytes", "t cells", "b cells", "nk cells", "dendritic cells", "granulocytes",
            "neutrophils", "eosinophils",
            // Tissue and fluid samples
            "bone marrow", "spleen", "lymph node", "thymus", "cerebrospinal fluid", "synovial fluid", "pleural fluid",
            "ascitic fluid", "bronchoalveolar lavage",
            // Other biological fluids
            "saliva", "urine", "stool", "nasal swab", "throat swab",
            // Cell culture and derivatives
            "cell culture supernatant", "cell lysate", "cell pellet",
            // Special preparations
            "isolated rna", "isolated dna", "protein extract");

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
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private IStatusService statusService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, ImmunologyManifestImportForm columnMapping) {
        List<ImmunologyManifestRow> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                errors.add(new ParseError(0, "file", "CSV file is empty or has no header row"));
                return new ParsedManifest(rows, errors);
            }

            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Required field indexes
            Integer uniqueParentSampleIdIdx = getColumnIndex(columnIndex,
                    columnMapping.getUniqueParentSampleIdColumn());
            Integer projectNameIdIdx = getColumnIndex(columnIndex, columnMapping.getProjectNameIdColumn());
            Integer deliveryManifestReferenceIdx = getColumnIndex(columnIndex,
                    columnMapping.getDeliveryManifestReferenceColumn());
            Integer collectionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateTimeColumn());
            Integer receptionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceptionDateTimeColumn());
            Integer sourceOriginIdx = getColumnIndex(columnIndex, columnMapping.getSourceOriginColumn());

            // Sample type field index (optional but validated if provided)
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());

            // Optional field indexes
            Integer sampleVolumeIdx = getColumnIndex(columnIndex, columnMapping.getSampleVolumeColumn());
            Integer storageConditionOnArrivalIdx = getColumnIndex(columnIndex,
                    columnMapping.getStorageConditionOnArrivalColumn());
            Integer transportTemperatureIdx = getColumnIndex(columnIndex,
                    columnMapping.getTransportTemperatureColumn());
            Integer receivingPersonnelNameIdx = getColumnIndex(columnIndex,
                    columnMapping.getReceivingPersonnelNameColumn());
            Integer manifestVerificationStatusIdx = getColumnIndex(columnIndex,
                    columnMapping.getManifestVerificationStatusColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Required fields
                String uniqueParentSampleId = getValueAtIndex(values, uniqueParentSampleIdIdx);
                String projectNameId = getValueAtIndex(values, projectNameIdIdx);
                String deliveryManifestReference = getValueAtIndex(values, deliveryManifestReferenceIdx);
                String collectionDateTime = getValueAtIndex(values, collectionDateTimeIdx);
                String receptionDateTime = getValueAtIndex(values, receptionDateTimeIdx);
                String sourceOrigin = getValueAtIndex(values, sourceOriginIdx);

                // Sample type (optional but validated)
                String sampleType = getValueAtIndex(values, sampleTypeIdx);

                // Optional fields
                String sampleVolume = getValueAtIndex(values, sampleVolumeIdx);
                String storageConditionOnArrival = getValueAtIndex(values, storageConditionOnArrivalIdx);
                String transportTemperature = getValueAtIndex(values, transportTemperatureIdx);
                String receivingPersonnelName = getValueAtIndex(values, receivingPersonnelNameIdx);
                String manifestVerificationStatus = getValueAtIndex(values, manifestVerificationStatusIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                boolean hasError = false;

                if (uniqueParentSampleId == null || uniqueParentSampleId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "uniqueParentSampleId",
                            "Unique Parent Sample Identifier is required"));
                    hasError = true;
                }

                if (projectNameId == null || projectNameId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "projectNameId", "Project Name/ID is required"));
                    hasError = true;
                }

                if (deliveryManifestReference == null || deliveryManifestReference.isBlank()) {
                    errors.add(new ParseError(rowNumber, "deliveryManifestReference",
                            "Delivery Manifest Reference is required"));
                    hasError = true;
                }

                if (collectionDateTime == null || collectionDateTime.isBlank()) {
                    errors.add(new ParseError(rowNumber, "collectionDateTime", "Collection Date & Time is required"));
                    hasError = true;
                }

                if (receptionDateTime == null || receptionDateTime.isBlank()) {
                    errors.add(new ParseError(rowNumber, "receptionDateTime", "Reception Date & Time is required"));
                    hasError = true;
                }

                if (sourceOrigin == null || sourceOrigin.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sourceOrigin", "Source/Origin is required"));
                    hasError = true;
                }

                if (hasError) {
                    continue;
                }

                rows.add(new ImmunologyManifestRow(rowNumber, uniqueParentSampleId.trim(), projectNameId.trim(),
                        deliveryManifestReference.trim(), collectionDateTime.trim(), receptionDateTime.trim(),
                        sourceOrigin.trim(), sampleType, sampleVolume, storageConditionOnArrival, transportTemperature,
                        receivingPersonnelName, manifestVerificationStatus, patientId, notes));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateManifest(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (ImmunologyManifestRow row : manifest.rows()) {
            // Validate sample type against Immunology lab types and database
            if (row.sampleType() != null && !row.sampleType().isBlank()) {
                String sampleTypeLower = row.sampleType().toLowerCase().trim();

                // First check: Is it a valid immunology sample type?
                if (!VALID_IMMUNOLOGY_SAMPLE_TYPES.contains(sampleTypeLower)) {
                    errors.add(new ParseError(row.rowNumber(), "sampleType",
                            "Invalid sample type for Immunology lab: '" + row.sampleType()
                                    + "'. Valid types include: Whole Blood, PBMC, Serum, Plasma, Buffy Coat, etc."));
                } else {
                    // Second check: Does it exist in the system's type_of_sample table?
                    TypeOfSample searchType = new TypeOfSample();
                    searchType.setDescription(row.sampleType().trim());
                    TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
                    if (found == null) {
                        errors.add(new ParseError(row.rowNumber(), "sampleType", "Sample type '" + row.sampleType()
                                + "' is not configured in the system. Please contact an administrator to add this sample type."));
                    }
                }
            }

            // Validate date/time formats
            if (row.collectionDateTime() != null && !row.collectionDateTime().isBlank()) {
                if (!isValidDateTime(row.collectionDateTime())) {
                    errors.add(new ParseError(row.rowNumber(), "collectionDateTime",
                            "Invalid date/time format (expected YYYY-MM-DD HH:MM or YYYY-MM-DD): "
                                    + row.collectionDateTime()));
                }
            }

            if (row.receptionDateTime() != null && !row.receptionDateTime().isBlank()) {
                // Allow 'now' as a special value
                if (!row.receptionDateTime().equalsIgnoreCase("now") && !isValidDateTime(row.receptionDateTime())) {
                    errors.add(new ParseError(row.rowNumber(), "receptionDateTime",
                            "Invalid date/time format (expected YYYY-MM-DD HH:MM, YYYY-MM-DD, or 'now'): "
                                    + row.receptionDateTime()));
                }
            }

            // Validate reception is after collection
            if (row.collectionDateTime() != null && row.receptionDateTime() != null
                    && isValidDateTime(row.collectionDateTime()) && !row.receptionDateTime().equalsIgnoreCase("now")
                    && isValidDateTime(row.receptionDateTime())) {
                LocalDateTime collectionDt = parseDateTime(row.collectionDateTime());
                LocalDateTime receptionDt = parseDateTime(row.receptionDateTime());
                if (collectionDt != null && receptionDt != null && receptionDt.isBefore(collectionDt)) {
                    errors.add(new ParseError(row.rowNumber(), "receptionDateTime",
                            "Reception Date/Time must be after Collection Date/Time"));
                }
            }

            // Validate manifest verification status values
            if (row.manifestVerificationStatus() != null && !row.manifestVerificationStatus().isBlank()) {
                String status = row.manifestVerificationStatus().toLowerCase();
                if (!status.equals("verified") && !status.equals("pending") && !status.equals("discrepancy")) {
                    errors.add(new ParseError(row.rowNumber(), "manifestVerificationStatus",
                            "Invalid verification status. Expected: Verified, Pending, or Discrepancy"));
                }
            }

            // Validate storage condition values
            if (row.storageConditionOnArrival() != null && !row.storageConditionOnArrival().isBlank()) {
                String condition = row.storageConditionOnArrival().toLowerCase();
                if (!condition.contains("ambient") && !condition.contains("refrigerated")
                        && !condition.contains("frozen") && !condition.contains("dry ice")
                        && !condition.contains("liquid nitrogen") && !condition.contains("room temp")
                        && !condition.contains("2-8") && !condition.contains("-20") && !condition.contains("-80")) {
                    // Just warn, don't reject - allow free text
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public ImmunologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new ImmunologyManifestImportResult(0, 0, errors, createdAccessionNumbers);
        }

        NotebookEntry entry = optEntry.get();
        NoteBook notebook = entry.getNotebook();
        Integer firstPageId = null;
        if (notebook != null) {
            Hibernate.initialize(notebook.getPages());
            List<NoteBookPage> pages = new ArrayList<>(notebook.getPages());
            pages.sort((a, b) -> {
                Integer o1 = a.getOrder() != null ? a.getOrder() : Integer.MAX_VALUE;
                Integer o2 = b.getOrder() != null ? b.getOrder() : Integer.MAX_VALUE;
                return o1.compareTo(o2);
            });
            if (!pages.isEmpty()) {
                firstPageId = pages.get(0).getId();
            }
        }

        String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
        if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
            sampleEnteredStatusId = "20";
        }

        // Get default sample type for immunology (Whole Blood)
        TypeOfSample defaultSampleType = getDefaultImmunologySampleType();

        int totalRequested = manifest.rows().size();
        int totalCreated = 0;

        for (ImmunologyManifestRow row : manifest.rows()) {
            // Determine sample type - use from CSV if provided and valid, otherwise default
            TypeOfSample sampleTypeToUse = defaultSampleType;
            if (row.sampleType() != null && !row.sampleType().isBlank()) {
                TypeOfSample searchType = new TypeOfSample();
                searchType.setDescription(row.sampleType().trim());
                TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
                if (found != null) {
                    sampleTypeToUse = found;
                }
            }

            // Create parent sample
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
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

            // Create sample item
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleTypeToUse);
            item.setExternalId(row.uniqueParentSampleId());
            item.setSortOrder("1");
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            String itemId = sampleItemService.insert(item);
            item.setId(itemId);
            totalCreated++;
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

            notebookEntryService.addSample(entryId, item, sysUserId);

            // Store reception metadata on page 1 NotebookPageSample
            if (firstPageId != null) {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(firstPageId,
                        Integer.parseInt(itemId));
                if (nps != null) {
                    Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                    // Auto-generated fields
                    data.put("barcodeQrCode", generateBarcodeQrCode(parentSample.getAccessionNumber()));

                    // Required fields from CSV
                    data.put("uniqueParentSampleId", row.uniqueParentSampleId());
                    data.put("projectNameId", row.projectNameId());
                    data.put("deliveryManifestReference", row.deliveryManifestReference());
                    data.put("collectionDateTime", row.collectionDateTime());

                    // Handle 'now' for reception date/time
                    if (row.receptionDateTime() != null && row.receptionDateTime().equalsIgnoreCase("now")) {
                        data.put("receptionDateTime",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    } else {
                        data.put("receptionDateTime", row.receptionDateTime());
                    }

                    data.put("sourceOrigin", row.sourceOrigin());

                    // Sample type from CSV (or default)
                    data.put("sampleType", sampleTypeToUse.getDescription());

                    // Optional fields from CSV
                    if (row.sampleVolume() != null && !row.sampleVolume().isBlank()) {
                        data.put("sampleVolume", row.sampleVolume());
                    }
                    if (row.storageConditionOnArrival() != null && !row.storageConditionOnArrival().isBlank()) {
                        data.put("storageConditionOnArrival", row.storageConditionOnArrival());
                    }
                    if (row.transportTemperature() != null && !row.transportTemperature().isBlank()) {
                        data.put("transportTemperature", row.transportTemperature());
                    }
                    if (row.receivingPersonnelName() != null && !row.receivingPersonnelName().isBlank()) {
                        data.put("receivingPersonnelName", row.receivingPersonnelName());
                    }
                    if (row.manifestVerificationStatus() != null && !row.manifestVerificationStatus().isBlank()) {
                        data.put("manifestVerificationStatus", row.manifestVerificationStatus());
                    }
                    if (row.patientId() != null && !row.patientId().isBlank()) {
                        data.put("patientId", row.patientId());
                    }
                    if (row.notes() != null && !row.notes().isBlank()) {
                        data.put("notes", row.notes());
                    }

                    // Set sample category
                    data.put("sampleCategory", "Immunology");

                    nps.setData(data);
                    notebookPageSampleService.update(nps);
                }
            }
        }

        return new ImmunologyManifestImportResult(totalRequested, totalCreated, errors,
                createdAccessionNumbers.stream().distinct().toList());
    }

    /**
     * Generate a barcode/QR code value based on accession number.
     */
    private String generateBarcodeQrCode(String accessionNumber) {
        return "QR-" + accessionNumber;
    }

    /**
     * Get default sample type for immunology samples.
     */
    private TypeOfSample getDefaultImmunologySampleType() {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription("Whole Blood");
        TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        if (found == null) {
            // Fallback to PBMC if whole blood not found
            searchType.setDescription("PBMC");
            found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        }
        if (found == null) {
            // Fallback to Serum
            searchType.setDescription("Serum");
            found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        }
        return found;
    }

    /**
     * Validate date/time format (YYYY-MM-DD HH:MM or YYYY-MM-DD).
     */
    private boolean isValidDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return false;
        }
        try {
            // Try datetime format first
            LocalDateTime.parse(dateTimeStr.trim(), DATE_TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            try {
                // Try date-only format
                java.time.LocalDate.parse(dateTimeStr.trim(), DATE_FORMATTER);
                return true;
            } catch (DateTimeParseException e2) {
                return false;
            }
        }
    }

    /**
     * Parse date/time string to LocalDateTime.
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                // Parse as date-only and assume midnight
                java.time.LocalDate date = java.time.LocalDate.parse(dateTimeStr.trim(), DATE_FORMATTER);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

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

    private Integer getColumnIndex(Map<String, Integer> columnIndex, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        return columnIndex.get(columnName.trim().toLowerCase());
    }

    private String getValueAtIndex(String[] values, Integer index) {
        if (index == null || index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getValidImmunologySampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_IMMUNOLOGY_SAMPLE_TYPES) {
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
}
