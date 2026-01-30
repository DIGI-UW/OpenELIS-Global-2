package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.form.GBDManifestImportForm;
import org.openelisglobal.notebook.service.GBDManifestImportService.GBDManifestImportResult;
import org.openelisglobal.notebook.service.GBDManifestImportService.GBDManifestRow;
import org.openelisglobal.notebook.service.GBDManifestImportService.ParseError;
import org.openelisglobal.notebook.service.GBDManifestImportService.ParsedManifest;
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
 * Genomic Bioanalytical Database (GBD) manifest import implementation.
 *
 * Parses GBD-specific CSV, creates samples + sample items, links to notebook
 * entry, and stores reception and processing metadata on page 1
 * NotebookPageSample in JSONB format for flexible genomic data storage.
 *
 * Reception Metadata: - Required: sampleId, sampleType, source, collectionDate,
 * receptionDateTime - Optional: projectStudyAssociation, volumeConcentration,
 * A260/280, A260/230, RIN
 *
 * Processing Metadata: - Optional: extractionMethodKit, pcrProtocol,
 * libraryPrepProtocol, sequencingPlatform, runId, operator, processingDateTime,
 * notes - Auto-generated: systemAssignedSampleId, barcodeQrCode
 *
 * Sample Types validated against GBD-specific list (DNA, RNA, cDNA, genomic
 * materials).
 */
