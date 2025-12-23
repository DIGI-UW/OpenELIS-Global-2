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
 * manifest processing.
 *
 * Handles pathology data points including: - Patient Identification: First Name
 * (MANDATORY), Surname (optional), National ID (optional) - Clinical metadata:
 * Patient ID, requesting clinician, clinical details - Research metadata: Study
 * ID, PI name, participant/animal ID, ethical approval ref - All samples:
 * Receiving date/time, receiving staff name, source facility
 */
@Service
public class PathologyManifestImportServiceImpl implements PathologyManifestImportService {

    /**
     * Valid Pathology sample types. These are the sample types recognized for the
     * Pathology laboratory workflow. Includes histopathology, cytopathology,
     * hematology, and research specimens.
     */
    private static final java.util.Set<String> VALID_PATHOLOGY_SAMPLE_TYPES = java.util.Set.of(
            // Histopathology
            "ffpe tissue block", "fresh biopsy", "surgical resection",
            // Cytopathology
            "fine needle aspirate (fna)", "pleural fluid", "peritoneal fluid", "pericardial fluid", "csf for cytology",
            "urine for cytology", "sputum for cytology", "cervical smear",
            // Hematology
            "edta blood (peripheral smear)",
            // Research specimens
            "human tissue (fresh)", "human tissue (frozen)", "human tissue (ffpe)", "animal tissue (mouse footpad)",
            "animal tissue (nerve)", "bacterial/cellular pellet", "primary cell culture",
            // Processed samples
            "tissue slide", "cell block", "lbc (liquid-based cytology) vial", "blood smear slide",
            "nucleic acid extract");

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
    public ParsedManifest parseManifestCsv(InputStream csvInput, PathologyManifestImportForm columnMapping,
            String sampleCategory) {
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
            // Patient Identification
            Integer firstNameIdx = getColumnIndex(columnIndex, columnMapping.getFirstNameColumn());
            Integer surnameIdx = getColumnIndex(columnIndex, columnMapping.getSurnameColumn());
            Integer nationalIdIdx = getColumnIndex(columnIndex, columnMapping.getNationalIdColumn());
            // Receiving Info
            Integer receivedDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceivedDateTimeColumn());
            Integer receivedByIdx = getColumnIndex(columnIndex, columnMapping.getReceivedByColumn());
            Integer sourceFacilityIdx = getColumnIndex(columnIndex, columnMapping.getSourceFacilityColumn());
            // Specimen Info
            Integer specimenTypeIdx = getColumnIndex(columnIndex, columnMapping.getSpecimenTypeColumn());
            Integer specimenSiteIdx = getColumnIndex(columnIndex, columnMapping.getSpecimenSiteColumn());
            Integer collectionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateTimeColumn());
            // Clinical metadata
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer requestingClinicianIdx = getColumnIndex(columnIndex, columnMapping.getRequestingClinicianColumn());
            Integer clinicalDetailsIdx = getColumnIndex(columnIndex, columnMapping.getClinicalDetailsColumn());
            // Research metadata
            Integer studyIdIdx = getColumnIndex(columnIndex, columnMapping.getStudyIdColumn());
            Integer piNameIdx = getColumnIndex(columnIndex, columnMapping.getPiNameColumn());
            Integer participantAnimalIdIdx = getColumnIndex(columnIndex, columnMapping.getParticipantAnimalIdColumn());
            Integer ethicalApprovalRefIdx = getColumnIndex(columnIndex, columnMapping.getEthicalApprovalRefColumn());
            // Optional
            Integer remarksIdx = getColumnIndex(columnIndex, columnMapping.getRemarksColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract Pathology values
                // Patient Identification
                String firstName = getValueAtIndex(values, firstNameIdx);
                String surname = getValueAtIndex(values, surnameIdx);
                String nationalId = getValueAtIndex(values, nationalIdIdx);
                // Receiving Info
                String receivedDateTime = getValueAtIndex(values, receivedDateTimeIdx);
                String receivedBy = getValueAtIndex(values, receivedByIdx);
                String sourceFacility = getValueAtIndex(values, sourceFacilityIdx);
                // Specimen Info
                String specimenType = getValueAtIndex(values, specimenTypeIdx);
                String specimenSite = getValueAtIndex(values, specimenSiteIdx);
                String collectionDateTime = getValueAtIndex(values, collectionDateTimeIdx);
                // Clinical metadata
                String patientId = getValueAtIndex(values, patientIdIdx);
                String requestingClinician = getValueAtIndex(values, requestingClinicianIdx);
                String clinicalDetails = getValueAtIndex(values, clinicalDetailsIdx);
                // Research metadata
                String studyId = getValueAtIndex(values, studyIdIdx);
                String piName = getValueAtIndex(values, piNameIdx);
                String participantAnimalId = getValueAtIndex(values, participantAnimalIdIdx);
                String ethicalApprovalRef = getValueAtIndex(values, ethicalApprovalRefIdx);
                // Optional
                String remarks = getValueAtIndex(values, remarksIdx);

                // Validate MANDATORY fields
                // First Name is MANDATORY
                if (firstName == null || firstName.isBlank()) {
                    errors.add(new ParseError(rowNumber, "firstName", "First Name is MANDATORY for order acceptance"));
                    continue;
                }

                // Specimen Type is required
                if (specimenType == null || specimenType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "specimenType", "Specimen Type is required"));
                    continue;
                }

                // Use the sampleCategory passed from controller (determined by endpoint type)
                rows.add(new PathologyManifestRow(rowNumber, firstName.trim(), surname, nationalId, sampleCategory, // Use
                                                                                                                    // the
                                                                                                                    // category
                                                                                                                    // from
                                                                                                                    // endpoint,
                                                                                                                    // not
                                                                                                                    // CSV
                        receivedDateTime, receivedBy, sourceFacility, specimenType.trim(), specimenSite,
                        collectionDateTime, patientId, requestingClinician, clinicalDetails, studyId, piName,
                        participantAnimalId, ethicalApprovalRef, remarks));
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
            searchType.setDescription(row.specimenType());

            TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
            if (found == null) {
                errors.add(new ParseError(row.rowNumber(), "specimenType",
                        "Unknown specimen type: " + row.specimenType()));
            }
        }

        return errors;
    }

    @Override
    public List<ParseError> validateCategoryMetadata(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (PathologyManifestRow row : manifest.rows()) {
            // All samples must have receiving info
            if (row.receivedDateTime() == null || row.receivedDateTime().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "receivedDateTime",
                        "Receiving Date & Time is required for all samples"));
            }
            if (row.receivedBy() == null || row.receivedBy().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "receivedBy",
                        "Receiving Staff Name is required for all samples"));
            }

            // Collection DateTime is required
            if (row.collectionDateTime() == null || row.collectionDateTime().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "collectionDateTime", "Collection Date & Time is required"));
            }

            // Note: Specimen Site is OPTIONAL (not required)

            if (PathologyManifestImportService.isClinical(row)) {
                // Clinical samples require patient ID, requesting clinician, clinical details
                if (row.patientId() == null || row.patientId().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "patientId",
                            "Patient ID is required for Clinical diagnostic samples"));
                }
                if (row.requestingClinician() == null || row.requestingClinician().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "requestingClinician",
                            "Requesting Clinician is required for Clinical diagnostic samples"));
                }
                if (row.clinicalDetails() == null || row.clinicalDetails().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "clinicalDetails",
                            "Clinical Details/Indication is required for Clinical diagnostic samples"));
                }
            } else if (PathologyManifestImportService.isResearch(row)) {
                // Research samples require study ID, PI name, participant/animal ID, ethical
                // approval
                if (row.studyId() == null || row.studyId().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "studyId", "Study ID is required for Research samples"));
                }
                if (row.piName() == null || row.piName().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "piName",
                            "Principal Investigator (PI) Name is required for Research samples"));
                }
                if (row.participantAnimalId() == null || row.participantAnimalId().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "participantAnimalId",
                            "Participant ID / Animal ID is required for Research samples"));
                }
                if (row.ethicalApprovalRef() == null || row.ethicalApprovalRef().isBlank()) {
                    errors.add(new ParseError(row.rowNumber(), "ethicalApprovalRef",
                            "Ethical Approval Reference is required for Research samples"));
                }
            }
        }

        return errors;
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
        int totalRequested = manifest.rows().size();
        int sequenceNumber = 1;

        for (PathologyManifestRow row : manifest.rows()) {
            // Look up sample type
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.specimenType());
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);

            if (sampleType == null) {
                errors.add(new ParseError(row.rowNumber(), "specimenType",
                        "Unknown specimen type: " + row.specimenType()));
                continue;
            }

            // Create a parent Sample record with generated accession number
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));

            // Set received timestamp from manifest row
            if (row.receivedDateTime() != null && !row.receivedDateTime().isBlank()) {
                java.sql.Timestamp receivedTimestamp = parseDateTime(row.receivedDateTime());
                if (receivedTimestamp != null) {
                    parentSample.setReceivedTimestamp(receivedTimestamp);
                } else {
                    parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
                }
            } else {
                parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
            }

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
            item.setExternalId(generateExternalId(row.firstName(), row.specimenType(), sequenceNumber));
            item.setSortOrder("1");
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            // Set collection date from manifest row
            if (row.collectionDateTime() != null && !row.collectionDateTime().isBlank()) {
                java.sql.Timestamp collectionTimestamp = parseDateTime(row.collectionDateTime());
                if (collectionTimestamp != null) {
                    item.setCollectionDate(collectionTimestamp);
                }
            }

            String itemId = sampleItemService.insert(item);
            item.setId(itemId);
            createdSamples.add(item);
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

            // Add sample to entry with all metadata
            notebookEntryService.addSample(entryId, item, sysUserId);

            sequenceNumber++;
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
    public String generateExternalId(String firstName, String specimenType, int sequenceNumber) {
        // Generate abbreviation from specimen type (first 3 letters of significant
        // words)
        String abbrev = generateSpecimenTypeAbbrev(specimenType);
        // Sanitize first name (remove spaces, special chars)
        String sanitizedName = firstName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (sanitizedName.length() > 10) {
            sanitizedName = sanitizedName.substring(0, 10);
        }
        return String.format("%s-%s-%03d", sanitizedName, abbrev, sequenceNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getValidPathologySampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_PATHOLOGY_SAMPLE_TYPES) {
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
     * Generate abbreviation from specimen type.
     */
    private String generateSpecimenTypeAbbrev(String specimenType) {
        if (specimenType == null || specimenType.isBlank()) {
            return "UNK";
        }
        // Take first 3 letters of each significant word
        String[] words = specimenType.toUpperCase().split("[\\s\\-\\/\\(\\)]+");
        StringBuilder abbrev = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0 && !word.equals("FOR") && !word.equals("THE") && !word.equals("AND")) {
                abbrev.append(word.substring(0, Math.min(3, word.length())));
                if (abbrev.length() >= 6) {
                    break;
                }
            }
        }
        return abbrev.length() > 0 ? abbrev.toString() : "UNK";
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
     * Parse date/time from various formats. Supports: yyyy-MM-dd HH:mm, yyyy-MM-dd,
     * dd/MM/yyyy HH:mm, etc.
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
