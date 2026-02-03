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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.dao.NotebookPageSampleDAO;
import org.openelisglobal.notebook.form.VirologyLabManifestImportForm;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.VirologyLabManifestImportService.VirologyLabManifestImportResult;
import org.openelisglobal.notebook.service.VirologyLabManifestImportService.ManifestRow;
import org.openelisglobal.notebook.service.VirologyLabManifestImportService.ParseError;
import org.openelisglobal.notebook.service.VirologyLabManifestImportService.ParsedManifest;
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
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.util.AccessionNumberHandler;

/**
 * VirologyLab (Virology Laboratory) manifest import implementation.
 *
 * Parses VirologyLab-specific CSV, creates samples + sample items, links to notebook
 * entry, and stores reception and processing metadata on page 1
 * NotebookPageSample in JSONB format for flexible virology data storage.
 */
@Service
public class VirologyLabManifestImportServiceImpl implements VirologyLabManifestImportService {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NoteBookService notebookService;

    @Autowired
    private NotebookPageSampleDAO notebookPageSampleDAO;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleDAO sampleDAO;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> VALID_VIROLOGY_SAMPLE_TYPES = Set.of(
        "DNA", "RNA", "cDNA", "Tissue", "Swab", "Blood", "Serum", "Plasma",
        "Viral Isolate", "Cell Culture", "PCR Product", "Library", "Other"
    );

    @Override
    public List<Map<String, String>> getValidVirologyLabSampleTypes() {
        List<Map<String, String>> sampleTypes = new ArrayList<>();
        for (String type : VALID_VIROLOGY_SAMPLE_TYPES) {
            Map<String, String> typeMap = new HashMap<>();
            typeMap.put("id", type.toLowerCase().replace(" ", "_"));
            typeMap.put("description", type);
            sampleTypes.add(typeMap);
        }
        return sampleTypes;
    }

