package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hibernate.Hibernate;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.BioanalyticalManifestImportForm;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.BioanalyticalManifestImportResult;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.BioanalyticalManifestRow;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.ParseError;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bioanalytical & Bioequivalence Laboratory manifest import implementation.
 *
 * Parses bioanalytical-specific CSV, creates samples + sample items, links to
 * notebook entry, and stores reception metadata on page 1 NotebookPageSample
 * data.
 *
 * Reception Metadata: - Required: uniqueSampleId, sampleType, sourceOrigin,
 * requestedTests, dateTimeOfReceipt, receivingPersonnel - Optional:
 * projectStudyAssociation, storageConditionPrior, sampleVolume,
 * transportTemperature, manifestVerificationStatus, notes - Auto-generated:
 * systemAssignedSampleId, barcodeQrCode
 *
 * Sample Types validated against bioanalytical-specific list (biological
 * matrices and pharmaceutical products).
 *
 * Tests validated against analytical test list (HPLC, LC-MS/MS, dissolution,
 * assay, hardness, friability, disintegration, identity test, etc.).
 */
@Service
public class BioanalyticalManifestImportServiceImpl implements BioanalyticalManifestImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Valid sample types for the Bioanalytical & Bioequivalence Laboratory. Must
     * match descriptions in type_of_sample table from Liquibase scripts.
     */
    private static final Set<String> VALID_BIOANALYTICAL_SAMPLE_TYPES = Set.of(
            // Biological matrices - from Medical Laboratory
            "plasma", "plasma - edta", "plasma - heparin", "plasma - serum separator", "serum", "urine",
            "cerebrospinal fluid", "saliva", "hair", "nail", "whole blood", "other biological matrix",

            // Pharmaceutical products - API forms
            "api", "api - powder", "api - solution",

            // Pharmaceutical products - solid dosage
            "tablet", "capsule",

            // Pharmaceutical products - other forms
            "suspension", "powder", "liquid", "cream", "gel", "patch",

            // Standards and excipients
            "reference standard", "excipient", "degradation product",

            // Stability study samples
            "stability sample - initial", "stability sample - intermediate", "stability sample - final",
            "accelerated sample");

    /**
     * Valid analytical tests for the Bioanalytical & Bioequivalence Laboratory.
     * Bioanalytical and pharmaceutical quality tests.
     */
    private static final Set<String> VALID_BIOANALYTICAL_TESTS = Set.of(
            // Bioanalytical tests
            "lc-ms/ms", "lc-ms/ms, bioequivalence", "drug concentration (hplc)", "drug concentration (lc-ms/ms)",
            "pharmacokinetic analysis", "biomarker quantification", "metabolite identification", "bioequivalence",

            // HPLC methods
            "hplc", "hplc, assay", "hplc uv-vis", "hplc-dad", "hplc-fl",

            // Pharmaceutical quality tests - chemical
            "assay", "assay, dissolution", "assay, dissolution, content uniformity", "assay (hplc)",
            "assay (titration)", "identity test", "identity test (uv)", "identity test (ftir)", "purity test",
            "related substances", "moisture content",

            // Pharmaceutical quality tests - physical
            "dissolution", "dissolution (usp apparatus i)", "dissolution (usp apparatus ii)", "disintegration",
            "hardness", "friability", "content uniformity");

    /**
     * Valid source origins for bioanalytical samples.
     */
    private static final Set<String> VALID_SOURCE_ORIGINS = Set.of(
            // Medical laboratories
            "medical laboratory", "medical lab", "medical lab (ctd-be)", "medical laboratory - clinical site a",
            "medical laboratory - clinical site b", "medical laboratory - clinical site c",

            // External clients and partners
            "external client", "external client - pharma research inc", "external client - cro partner",
            "pharmaceutical company", "contract research organization", "cro",

            // Research facilities
            "internal researcher", "researcher", "researcher - university lab", "researcher - formulation lab",
            "university lab", "formulation lab");

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private IStatusService statusService;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, BioanalyticalManifestImportForm columnMapping) {
        List<BioanalyticalManifestRow> rows = new ArrayList<>();
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

            Integer uniqueSampleIdIdx = getColumnIndex(columnIndex, columnMapping.getUniqueSampleIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer sourceOriginIdx = getColumnIndex(columnIndex, columnMapping.getSourceOriginColumn());
            Integer requestedTestsIdx = getColumnIndex(columnIndex, columnMapping.getRequestedTestsColumn());
            Integer dateTimeOfReceiptIdx = getColumnIndex(columnIndex, columnMapping.getDateTimeOfReceiptColumn());
            Integer receivingPersonnelIdx = getColumnIndex(columnIndex, columnMapping.getReceivingPersonnelColumn());

            Integer projectStudyAssociationIdx = getColumnIndex(columnIndex,
                    columnMapping.getProjectStudyAssociationColumn());
            Integer storageConditionPriorIdx = getColumnIndex(columnIndex,
                    columnMapping.getStorageConditionPriorColumn());
            Integer sampleVolumeIdx = getColumnIndex(columnIndex, columnMapping.getSampleVolumeColumn());
            Integer transportTemperatureIdx = getColumnIndex(columnIndex,
                    columnMapping.getTransportTemperatureColumn());
            Integer manifestVerificationStatusIdx = getColumnIndex(columnIndex,
                    columnMapping.getManifestVerificationStatusColumn());
            Integer subjectIdIdx = getColumnIndex(columnIndex, columnMapping.getSubjectIdColumn());
            Integer timepointIdx = getColumnIndex(columnIndex, columnMapping.getTimepointColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] fields = parseCSVLine(line);

                String uniqueSampleId = getFieldValue(fields, uniqueSampleIdIdx);
                String sampleType = getFieldValue(fields, sampleTypeIdx);
                String sourceOrigin = getFieldValue(fields, sourceOriginIdx);
                String requestedTests = getFieldValue(fields, requestedTestsIdx);
                String dateTimeOfReceipt = getFieldValue(fields, dateTimeOfReceiptIdx);
                String receivingPersonnel = getFieldValue(fields, receivingPersonnelIdx);
                String projectStudyAssociation = getFieldValue(fields, projectStudyAssociationIdx);
                String storageConditionPrior = getFieldValue(fields, storageConditionPriorIdx);
                String sampleVolume = getFieldValue(fields, sampleVolumeIdx);
                String transportTemperature = getFieldValue(fields, transportTemperatureIdx);
                String manifestVerificationStatus = getFieldValue(fields, manifestVerificationStatusIdx);
                String subjectId = getFieldValue(fields, subjectIdIdx);
                String timepoint = getFieldValue(fields, timepointIdx);
                String notes = getFieldValue(fields, notesIdx);

                rows.add(new BioanalyticalManifestRow(rowNumber, uniqueSampleId, sampleType, sourceOrigin,
                        requestedTests, dateTimeOfReceipt, receivingPersonnel, projectStudyAssociation,
                        storageConditionPrior, sampleVolume, transportTemperature, manifestVerificationStatus,
                        subjectId, timepoint, notes));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    public List<ParseError> validateManifest(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (BioanalyticalManifestRow row : manifest.rows()) {
            if (row.uniqueSampleId() == null || row.uniqueSampleId().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "uniqueSampleId", "Sample ID is required"));
            }

            if (row.sampleType() == null || row.sampleType().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Sample Type is required"));
            } else if (!VALID_BIOANALYTICAL_SAMPLE_TYPES.contains(row.sampleType().toLowerCase())) {
                errors.add(new ParseError(row.rowNumber(), "sampleType",
                        "Sample Type '" + row.sampleType() + "' is not valid for bioanalytical lab"));
            }

            if (row.sourceOrigin() == null || row.sourceOrigin().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "sourceOrigin", "Source Origin is required"));
            } else if (!VALID_SOURCE_ORIGINS.contains(row.sourceOrigin().toLowerCase())) {
                errors.add(new ParseError(row.rowNumber(), "sourceOrigin",
                        "Source Origin '" + row.sourceOrigin() + "' is not valid"));
            }

            if (row.requestedTests() == null || row.requestedTests().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "requestedTests", "Requested Tests are required"));
            } else {
                // Validate each test
                String[] tests = row.requestedTests().split(";");
                for (String test : tests) {
                    String trimmedTest = test.trim().toLowerCase();
                    if (!VALID_BIOANALYTICAL_TESTS.contains(trimmedTest)) {
                        errors.add(new ParseError(row.rowNumber(), "requestedTests",
                                "Test '" + test.trim() + "' is not valid for bioanalytical lab"));
                    }
                }
            }

            if (row.dateTimeOfReceipt() == null || row.dateTimeOfReceipt().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "dateTimeOfReceipt", "Receipt Date/Time is required"));
            } else {
                try {
                    LocalDateTime.parse(row.dateTimeOfReceipt(), DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    errors.add(new ParseError(row.rowNumber(), "dateTimeOfReceipt",
                            "Receipt Date/Time format must be yyyy-MM-dd HH:mm"));
                }
            }

            if (row.receivingPersonnel() == null || row.receivingPersonnel().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "receivingPersonnel", "Receiving Personnel is required"));
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public BioanalyticalManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdSampleIds = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new BioanalyticalManifestImportResult(0, 0, errors, createdSampleIds);
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

        int totalRequested = manifest.rows().size();
        int totalCreated = 0;

        for (BioanalyticalManifestRow row : manifest.rows()) {
            try {
                Sample parentSample = new Sample();
                parentSample.setSysUserId(sysUserId);
                parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
                parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
                String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
                parentSample.setId(sampleIdDb);

                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setExternalId(row.uniqueSampleId());
                item.setSortOrder("1");
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                totalCreated++;
                createdSampleIds.add(parentSample.getAccessionNumber());

                notebookEntryService.addSample(entryId, item, sysUserId);

                if (firstPageId != null) {
                    NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(firstPageId,
                            Integer.parseInt(itemId));
                    if (nps != null) {
                        Map<String, Object> data = getData(row, nps, parentSample);
                        nps.setData(data);
                        notebookPageSampleService.update(nps);
                    }
                }

            } catch (Exception e) {
                errors.add(new ParseError(row.rowNumber(), "sample", "Error creating sample: " + e.getMessage()));
            }
        }

        return new BioanalyticalManifestImportResult(totalRequested, totalCreated, errors,
                createdSampleIds.stream().distinct().toList());
    }

    private Map<String, Object> getData(BioanalyticalManifestRow row, NotebookPageSample nps, Sample parentSample) {
        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();
        data.put("barcodeQrCode", generateBarcodeQrCode(parentSample.getAccessionNumber()));
        data.put("uniqueSampleId", row.uniqueSampleId());
        data.put("sampleType", row.sampleType());
        data.put("sourceOrigin", row.sourceOrigin());
        data.put("requestedTests", row.requestedTests());
        data.put("dateTimeOfReceipt", row.dateTimeOfReceipt());
        data.put("receivingPersonnel", row.receivingPersonnel());

        if (row.projectStudyAssociation() != null && !row.projectStudyAssociation().isBlank()) {
            data.put("projectStudyAssociation", row.projectStudyAssociation());
        }
        if (row.storageConditionPrior() != null && !row.storageConditionPrior().isBlank()) {
            data.put("storageConditionPrior", row.storageConditionPrior());
        }
        if (row.sampleVolume() != null && !row.sampleVolume().isBlank()) {
            data.put("sampleVolume", row.sampleVolume());
        }
        if (row.transportTemperature() != null && !row.transportTemperature().isBlank()) {
            data.put("transportTemperature", row.transportTemperature());
        }
        if (row.manifestVerificationStatus() != null && !row.manifestVerificationStatus().isBlank()) {
            data.put("manifestVerificationStatus", row.manifestVerificationStatus());
        }
        if (row.notes() != null && !row.notes().isBlank()) {
            data.put("notes", row.notes());
        }
        if (row.subjectId() != null && !row.subjectId().isBlank()) {
            data.put("subjectId", row.subjectId());
        }
        if (row.timepoint() != null && !row.timepoint().isBlank()) {
            data.put("timepoint", row.timepoint());
        }

        data.put("sampleCategory", "Bioanalytical");
        return data;
    }

    /**
     * Generate a barcode/QR code value based on unique sample ID.
     */
    private String generateBarcodeQrCode(String uniqueSampleId) {
        return "QR-" + uniqueSampleId;
    }

    @Override
    public List<Map<String, String>> getValidBioanalyticalSampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String sampleType : VALID_BIOANALYTICAL_SAMPLE_TYPES) {
            Map<String, String> map = new HashMap<>();
            map.put("id", sampleType);
            map.put("description", sampleType);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, String>> getValidBioanalyticalTests() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String test : VALID_BIOANALYTICAL_TESTS) {
            Map<String, String> map = new HashMap<>();
            map.put("id", test);
            map.put("description", test);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, String>> getValidSourceOrigins() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String origin : VALID_SOURCE_ORIGINS) {
            Map<String, String> map = new HashMap<>();
            map.put("id", origin);
            map.put("description", origin);
            result.add(map);
        }
        return result;
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    private Integer getColumnIndex(Map<String, Integer> columnIndex, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        return columnIndex.get(columnName.toLowerCase());
    }

    private String getFieldValue(String[] fields, Integer index) {
        if (index == null || index >= fields.length) {
            return null;
        }
        String value = fields[index].trim();
        return value.isBlank() ? null : value;
    }
}
