package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.PharmaManifestImportForm;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParseError;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.PharmaManifestImportService.PharmaManifestImportResult;
import org.openelisglobal.notebook.service.PharmaManifestImportService.PharmaManifestRow;
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

/**
 * Pharmaceuticals manifest import: parses pharma-specific CSV, creates sample +
 * sample items, links to notebook entry, and stores metadata on page 1
 * NotebookPageSample data.
 *
 * Updated to support the new dataPoints schema: - Required: sampleName,
 * lotBatchNumber, dateOfManufacture, expiryRetestDate, storageCondition,
 * ownerRequester - Optional: sampleType (validated against Pharmaceutical lab
 * types), alphanumericCode, chemicalIupacName, gradeSpecification,
 * chainOfCustodyDetails, patientId, clinicalTrialNumber, consentStatus -
 * Auto-generated: uniqueSampleId, barcodeQrCode
 */
@Service
public class PharmaManifestImportServiceImpl implements PharmaManifestImportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Valid sample types for the Pharmaceutical laboratory. Must match descriptions
     * in type_of_sample table from Liquibase scripts.
     */
    private static final java.util.Set<String> VALID_PHARMA_SAMPLE_TYPES = java.util.Set.of(
            // From 013-pharma-sample-types.xml
            "finished dosage form", "api", "natural / herbal", "water (pw, wfi)",
            // From 019-pharma-department-sample-types.xml - Substances & Products
            "excipients", "bulk powders", "intermediates", "placebos", "reference standards", "impurity standards",
            "tablets", "capsules", "injections", "creams", "patches",
            // Natural & Herbal
            "plant root", "plant leaf", "plant stem", "plant flower",
            // Biological & Stability
            "blood", "plasma", "urine", "saliva", "cerebrospinal fluid", "animal tissues", "animal organs",
            "animal feces", "cell culture samples",
            // Microbiological & Environmental
            "microbiological cultures", "purified water", "water for injection", "surface swabs", "equipment swabs",
            "environmental swabs");

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

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, PharmaManifestImportForm columnMapping) {
        List<PharmaManifestRow> rows = new ArrayList<>();
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
            Integer sampleNameIdx = getColumnIndex(columnIndex, columnMapping.getSampleNameColumn());
            Integer lotBatchNumberIdx = getColumnIndex(columnIndex, columnMapping.getLotBatchNumberColumn());
            Integer dateOfManufactureIdx = getColumnIndex(columnIndex, columnMapping.getDateOfManufactureColumn());
            Integer expiryRetestDateIdx = getColumnIndex(columnIndex, columnMapping.getExpiryRetestDateColumn());
            Integer storageConditionIdx = getColumnIndex(columnIndex, columnMapping.getStorageConditionColumn());
            Integer ownerRequesterIdx = getColumnIndex(columnIndex, columnMapping.getOwnerRequesterColumn());

            // Sample type field index (optional but validated if provided)
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());

            // Optional field indexes
            Integer alphanumericCodeIdx = getColumnIndex(columnIndex, columnMapping.getAlphanumericCodeColumn());
            Integer chemicalIupacNameIdx = getColumnIndex(columnIndex, columnMapping.getChemicalIupacNameColumn());
            Integer gradeSpecificationIdx = getColumnIndex(columnIndex, columnMapping.getGradeSpecificationColumn());
            Integer chainOfCustodyDetailsIdx = getColumnIndex(columnIndex,
                    columnMapping.getChainOfCustodyDetailsColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer clinicalTrialNumberIdx = getColumnIndex(columnIndex, columnMapping.getClinicalTrialNumberColumn());
            Integer consentStatusIdx = getColumnIndex(columnIndex, columnMapping.getConsentStatusColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Required fields
                String sampleName = getValueAtIndex(values, sampleNameIdx);
                String lotBatchNumber = getValueAtIndex(values, lotBatchNumberIdx);
                String dateOfManufacture = getValueAtIndex(values, dateOfManufactureIdx);
                String expiryRetestDate = getValueAtIndex(values, expiryRetestDateIdx);
                String storageCondition = getValueAtIndex(values, storageConditionIdx);
                String ownerRequester = getValueAtIndex(values, ownerRequesterIdx);

                // Sample type (optional but validated)
                String sampleType = getValueAtIndex(values, sampleTypeIdx);

                // Optional fields
                String alphanumericCode = getValueAtIndex(values, alphanumericCodeIdx);
                String chemicalIupacName = getValueAtIndex(values, chemicalIupacNameIdx);
                String gradeSpecification = getValueAtIndex(values, gradeSpecificationIdx);
                String chainOfCustodyDetails = getValueAtIndex(values, chainOfCustodyDetailsIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String clinicalTrialNumber = getValueAtIndex(values, clinicalTrialNumberIdx);
                String consentStatus = getValueAtIndex(values, consentStatusIdx);

                // Validate required fields
                boolean hasError = false;

                if (sampleName == null || sampleName.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleName", "Sample Name is required"));
                    hasError = true;
                }

                if (lotBatchNumber == null || lotBatchNumber.isBlank()) {
                    errors.add(new ParseError(rowNumber, "lotBatchNumber", "Lot/Batch Number is required"));
                    hasError = true;
                }

                if (dateOfManufacture == null || dateOfManufacture.isBlank()) {
                    errors.add(new ParseError(rowNumber, "dateOfManufacture", "Date of Manufacture is required"));
                    hasError = true;
                }

                if (expiryRetestDate == null || expiryRetestDate.isBlank()) {
                    errors.add(new ParseError(rowNumber, "expiryRetestDate", "Expiry/Re-test Date is required"));
                    hasError = true;
                }

                if (storageCondition == null || storageCondition.isBlank()) {
                    errors.add(new ParseError(rowNumber, "storageCondition", "Storage Condition is required"));
                    hasError = true;
                }

                if (ownerRequester == null || ownerRequester.isBlank()) {
                    errors.add(new ParseError(rowNumber, "ownerRequester", "Owner/Requester is required"));
                    hasError = true;
                }

                if (hasError) {
                    continue;
                }

                rows.add(new PharmaManifestRow(rowNumber, sampleName.trim(), lotBatchNumber.trim(),
                        dateOfManufacture.trim(), expiryRetestDate.trim(), storageCondition.trim(),
                        ownerRequester.trim(), sampleType, alphanumericCode, chemicalIupacName, gradeSpecification,
                        chainOfCustodyDetails, patientId, clinicalTrialNumber, consentStatus));
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

        for (PharmaManifestRow row : manifest.rows()) {
            // Validate sample type against Pharmaceutical lab types and database
            if (row.sampleType() != null && !row.sampleType().isBlank()) {
                String sampleTypeLower = row.sampleType().toLowerCase().trim();

                // First check: Is it a valid pharmaceutical sample type?
                if (!VALID_PHARMA_SAMPLE_TYPES.contains(sampleTypeLower)) {
                    errors.add(new ParseError(row.rowNumber(), "sampleType",
                            "Invalid sample type for Pharmaceutical lab: '" + row.sampleType()
                                    + "'. Valid types include: Finished dosage form, API, Tablets, Capsules, Injections, etc."));
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

            // Validate date formats
            if (row.dateOfManufacture() != null && !row.dateOfManufacture().isBlank()) {
                if (!isValidDate(row.dateOfManufacture())) {
                    errors.add(new ParseError(row.rowNumber(), "dateOfManufacture",
                            "Invalid date format (expected YYYY-MM-DD): " + row.dateOfManufacture()));
                }
            }

            if (row.expiryRetestDate() != null && !row.expiryRetestDate().isBlank()) {
                if (!isValidDate(row.expiryRetestDate())) {
                    errors.add(new ParseError(row.rowNumber(), "expiryRetestDate",
                            "Invalid date format (expected YYYY-MM-DD): " + row.expiryRetestDate()));
                }
            }

            // Validate expiry date is after manufacture date
            if (row.dateOfManufacture() != null && row.expiryRetestDate() != null
                    && isValidDate(row.dateOfManufacture()) && isValidDate(row.expiryRetestDate())) {
                LocalDate mfgDate = LocalDate.parse(row.dateOfManufacture(), DATE_FORMATTER);
                LocalDate expDate = LocalDate.parse(row.expiryRetestDate(), DATE_FORMATTER);
                if (!expDate.isAfter(mfgDate)) {
                    errors.add(new ParseError(row.rowNumber(), "expiryRetestDate",
                            "Expiry/Re-test Date must be after Date of Manufacture"));
                }
            }

            // Validate consent status values
            if (row.consentStatus() != null && !row.consentStatus().isBlank()) {
                String consent = row.consentStatus().toLowerCase();
                if (!consent.equals("obtained") && !consent.equals("pending") && !consent.equals("waived")
                        && !consent.equals("not applicable")) {
                    errors.add(new ParseError(row.rowNumber(), "consentStatus",
                            "Invalid consent status. Expected: Obtained, Pending, Waived, or Not Applicable"));
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public PharmaManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new PharmaManifestImportResult(0, 0, errors, createdAccessionNumbers);
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

        // Get default sample type for pharma (Finished dosage form)
        TypeOfSample defaultSampleType = getDefaultPharmaSampleType();

        int totalRequested = manifest.rows().size();
        int totalCreated = 0;

        for (PharmaManifestRow row : manifest.rows()) {
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
            String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
            parentSample.setId(sampleIdDb);

            // Create sample item
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleTypeToUse);
            item.setExternalId(generateExternalId(row));
            item.setSortOrder("1");
            item.setStatusId(sampleEnteredStatusId);
            item.setSysUserId(sysUserId);

            String itemId = sampleItemService.insert(item);
            item.setId(itemId);
            totalCreated++;
            createdAccessionNumbers.add(parentSample.getAccessionNumber());

            notebookEntryService.addSample(entryId, item, sysUserId);

            // Store metadata on page 1 NotebookPageSample
            if (firstPageId != null) {
                NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(firstPageId,
                        Integer.parseInt(itemId));
                if (nps != null) {
                    Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                    // Auto-generated fields
                    data.put("uniqueSampleId", generateUniqueSampleId());
                    data.put("barcodeQrCode", generateBarcodeQrCode(parentSample.getAccessionNumber()));

                    // Required fields from CSV
                    data.put("sampleName", row.sampleName());
                    data.put("lotBatchNumber", row.lotBatchNumber());
                    data.put("dateOfManufacture", row.dateOfManufacture());
                    data.put("expiryRetestDate", row.expiryRetestDate());
                    data.put("storageCondition", row.storageCondition());
                    data.put("ownerRequester", row.ownerRequester());

                    // Sample type from CSV (or default)
                    data.put("sampleType", sampleTypeToUse.getDescription());

                    // Optional fields from CSV
                    if (row.alphanumericCode() != null && !row.alphanumericCode().isBlank()) {
                        data.put("alphanumericCode", row.alphanumericCode());
                    }
                    if (row.chemicalIupacName() != null && !row.chemicalIupacName().isBlank()) {
                        data.put("chemicalIupacName", row.chemicalIupacName());
                    }
                    if (row.gradeSpecification() != null && !row.gradeSpecification().isBlank()) {
                        data.put("gradeSpecification", row.gradeSpecification());
                    }
                    if (row.chainOfCustodyDetails() != null && !row.chainOfCustodyDetails().isBlank()) {
                        data.put("chainOfCustodyDetails", row.chainOfCustodyDetails());
                    }
                    if (row.patientId() != null && !row.patientId().isBlank()) {
                        data.put("patientId", row.patientId());
                    }
                    if (row.clinicalTrialNumber() != null && !row.clinicalTrialNumber().isBlank()) {
                        data.put("clinicalTrialNumber", row.clinicalTrialNumber());
                    }
                    if (row.consentStatus() != null && !row.consentStatus().isBlank()) {
                        data.put("consentStatus", row.consentStatus());
                    }

                    // Set sample category
                    data.put("sampleCategory", "Pharmaceutical");

                    nps.setData(data);
                    notebookPageSampleService.update(nps);
                }
            }
        }

        return new PharmaManifestImportResult(totalRequested, totalCreated, errors,
                createdAccessionNumbers.stream().distinct().toList());
    }

    /**
     * Generate a unique sample ID (system-generated).
     */
    private String generateUniqueSampleId() {
        return "PH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generate a barcode/QR code value based on accession number.
     */
    private String generateBarcodeQrCode(String accessionNumber) {
        return "QR-" + accessionNumber;
    }

    /**
     * Generate external ID from sample name and lot/batch number.
     */
    private String generateExternalId(PharmaManifestRow row) {
        String alphaCode = row.alphanumericCode();
        if (alphaCode != null && !alphaCode.isBlank()) {
            return alphaCode.trim();
        }
        // Fallback: combine lot number with abbreviated sample name
        String sampleAbbrev = row.sampleName().length() > 10 ? row.sampleName().substring(0, 10) : row.sampleName();
        return sampleAbbrev.replaceAll("\\s+", "-") + "-" + row.lotBatchNumber();
    }

    /**
     * Get default sample type for pharmaceutical samples.
     */
    private TypeOfSample getDefaultPharmaSampleType() {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription("Finished dosage form");
        TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        if (found == null) {
            // Fallback to API if finished dosage form not found
            searchType.setDescription("API");
            found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        }
        return found;
    }

    /**
     * Validate date format (YYYY-MM-DD).
     */
    private boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
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
    public List<Map<String, String>> getValidPharmaSampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_PHARMA_SAMPLE_TYPES) {
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