    @Override
    public ParsedManifest parseManifestCsv(InputStream inputStream, VirologyLabManifestImportForm form) {
        List<ManifestRow> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            int rowNumber = 0;
            boolean isHeaderRow = true;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (isHeaderRow) {
                    isHeaderRow = false;
                    continue; // Skip header row
                }

                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                try {
                    ManifestRow row = parseRowFromCsv(line, rowNumber, form);
                    rows.add(row);
                } catch (Exception e) {
                    errors.add(new ParseError(rowNumber, "row", "Failed to parse row: " + e.getMessage()));
                    LogEvent.logError(this.getClass().getSimpleName(), "parseManifestCsv",
                        "Error parsing row " + rowNumber + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Failed to read CSV file: " + e.getMessage()));
            LogEvent.logError(this.getClass().getSimpleName(), "parseManifestCsv",
                "IO error reading CSV: " + e.getMessage());
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    public List<ParseError> validateManifest(ParsedManifest parsedManifest) {
        List<ParseError> errors = new ArrayList<>();

        for (ManifestRow row : parsedManifest.rows()) {
            validateRequiredFields(row, errors);
            validateSampleType(row, errors);
            validateDates(row, errors);
        }

        return errors;
    }

    @Override
    @Transactional
    public VirologyLabManifestImportResult createSamplesForEntry(Integer entryId,
            ParsedManifest parsedManifest, String sysUserId) {

        List<String> createdSampleIds = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();
        int totalRequested = parsedManifest.rows().size();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new VirologyLabManifestImportResult(totalRequested, 0, createdSampleIds, errors);
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

        for (ManifestRow row : parsedManifest.rows()) {
            try {
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

                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setExternalId(row.sampleId());
                item.setSortOrder("1");
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
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
                errors.add(new ParseError(row.rowNumber(), "sample",
                    "Failed to create sample: " + e.getMessage()));
                LogEvent.logError(this.getClass().getSimpleName(), "createSamplesForEntry",
                    "Error creating sample for row " + row.rowNumber() + ": " + e.getMessage());
            }
        }

        return new VirologyLabManifestImportResult(totalRequested, createdSampleIds.size(),
            createdSampleIds, errors);
    }

    private ManifestRow parseRowFromCsv(String line, int rowNumber, VirologyLabManifestImportForm form) {
        String[] columns = line.split(",", -1);
        Map<String, String> columnMap = new HashMap<>();

        // Map columns based on form configuration
        if (form.getSampleIdColumn() != null && !form.getSampleIdColumn().isEmpty()) {
            columnMap.put("sampleId", getColumnValue(columns, form.getSampleIdColumn()));
        }
        if (form.getSampleTypeColumn() != null && !form.getSampleTypeColumn().isEmpty()) {
            columnMap.put("sampleType", getColumnValue(columns, form.getSampleTypeColumn()));
        }
        // Add other column mappings as needed...

        return new ManifestRow(
            rowNumber,
            columnMap.getOrDefault("sampleId", ""),
            columnMap.getOrDefault("sampleType", ""),
            columnMap.getOrDefault("source", ""),
            columnMap.getOrDefault("collectionDate", ""),
            columnMap.getOrDefault("receptionDateTime", ""),
            columnMap.getOrDefault("projectStudyAssociation", ""),
            columnMap.getOrDefault("volumeConcentration", ""),
            columnMap.getOrDefault("a260_280", ""),
            columnMap.getOrDefault("a260_230", ""),
            columnMap.getOrDefault("rin", ""),
            columnMap.getOrDefault("extractionMethodKit", ""),
            columnMap.getOrDefault("pcrProtocol", ""),
            columnMap.getOrDefault("libraryPrepProtocol", ""),
            columnMap.getOrDefault("sequencingPlatform", ""),
            columnMap.getOrDefault("runId", ""),
            columnMap.getOrDefault("operator", ""),
            columnMap.getOrDefault("processingDateTime", ""),
            columnMap.getOrDefault("notes", "")
        );
    }

    private String getColumnValue(String[] columns, String columnName) {
        // This is a simplified implementation
        // In practice, you'd map column names to indices
        return columns.length > 0 ? columns[0].trim().replace("\"", "") : "";
    }

    private void validateRequiredFields(ManifestRow row, List<ParseError> errors) {
        if (row.sampleId() == null || row.sampleId().trim().isEmpty()) {
            errors.add(new ParseError(row.rowNumber(), "sampleId", "Sample ID is required"));
        }
        if (row.sampleType() == null || row.sampleType().trim().isEmpty()) {
            errors.add(new ParseError(row.rowNumber(), "sampleType", "Sample Type is required"));
        }
        if (row.source() == null || row.source().trim().isEmpty()) {
            errors.add(new ParseError(row.rowNumber(), "source", "Source is required"));
        }
        if (row.collectionDate() == null || row.collectionDate().trim().isEmpty()) {
            errors.add(new ParseError(row.rowNumber(), "collectionDate", "Collection Date is required"));
        }
        if (row.receptionDateTime() == null || row.receptionDateTime().trim().isEmpty()) {
            errors.add(new ParseError(row.rowNumber(), "receptionDateTime", "Reception Date Time is required"));
        }
    }

    private void validateSampleType(ManifestRow row, List<ParseError> errors) {
        if (row.sampleType() != null && !row.sampleType().trim().isEmpty()) {
            if (!VALID_VIROLOGY_SAMPLE_TYPES.contains(row.sampleType().trim())) {
                errors.add(new ParseError(row.rowNumber(), "sampleType",
                    "Invalid sample type: " + row.sampleType() + ". Must be one of: " + VALID_VIROLOGY_SAMPLE_TYPES));
            }
        }
    }

    private void validateDates(ManifestRow row, List<ParseError> errors) {
        // Validate collection date
        if (row.collectionDate() != null && !row.collectionDate().trim().isEmpty()) {
            try {
                LocalDate.parse(row.collectionDate().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                errors.add(new ParseError(row.rowNumber(), "collectionDate",
                    "Invalid date format. Expected YYYY-MM-DD"));
            }
        }

        // Validate reception date time
        if (row.receptionDateTime() != null && !row.receptionDateTime().trim().isEmpty()
                && !"now".equalsIgnoreCase(row.receptionDateTime().trim())) {
            try {
                LocalDateTime.parse(row.receptionDateTime().trim().replace(" ", "T"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                errors.add(new ParseError(row.rowNumber(), "receptionDateTime",
                    "Invalid date time format. Expected YYYY-MM-DD HH:MM or 'now'"));
            }
        }
    }

    private Map<String, Object> getData(ManifestRow row, NotebookPageSample nps, Sample parentSample) {
        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData()) : new HashMap<>();

        // Add VirologyLab-specific metadata
        data.put("sampleId", row.sampleId());
        data.put("sampleType", row.sampleType());
        data.put("source", row.source());
        data.put("collectionDate", row.collectionDate());
        data.put("receptionDateTime", row.receptionDateTime());
        data.put("projectStudyAssociation", row.projectStudyAssociation());
        data.put("volumeConcentration", row.volumeConcentration());
        data.put("a260_280", row.a260_280());
        data.put("a260_230", row.a260_230());
        data.put("rin", row.rin());
        data.put("extractionMethodKit", row.extractionMethodKit());
        data.put("pcrProtocol", row.pcrProtocol());
        data.put("libraryPrepProtocol", row.libraryPrepProtocol());
        data.put("sequencingPlatform", row.sequencingPlatform());
        data.put("runId", row.runId());
        data.put("operator", row.operator());
        data.put("processingDateTime", row.processingDateTime());
        data.put("notes", row.notes());

        return data;
    }
}