@Service
public class GBDManifestImportServiceImpl implements GBDManifestImportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Valid sample types for the Genomic Bioanalytical Database. Must match
     * descriptions in type_of_sample table from Liquibase scripts.
     */
    private static final Set<String> VALID_GBD_SAMPLE_TYPES = Set.of(
            // DNA samples
            "dna", "dna - genomic", "dna - plasmid", "dna - cdna", "dna - pcr product", "pcr product",

            // RNA samples
            "rna", "rna - total", "rna - mrna", "rna - ncrna", "rna - mirna", "rna - rrna",

            // cDNA samples
            "cdna", "cdna - first strand", "cdna - double strand", "cdna - library",

            // Genomic materials
            "whole blood", "saliva", "buccal swab", "tissue", "hair follicle", "nail", "cultured cells",
            "bacterial culture", "viral lysate",

            // Quality control samples
            "positive control", "negative control", "reference standard", "quality control sample",

            // Library preparation intermediates
            "fragmented dna", "adapter-ligated dna", "pcr-amplified library", "size-selected library",

            // Sequencing samples
            "sequencing library", "sequencing-ready sample", "pooled sample",

            // Other genomic materials
            "germ line dna", "somatic dna", "cfna - circulating nucleic acids", "environmental sample",
            "other genomic material");

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Autowired
    private IStatusService statusService;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, GBDManifestImportForm columnMapping) {
        List<GBDManifestRow> rows = new ArrayList<>();
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

            // Required field indices
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer sourceIdx = getColumnIndex(columnIndex, columnMapping.getSourceColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer receptionDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceptionDateTimeColumn());

            // Optional reception metadata indices
            Integer projectStudyAssociationIdx = getColumnIndex(columnIndex,
                    columnMapping.getProjectStudyAssociationColumn());
            Integer volumeConcentrationIdx = getColumnIndex(columnIndex, columnMapping.getVolumeConcentrationColumn());
            Integer a260_280Idx = getColumnIndex(columnIndex, columnMapping.getA260_280Column());
            Integer a260_230Idx = getColumnIndex(columnIndex, columnMapping.getA260_230Column());
            Integer rinIdx = getColumnIndex(columnIndex, columnMapping.getRinColumn());

            // Optional processing metadata indices
            Integer extractionMethodKitIdx = getColumnIndex(columnIndex, columnMapping.getExtractionMethodKitColumn());
            Integer pcrProtocolIdx = getColumnIndex(columnIndex, columnMapping.getPcrProtocolColumn());
            Integer libraryPrepProtocolIdx = getColumnIndex(columnIndex, columnMapping.getLibraryPrepProtocolColumn());
            Integer sequencingPlatformIdx = getColumnIndex(columnIndex, columnMapping.getSequencingPlatformColumn());
            Integer runIdIdx = getColumnIndex(columnIndex, columnMapping.getRunIdColumn());
            Integer operatorIdx = getColumnIndex(columnIndex, columnMapping.getOperatorColumn());
            Integer processingDateTimeIdx = getColumnIndex(columnIndex, columnMapping.getProcessingDateTimeColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] fields = parseCSVLine(line);

                String sampleId = getFieldValue(fields, sampleIdIdx);
                String sampleType = getFieldValue(fields, sampleTypeIdx);
                String source = getFieldValue(fields, sourceIdx);
                String collectionDate = getFieldValue(fields, collectionDateIdx);
                String receptionDateTime = getFieldValue(fields, receptionDateTimeIdx);
                String projectStudyAssociation = getFieldValue(fields, projectStudyAssociationIdx);
                String volumeConcentration = getFieldValue(fields, volumeConcentrationIdx);
                String a260_280 = getFieldValue(fields, a260_280Idx);
                String a260_230 = getFieldValue(fields, a260_230Idx);
                String rin = getFieldValue(fields, rinIdx);
                String extractionMethodKit = getFieldValue(fields, extractionMethodKitIdx);
                String pcrProtocol = getFieldValue(fields, pcrProtocolIdx);
                String libraryPrepProtocol = getFieldValue(fields, libraryPrepProtocolIdx);
                String sequencingPlatform = getFieldValue(fields, sequencingPlatformIdx);
                String runId = getFieldValue(fields, runIdIdx);
                String operator = getFieldValue(fields, operatorIdx);
                String processingDateTime = getFieldValue(fields, processingDateTimeIdx);
                String notes = getFieldValue(fields, notesIdx);

                rows.add(new GBDManifestRow(rowNumber, sampleId, sampleType, source, collectionDate, receptionDateTime,
                        projectStudyAssociation, volumeConcentration, a260_280, a260_230, rin, extractionMethodKit,
                        pcrProtocol, libraryPrepProtocol, sequencingPlatform, runId, operator, processingDateTime,
                        notes));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    public List<ParseError> validateManifest(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (GBDManifestRow row : manifest.rows()) {
            // Validate required fields
            if (row.sampleId() == null || row.sampleId().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "sampleId", "Sample ID is required"));
            }

            if (row.sampleType() == null || row.sampleType().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Sample Type is required"));
            } else if (!VALID_GBD_SAMPLE_TYPES.contains(row.sampleType().toLowerCase())) {
                errors.add(new ParseError(row.rowNumber(), "sampleType",
                        "Sample Type '" + row.sampleType() + "' is not valid for GBD"));
            }

            if (row.source() == null || row.source().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "source", "Source is required"));
            }

            if (row.collectionDate() == null || row.collectionDate().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "collectionDate", "Collection Date is required"));
            } else {
                try {
                    LocalDate.parse(row.collectionDate(), DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    errors.add(new ParseError(row.rowNumber(), "collectionDate",
                            "Collection Date format must be yyyy-MM-dd"));
                }
            }

            if (row.receptionDateTime() == null || row.receptionDateTime().isBlank()) {
                errors.add(new ParseError(row.rowNumber(), "receptionDateTime", "Reception Date/Time is required"));
            } else {
                try {
                    LocalDateTime.parse(row.receptionDateTime(), DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    errors.add(new ParseError(row.rowNumber(), "receptionDateTime",
                            "Reception Date/Time format must be yyyy-MM-dd HH:mm"));
                }
            }

            // Validate optional numeric fields
            if (row.a260_280() != null && !row.a260_280().isBlank()) {
                try {
                    Double.parseDouble(row.a260_280());
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(row.rowNumber(), "a260_280",
                            "A260/280 ratio must be a valid decimal number"));
                }
            }

            if (row.a260_230() != null && !row.a260_230().isBlank()) {
                try {
                    Double.parseDouble(row.a260_230());
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(row.rowNumber(), "a260_230",
                            "A260/230 ratio must be a valid decimal number"));
                }
            }

            if (row.rin() != null && !row.rin().isBlank()) {
                try {
                    Double.parseDouble(row.rin());
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(row.rowNumber(), "rin", "RIN must be a valid decimal number"));
                }
            }

            if (row.volumeConcentration() != null && !row.volumeConcentration().isBlank()) {
                // Extract numeric part from values that may have units (e.g., "100 ng/µL", "5
                // mL")
                String volumeValue = row.volumeConcentration().trim();
                String numericPart = volumeValue.replaceAll("[^0-9.]", "");
                if (numericPart.isEmpty()) {
                    errors.add(new ParseError(row.rowNumber(), "volumeConcentration",
                            "Volume/Concentration must contain a numeric value"));
                } else {
                    try {
                        Double.parseDouble(numericPart);
                    } catch (NumberFormatException e) {
                        errors.add(new ParseError(row.rowNumber(), "volumeConcentration",
                                "Volume/Concentration must be a valid number (units optional)"));
                    }
                }
            }

            // Validate optional datetime field
            if (row.processingDateTime() != null && !row.processingDateTime().isBlank()) {
                try {
                    LocalDateTime.parse(row.processingDateTime(), DATE_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    errors.add(new ParseError(row.rowNumber(), "processingDateTime",
                            "Processing Date/Time format must be yyyy-MM-dd HH:mm"));
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public GBDManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdSampleIds = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new GBDManifestImportResult(0, 0, errors, createdSampleIds);
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

        for (GBDManifestRow row : manifest.rows()) {
            try {
                Sample parentSample = new Sample();
                parentSample.setSysUserId(sysUserId);
                parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
                parentSample.setReceivedTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
                String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
                parentSample.setId(sampleIdDb);

                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setExternalId(row.sampleId());
                item.setSortOrder("1");
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                totalCreated++;
                createdSampleIds.add(parentSample.getAccessionNumber());

                notebookEntryService.addSample(entryId, item, sysUserId);

                if (firstPageId != null) {
                    NotebookPageSample nps = notebookPageSampleDAO.getByPageIdAndSampleItemId(firstPageId,
                            Integer.parseInt(itemId));
                    if (nps != null) {
                        Map<String, Object> data = getData(row, nps, parentSample);
                        nps.setData(data);
                        notebookPageSampleDAO.update(nps);
                    }
                }

            } catch (Exception e) {
                errors.add(new ParseError(row.rowNumber(), "sample", "Error creating sample: " + e.getMessage()));
            }
        }

        return new GBDManifestImportResult(totalRequested, totalCreated, errors,
                createdSampleIds.stream().distinct().toList());
    }

    private Map<String, Object> getData(GBDManifestRow row, NotebookPageSample nps, Sample parentSample) {
        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Add barcode and required fields
        data.put("barcodeQrCode", generateBarcodeQrCode(parentSample.getAccessionNumber()));
        data.put("sampleId", row.sampleId());
        data.put("sampleType", row.sampleType());
        data.put("source", row.source());
        data.put("collectionDate", row.collectionDate());
        data.put("receptionDateTime", row.receptionDateTime());

        // Add optional reception metadata
        if (row.projectStudyAssociation() != null && !row.projectStudyAssociation().isBlank()) {
            data.put("projectStudyAssociation", row.projectStudyAssociation());
        }
        if (row.volumeConcentration() != null && !row.volumeConcentration().isBlank()) {
            data.put("volumeConcentration", row.volumeConcentration());
        }
        if (row.a260_280() != null && !row.a260_280().isBlank()) {
            data.put("a260_280", row.a260_280());
        }
        if (row.a260_230() != null && !row.a260_230().isBlank()) {
            data.put("a260_230", row.a260_230());
        }
        if (row.rin() != null && !row.rin().isBlank()) {
            data.put("rin", row.rin());
        }

        // Add optional processing metadata
        if (row.extractionMethodKit() != null && !row.extractionMethodKit().isBlank()) {
            data.put("extractionMethodKit", row.extractionMethodKit());
        }
        if (row.pcrProtocol() != null && !row.pcrProtocol().isBlank()) {
            data.put("pcrProtocol", row.pcrProtocol());
        }
        if (row.libraryPrepProtocol() != null && !row.libraryPrepProtocol().isBlank()) {
            data.put("libraryPrepProtocol", row.libraryPrepProtocol());
        }
        if (row.sequencingPlatform() != null && !row.sequencingPlatform().isBlank()) {
            data.put("sequencingPlatform", row.sequencingPlatform());
        }
        if (row.runId() != null && !row.runId().isBlank()) {
            data.put("runId", row.runId());
        }
        if (row.operator() != null && !row.operator().isBlank()) {
            data.put("operator", row.operator());
        }
        if (row.processingDateTime() != null && !row.processingDateTime().isBlank()) {
            data.put("processingDateTime", row.processingDateTime());
        }
        if (row.notes() != null && !row.notes().isBlank()) {
            data.put("notes", row.notes());
        }

        data.put("sampleCategory", "GBD");
        return data;
    }

    /**
     * Generate a barcode/QR code value based on accession number.
     */
    private String generateBarcodeQrCode(String accessionNumber) {
        return "QR-" + accessionNumber;
    }

    @Override
    public List<Map<String, String>> getValidGBDSampleTypes() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String sampleType : VALID_GBD_SAMPLE_TYPES) {
            Map<String, String> map = new HashMap<>();
            map.put("id", sampleType);
            map.put("description", sampleType);
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
