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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.VirologyManifestImportForm;
import org.openelisglobal.notebook.service.VirologyManifestImportService.ParseError;
import org.openelisglobal.notebook.service.VirologyManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.VirologyManifestImportService.VirologyManifestImportResult;
import org.openelisglobal.notebook.service.VirologyManifestImportService.VirologyManifestRow;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sample.dao.SampleDAO;
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
 * Virology & Vaccine Unit manifest import: parses virology-specific CSV,
 * creates sample + sample items, links to notebook entry, and stores reception
 * metadata on page 1 NotebookPageSample data.
 *
 * Sample Types (per PDF spec): - Incoming: Clinical specimens, Viral isolates,
 * Cell culture samples, Vaccine candidates, Bacterial samples - Generated:
 * Cultured viruses, Vaccine products, Seed virus stocks, Bacterial cultures and
 * products
 *
 * Test Types: Viral Testing, Bacterial Testing
 */
@Service
public class VirologyManifestImportServiceImpl implements VirologyManifestImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Lock object for synchronizing accession number generation
    private static final Object ACCESSION_NUMBER_LOCK = new Object();

    // Accession number generator for generating unique accession numbers
    private IAccessionNumberGenerator accessionNumberGenerator;

    /**
     * Valid sample types for the Virology & Vaccine Unit laboratory. Based on PDF
     * specification: - Incoming: Clinical specimens, viral isolates, cell culture
     * samples, vaccine candidates, bacterial samples - Generated: Cultured viruses,
     * vaccine products, seed virus stocks, bacterial cultures and products
     */
    private static final java.util.Set<String> VALID_VIROLOGY_SAMPLE_TYPES = java.util.Set.of(
            // Incoming sample types - Clinical specimens
            "nasal swab", "throat swab", "nasopharyngeal swab", "oropharyngeal swab", "blood serum", "whole blood",
            "plasma", "cerebrospinal fluid", "bronchoalveolar lavage", "sputum", "stool", "urine", "saliva",
            "tissue biopsy", "skin lesion swab", "vesicle fluid", "amniotic fluid",
            // Incoming sample types - Viral isolates
            "viral isolate", "virus stock", "primary isolate",
            // Incoming sample types - Cell culture samples
            "cell culture sample", "cell monolayer", "cell suspension", "infected cell culture",
            "uninfected cell culture",
            // Incoming sample types - Vaccine candidates
            "vaccine candidate", "attenuated virus", "inactivated virus", "viral vector", "recombinant protein",
            "mrna vaccine",
            // Incoming sample types - Bacterial samples
            "bacterial isolate", "bacterial culture", "bacterial swab",
            // Generated materials - Cultured viruses
            "cultured virus", "passage virus", "plaque purified virus", "virus supernatant", "concentrated virus",
            // Generated materials - Vaccine products
            "vaccine bulk", "vaccine intermediate", "final vaccine product", "vaccine formulation",
            "adjuvanted vaccine",
            // Generated materials - Seed virus stocks
            "master seed virus", "working seed virus", "seed lot",
            // Generated materials - Bacterial cultures and products
            "bacterial product", "bacterial lysate", "bacterial antigen", "recombinant antigen");

    /**
     * Valid test types for the Virology & Vaccine Unit.
     */
    private static final java.util.Set<String> VALID_TEST_TYPES = java.util.Set.of("viral", "bacterial",
            "viral testing", "bacterial testing", "virus culture", "bacterial culture", "vaccine production",
            "quality control", "titer determination", "genome sequencing");

    /**
     * Valid source types for virology samples.
     */
    private static final java.util.Set<String> VALID_SOURCE_TYPES = java.util.Set.of("patient", "animal model",
            "environmental", "production batch", "clinical", "research", "qc sample", "reference material");

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
    private NoteBookPageService noteBookPageService;

    @Autowired
    private IStatusService statusService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, VirologyManifestImportForm columnMapping) {
        List<VirologyManifestRow> rows = new ArrayList<>();
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
                String headerKey = headers[i].trim().toLowerCase();
                columnIndex.put(headerKey, i);
            }

            // Required field indexes - Sample Arrival
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());
            Integer sourceIdx = getColumnIndex(columnIndex, columnMapping.getSourceColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer receptionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceptionDateTimeColumn());
            Integer testTypeIdx = getColumnIndex(columnIndex, columnMapping.getTestTypeColumn());
            Integer projectStudyAssociationIdx = getColumnIndex(columnIndex,
                    columnMapping.getProjectStudyAssociationColumn());

            // Virus/Vaccine Production field indexes
            Integer batchIdIdx = getColumnIndex(columnIndex, columnMapping.getBatchIdColumn());
            Integer productionStageIdx = getColumnIndex(columnIndex, columnMapping.getProductionStageColumn());
            Integer cellLineUsedIdx = getColumnIndex(columnIndex, columnMapping.getCellLineUsedColumn());
            Integer passageNumberIdx = getColumnIndex(columnIndex, columnMapping.getPassageNumberColumn());
            Integer titerValuesIdx = getColumnIndex(columnIndex, columnMapping.getTiterValuesColumn());
            Integer qualityControlResultsIdx = getColumnIndex(columnIndex,
                    columnMapping.getQualityControlResultsColumn());
            Integer formulationDetailsIdx = getColumnIndex(columnIndex, columnMapping.getFormulationDetailsColumn());

            // Optional field indexes
            Integer collectionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateTimeColumn());
            Integer storageConditionOnArrivalIdx = getColumnIndex(columnIndex,
                    columnMapping.getStorageConditionOnArrivalColumn());
            Integer transportTemperatureIdx = getColumnIndex(columnIndex,
                    columnMapping.getTransportTemperatureColumn());
            Integer receivingPersonnelNameIdx = getColumnIndex(columnIndex,
                    columnMapping.getReceivingPersonnelNameColumn());
            Integer manifestVerificationStatusIdx = getColumnIndex(columnIndex,
                    columnMapping.getManifestVerificationStatusColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Required fields - Sample Arrival
                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String source = getValueAtIndex(values, sourceIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String receptionDateTime = getValueAtIndex(values, receptionDateTimeIdx);
                String testType = getValueAtIndex(values, testTypeIdx);
                String projectStudyAssociation = getValueAtIndex(values, projectStudyAssociationIdx);

                // Virus/Vaccine Production fields
                String batchId = getValueAtIndex(values, batchIdIdx);
                String productionStage = getValueAtIndex(values, productionStageIdx);
                String cellLineUsed = getValueAtIndex(values, cellLineUsedIdx);
                String passageNumber = getValueAtIndex(values, passageNumberIdx);
                String titerValues = getValueAtIndex(values, titerValuesIdx);
                String qualityControlResults = getValueAtIndex(values, qualityControlResultsIdx);
                String formulationDetails = getValueAtIndex(values, formulationDetailsIdx);

                // Optional fields
                String collectionDateTime = getValueAtIndex(values, collectionDateTimeIdx);
                String storageConditionOnArrival = getValueAtIndex(values, storageConditionOnArrivalIdx);
                String transportTemperature = getValueAtIndex(values, transportTemperatureIdx);
                String receivingPersonnelName = getValueAtIndex(values, receivingPersonnelNameIdx);
                String manifestVerificationStatus = getValueAtIndex(values, manifestVerificationStatusIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                boolean hasError = false;

                if (sampleId == null || sampleId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleId", "Sample ID is required"));
                    hasError = true;
                }

                if (source == null || source.isBlank()) {
                    errors.add(new ParseError(rowNumber, "source",
                            "Source is required (patient, animal model, environmental, production batch)"));
                    hasError = true;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleType", "Sample Type is required"));
                    hasError = true;
                }

                if (receptionDateTime == null || receptionDateTime.isBlank()) {
                    errors.add(new ParseError(rowNumber, "receptionDateTime", "Reception Date & Time is required"));
                    hasError = true;
                }

                if (testType == null || testType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "testType", "Test Type is required (viral vs. bacterial)"));
                    hasError = true;
                }

                if (projectStudyAssociation == null || projectStudyAssociation.isBlank()) {
                    errors.add(new ParseError(rowNumber, "projectStudyAssociation",
                            "Project/Study Association is required"));
                    hasError = true;
                }

                if (hasError) {
                    continue;
                }

                rows.add(new VirologyManifestRow(rowNumber, sampleId.trim(), source.trim(), sampleType.trim(),
                        receptionDateTime.trim(), testType.trim(), projectStudyAssociation.trim(), batchId,
                        productionStage, cellLineUsed, passageNumber, titerValues, qualityControlResults,
                        formulationDetails, collectionDateTime, storageConditionOnArrival, transportTemperature,
                        receivingPersonnelName, manifestVerificationStatus, notes));
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

        for (VirologyManifestRow row : manifest.rows()) {
            // Validate sample type against Virology lab types and database
            if (row.sampleType() != null && !row.sampleType().isBlank()) {
                String sampleTypeLower = row.sampleType().toLowerCase().trim();

                // First check: Is it a valid virology sample type?
                if (!VALID_VIROLOGY_SAMPLE_TYPES.contains(sampleTypeLower)) {
                    errors.add(new ParseError(row.rowNumber(), "sampleType", "Invalid sample type for Virology lab: '"
                            + row.sampleType()
                            + "'. Valid types include: Nasal Swab, Throat Swab, Blood Serum, Viral Isolate, etc."));
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

            // Validate test type
            if (row.testType() != null && !row.testType().isBlank()) {
                String testTypeLower = row.testType().toLowerCase().trim();
                if (!VALID_TEST_TYPES.contains(testTypeLower)) {
                    errors.add(new ParseError(row.rowNumber(), "testType", "Invalid test type: '" + row.testType()
                            + "'. Expected: Viral, Bacterial, Virus Culture, Bacterial Culture, etc."));
                }
            }

            // Validate source type
            if (row.source() != null && !row.source().isBlank()) {
                String sourceLower = row.source().toLowerCase().trim();
                if (!VALID_SOURCE_TYPES.contains(sourceLower)) {
                    // Just warn, allow custom sources
                }
            }

            // Validate date/time formats
            if (row.receptionDateTime() != null && !row.receptionDateTime().isBlank()) {
                if (!row.receptionDateTime().equalsIgnoreCase("now") && !isValidDateTime(row.receptionDateTime())) {
                    errors.add(new ParseError(row.rowNumber(), "receptionDateTime",
                            "Invalid date/time format (expected YYYY-MM-DD HH:MM, YYYY-MM-DD, or 'now'): "
                                    + row.receptionDateTime()));
                }
            }

            if (row.collectionDateTime() != null && !row.collectionDateTime().isBlank()) {
                if (!isValidDateTime(row.collectionDateTime())) {
                    errors.add(new ParseError(row.rowNumber(), "collectionDateTime",
                            "Invalid date/time format (expected YYYY-MM-DD HH:MM or YYYY-MM-DD): "
                                    + row.collectionDateTime()));
                }
            }

            // Validate reception is after collection (if both provided)
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

            // Validate passage number (should be numeric if provided)
            if (row.passageNumber() != null && !row.passageNumber().isBlank()) {
                try {
                    Integer.parseInt(row.passageNumber().trim());
                } catch (NumberFormatException e) {
                    // Allow non-numeric (e.g., "P3", "Passage 5")
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public VirologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new VirologyManifestImportResult(0, 0, errors, createdAccessionNumbers);
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

        // Get default sample type for virology (Nasal Swab)
        TypeOfSample defaultSampleType = getDefaultVirologySampleType();

        int totalRequested = manifest.rows().size();
        int totalCreated = 0;

        for (VirologyManifestRow row : manifest.rows()) {
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

            // Create parent sample with synchronized accession number generation
            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

            // Synchronize accession number generation to prevent duplicates
            String sampleIdDb;
            synchronized (ACCESSION_NUMBER_LOCK) {
                String generatedAccessionNumber = null;
                int maxAttempts = 100;
                int attempts = 0;

                while (generatedAccessionNumber == null && attempts < maxAttempts) {
                    String candidateNumber = getNextAccessionNumberInternal();

                    // Check if this accession number already exists in the database
                    Sample existingSample = sampleService.getSampleByAccessionNumber(candidateNumber);
                    if (existingSample == null) {
                        generatedAccessionNumber = candidateNumber;
                    } else {
                        attempts++;
                        LogEvent.logWarn(this.getClass().getSimpleName(), "createSamplesForEntry",
                                "Accession number " + candidateNumber + " already exists. Retrying (attempt "
                                        + attempts + ")");
                    }
                }

                if (generatedAccessionNumber == null) {
                    errors.add(new ParseError(row.rowNumber(), "sample",
                            "Failed to generate unique accession number after " + maxAttempts + " attempts"));
                    continue;
                }

                parentSample.setAccessionNumber(generatedAccessionNumber);
                sampleService.insertDataWithAccessionNumber(parentSample);
                sampleIdDb = parentSample.getId();

                // Flush to ensure the database sees this sample before the next iteration
                entityManager.flush();
            }
            parentSample.setId(sampleIdDb);

            // Create sample item
            SampleItem item = new SampleItem();
            item.setSample(parentSample);
            item.setTypeOfSample(sampleTypeToUse);
            item.setExternalId(row.sampleId());
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

                // If NotebookPageSample record doesn't exist, create it
                if (nps == null) {
                    nps = new NotebookPageSample();
                    nps.setNotebookPage(noteBookPageService.get(firstPageId));
                    nps.setSampleItemId(itemId);
                    nps.setStatus(NotebookPageSample.Status.PENDING);
                    nps.setData(new HashMap<>());
                    notebookPageSampleService.insert(nps);
                }

                if (nps != null) {
                    Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

                    // Auto-generated fields
                    data.put("barcodeQrCode", generateBarcodeQrCode(parentSample.getAccessionNumber()));

                    // Required fields from CSV - Sample Arrival
                    data.put("sampleId", row.sampleId());
                    data.put("source", row.source());
                    data.put("sampleType", sampleTypeToUse.getDescription());
                    data.put("testType", row.testType());
                    data.put("projectStudyAssociation", row.projectStudyAssociation());

                    // Handle 'now' for reception date/time
                    if (row.receptionDateTime() != null && row.receptionDateTime().equalsIgnoreCase("now")) {
                        data.put("receptionDateTime",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    } else {
                        data.put("receptionDateTime", row.receptionDateTime());
                    }

                    // Virus/Vaccine Production fields
                    if (row.batchId() != null && !row.batchId().isBlank()) {
                        data.put("batchId", row.batchId());
                    }
                    if (row.productionStage() != null && !row.productionStage().isBlank()) {
                        data.put("productionStage", row.productionStage());
                    }
                    if (row.cellLineUsed() != null && !row.cellLineUsed().isBlank()) {
                        data.put("cellLineUsed", row.cellLineUsed());
                    }
                    if (row.passageNumber() != null && !row.passageNumber().isBlank()) {
                        data.put("passageNumber", row.passageNumber());
                    }
                    if (row.titerValues() != null && !row.titerValues().isBlank()) {
                        data.put("titerValues", row.titerValues());
                    }
                    if (row.qualityControlResults() != null && !row.qualityControlResults().isBlank()) {
                        data.put("qualityControlResults", row.qualityControlResults());
                    }
                    if (row.formulationDetails() != null && !row.formulationDetails().isBlank()) {
                        data.put("formulationDetails", row.formulationDetails());
                    }

                    // Optional fields from CSV
                    if (row.collectionDateTime() != null && !row.collectionDateTime().isBlank()) {
                        data.put("collectionDateTime", row.collectionDateTime());
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
                    if (row.notes() != null && !row.notes().isBlank()) {
                        data.put("notes", row.notes());
                    }

                    // Set sample category
                    data.put("sampleCategory", "Virology");

                    nps.setData(data);
                    notebookPageSampleService.update(nps);
                }
            }
        }

        return new VirologyManifestImportResult(totalRequested, totalCreated, errors,
                createdAccessionNumbers.stream().distinct().toList());
    }

    /**
     * Generate a barcode/QR code value based on accession number.
     */
    private String generateBarcodeQrCode(String accessionNumber) {
        return "QR-" + accessionNumber;
    }

    /**
     * Get default sample type for virology samples.
     */
    private TypeOfSample getDefaultVirologySampleType() {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription("Nasal Swab");
        TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        if (found == null) {
            // Fallback to Throat Swab if nasal swab not found
            searchType.setDescription("Throat Swab");
            found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        }
        if (found == null) {
            // Fallback to Blood Serum
            searchType.setDescription("Blood Serum");
            found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        }
        if (found == null) {
            // Fallback to Viral Isolate
            searchType.setDescription("Viral Isolate");
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
    public List<Map<String, String>> getValidVirologySampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();

        for (String validType : VALID_VIROLOGY_SAMPLE_TYPES) {
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
     * Get the next accession number using the accession number generator. This
     * method should be called from within a synchronized block.
     */
    private String getNextAccessionNumberInternal() {
        if (accessionNumberGenerator == null) {
            accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        }
        if (accessionNumberGenerator != null) {
            return accessionNumberGenerator.getNextAccessionNumber(null, true);
        }
        // Fallback to old method if generator not available
        return sampleDAO.getNextAccessionNumber();
    }
}
