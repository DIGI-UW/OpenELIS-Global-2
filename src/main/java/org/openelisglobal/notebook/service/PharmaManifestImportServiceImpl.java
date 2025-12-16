package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Pharmaceuticals manifest import: parses pharma-specific CSV, creates sample
 * + sample items, links to notebook entry, and stores metadata on page 1
 * NotebookPageSample data.
 */
@Service
public class PharmaManifestImportServiceImpl implements PharmaManifestImportService {

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
                return new ParsedManifest(rows, errors);
            }

            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }

            Integer groupIdIdx = getColumnIndex(columnIndex, columnMapping.getGroupIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());
            Integer chemicalNameIdx = getColumnIndex(columnIndex, columnMapping.getChemicalNameColumn());
            Integer gradeIdx = getColumnIndex(columnIndex, columnMapping.getGradeColumn());
            Integer lotNumberIdx = getColumnIndex(columnIndex, columnMapping.getLotNumberColumn());
            Integer domIdx = getColumnIndex(columnIndex, columnMapping.getDateOfManufactureColumn());
            Integer expiryIdx = getColumnIndex(columnIndex, columnMapping.getExpiryOrRetestDateColumn());
            Integer storageIdx = getColumnIndex(columnIndex, columnMapping.getStorageConditionColumn());
            Integer ownerIdx = getColumnIndex(columnIndex, columnMapping.getOwnerColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer trialIdx = getColumnIndex(columnIndex, columnMapping.getClinicalTrialNumberColumn());
            Integer consentIdx = getColumnIndex(columnIndex, columnMapping.getConsentStatusColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                String groupId = getValueAtIndex(values, groupIdIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);
                String chemicalName = getValueAtIndex(values, chemicalNameIdx);
                String grade = getValueAtIndex(values, gradeIdx);
                String lotNumber = getValueAtIndex(values, lotNumberIdx);
                String dateOfManufacture = getValueAtIndex(values, domIdx);
                String expiryOrRetestDate = getValueAtIndex(values, expiryIdx);
                String storageCondition = getValueAtIndex(values, storageIdx);
                String owner = getValueAtIndex(values, ownerIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String clinicalTrialNumber = getValueAtIndex(values, trialIdx);
                String consentStatus = getValueAtIndex(values, consentIdx);
                String notes = getValueAtIndex(values, notesIdx);

                if (groupId == null || groupId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "group_id", "Group ID is required"));
                    continue;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sample_type", "Sample type is required"));
                    continue;
                }

                int numOfSamples;
                try {
                    numOfSamples = numOfSamplesStr != null && !numOfSamplesStr.isBlank()
                            ? Integer.parseInt(numOfSamplesStr.trim())
                            : 1;
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(rowNumber, "num_of_samples", "Invalid number format: " + numOfSamplesStr));
                    continue;
                }

                rows.add(new PharmaManifestRow(rowNumber, groupId.trim(), sampleType.trim(), numOfSamples, chemicalName,
                        grade, lotNumber, dateOfManufacture, expiryOrRetestDate, storageCondition, owner, patientId,
                        clinicalTrialNumber, consentStatus, notes));
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
        for (PharmaManifestRow row : manifest.rows()) {
            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());
            TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
            if (found == null) {
                errors.add(new ParseError(row.rowNumber(), "sample_type", "Unknown sample type: " + row.sampleType()));
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

        int totalRequested = 0;
        int totalCreated = 0;

        for (PharmaManifestRow row : manifest.rows()) {
            if (row.numOfSamples() <= 0) {
                continue;
            }

            totalRequested += row.numOfSamples();

            TypeOfSample searchType = new TypeOfSample();
            searchType.setDescription(row.sampleType());
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
            if (sampleType == null) {
                errors.add(new ParseError(row.rowNumber(), "sample_type", "Unknown sample type: " + row.sampleType()));
                continue;
            }

            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
            String sampleIdDb = sampleService.generateAccessionNumberAndInsert(parentSample);
            parentSample.setId(sampleIdDb);

            for (int seq = 1; seq <= row.numOfSamples(); seq++) {
                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setTypeOfSample(sampleType);
                item.setExternalId(generateExternalId(row.groupId(), seq));
                item.setSortOrder(Integer.toString(seq));
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                totalCreated++;
                createdAccessionNumbers.add(parentSample.getAccessionNumber());

                notebookEntryService.addSample(entryId, item, sysUserId);

                if (firstPageId != null) {
                    NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(firstPageId,
                            Integer.parseInt(itemId));
                    if (nps != null) {
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();
                        data.put("sampleMaterial", row.sampleType());
                        data.put("chemicalName", row.chemicalName());
                        data.put("grade", row.grade());
                        data.put("lotNumber", row.lotNumber());
                        data.put("dateOfManufacture", row.dateOfManufacture());
                        data.put("expiryOrRetestDate", row.expiryOrRetestDate());
                        data.put("storageCondition", row.storageCondition());
                        data.put("owner", row.owner());
                        data.put("patientId", row.patientId());
                        data.put("clinicalTrialNumber", row.clinicalTrialNumber());
                        data.put("consentStatus", row.consentStatus());
                        data.put("notes", row.notes());
                        data.put("sampleCategory", "Pharmaceutical");
                        nps.setData(data);
                        notebookPageSampleService.update(nps);
                    }
                }
            }
        }

        return new PharmaManifestImportResult(totalRequested, totalCreated, errors,
                createdAccessionNumbers.stream().distinct().toList());
    }

    private String generateExternalId(String groupId, int seq) {
        return String.format("%s-%03d", groupId, seq);
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
}

