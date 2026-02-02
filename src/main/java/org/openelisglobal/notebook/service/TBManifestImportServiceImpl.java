package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.notebook.form.TBManifestImportForm;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.DepartmentSampleTypeService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Implementation of TBManifestImportService for spec-compliant TB manifest
 * processing. Handles TB data points per FR-014: specimen information, patient
 * metadata, clinical context, requested tests, and receipt details.
 */
@Service
public class TBManifestImportServiceImpl implements TBManifestImportService {

    private static final Logger logger = LoggerFactory.getLogger(TBManifestImportServiceImpl.class);

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private DepartmentSampleTypeService departmentSampleTypeService;

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private TBSampleCreationService tbSampleCreationService;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, TBManifestImportForm columnMapping) {
        List<TBManifestRow> rows = new ArrayList<>();
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

            // Get column indices from TB mapping
            // A. Sample Identity
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());

            // B. Specimen Information
            Integer specimenTypeIdx = getColumnIndex(columnIndex, columnMapping.getSpecimenTypeColumn());
            Integer specimenQualityIdx = getColumnIndex(columnIndex, columnMapping.getSpecimenQualityColumn());

            // C. Request Paper Details
            Integer documentNumberIdx = getColumnIndex(columnIndex, columnMapping.getDocumentNumberColumn());
            Integer referringFacilityIdx = getColumnIndex(columnIndex, columnMapping.getReferringFacilityColumn());

            // D. Patient Metadata
            Integer patientNameIdx = getColumnIndex(columnIndex, columnMapping.getPatientNameColumn());
            Integer patientAgeIdx = getColumnIndex(columnIndex, columnMapping.getPatientAgeColumn());
            Integer patientSexIdx = getColumnIndex(columnIndex, columnMapping.getPatientSexColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer studyIdIdx = getColumnIndex(columnIndex, columnMapping.getStudyIdColumn());
            Integer patientAddressIdx = getColumnIndex(columnIndex, columnMapping.getPatientAddressColumn());
            Integer patientPhoneIdx = getColumnIndex(columnIndex, columnMapping.getPatientPhoneColumn());
            Integer physicianPhoneIdx = getColumnIndex(columnIndex, columnMapping.getPhysicianPhoneColumn());
            Integer consentStatusIdx = getColumnIndex(columnIndex, columnMapping.getConsentStatusColumn());

            // E. Clinical Context
            Integer treatmentHistoryIdx = getColumnIndex(columnIndex, columnMapping.getTreatmentHistoryColumn());

            // F. Requested Tests
            Integer cultureIdx = getColumnIndex(columnIndex, columnMapping.getCultureColumn());
            Integer smearMicroscopyIdx = getColumnIndex(columnIndex, columnMapping.getSmearMicroscopyColumn());
            Integer genexpertIdx = getColumnIndex(columnIndex, columnMapping.getGenexpertColumn());
            Integer identificationIdx = getColumnIndex(columnIndex, columnMapping.getIdentificationColumn());
            Integer dstFirstLineIdx = getColumnIndex(columnIndex, columnMapping.getDstFirstLineColumn());
            Integer dstSecondLineIdx = getColumnIndex(columnIndex, columnMapping.getDstSecondLineColumn());
            Integer intendedMethodIdx = getColumnIndex(columnIndex, columnMapping.getIntendedMethodColumn());

            // G. Receipt Details
            Integer receivedSiteIdx = getColumnIndex(columnIndex, columnMapping.getReceivedSiteColumn());
            Integer receivedDateIdx = getColumnIndex(columnIndex, columnMapping.getReceivedDateColumn());
            Integer receivedTimeIdx = getColumnIndex(columnIndex, columnMapping.getReceivedTimeColumn());

            // Common
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract values
                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String specimenType = getValueAtIndex(values, specimenTypeIdx);
                String specimenQuality = getValueAtIndex(values, specimenQualityIdx);
                String documentNumber = getValueAtIndex(values, documentNumberIdx);
                String referringFacility = getValueAtIndex(values, referringFacilityIdx);
                String patientName = getValueAtIndex(values, patientNameIdx);
                String patientAge = getValueAtIndex(values, patientAgeIdx);
                String patientSex = getValueAtIndex(values, patientSexIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String studyId = getValueAtIndex(values, studyIdIdx);
                String patientAddress = getValueAtIndex(values, patientAddressIdx);
                String patientPhone = getValueAtIndex(values, patientPhoneIdx);
                String physicianPhone = getValueAtIndex(values, physicianPhoneIdx);
                String consentStatus = getValueAtIndex(values, consentStatusIdx);
                String treatmentHistory = getValueAtIndex(values, treatmentHistoryIdx);
                String culture = getValueAtIndex(values, cultureIdx);
                String smearMicroscopy = getValueAtIndex(values, smearMicroscopyIdx);
                String genexpert = getValueAtIndex(values, genexpertIdx);
                String identification = getValueAtIndex(values, identificationIdx);
                String dstFirstLine = getValueAtIndex(values, dstFirstLineIdx);
                String dstSecondLine = getValueAtIndex(values, dstSecondLineIdx);
                String intendedMethod = getValueAtIndex(values, intendedMethodIdx);
                String receivedSite = getValueAtIndex(values, receivedSiteIdx);
                String receivedDate = getValueAtIndex(values, receivedDateIdx);
                String receivedTime = getValueAtIndex(values, receivedTimeIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);

                // Validate required fields
                if (specimenType == null || specimenType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "specimenType", "Specimen type is required"));
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

                rows.add(new TBManifestRow(rowNumber, sampleId, specimenType.trim(), specimenQuality, documentNumber,
                        referringFacility, patientName, patientAge, patientSex, patientId, studyId, patientAddress,
                        patientPhone, physicianPhone, consentStatus, treatmentHistory, culture, smearMicroscopy,
                        genexpert, identification, dstFirstLine, dstSecondLine, intendedMethod, receivedSite,
                        receivedDate, receivedTime, numOfSamples));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateSpecimenTypes(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (TBManifestRow row : manifest.rows()) {
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
    @Transactional(readOnly = true)
    public List<ParseError> validateSpecimenTypesForEntry(Integer entryId, ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        // Get the notebook entry to find linked departments
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            // Fall back to global validation if entry not found
            return validateSpecimenTypes(manifest);
        }

        NotebookEntry entry = optEntry.get();

        // Get linked departments from the notebook template
        Set<TestSection> departments = entry.getLinkedDepartments();

        // If no departments linked, fall back to global validation
        if (departments.isEmpty()) {
            logger.info("No departments linked to entry {}, using global specimen type validation", entryId);
            return validateSpecimenTypes(manifest);
        }

        // Collect department IDs
        List<String> departmentIds = departments.stream().map(ts -> String.valueOf(ts.getId()))
                .collect(Collectors.toList());

        // Get allowed sample types from all linked departments (union)
        List<TypeOfSample> allowedTypes = departmentSampleTypeService.getSampleTypesForDepartments(departmentIds);
        Set<String> allowedDescriptions = new HashSet<>();
        for (TypeOfSample type : allowedTypes) {
            if (type.getDescription() != null) {
                allowedDescriptions.add(type.getDescription().toLowerCase().trim());
            }
        }

        logger.info("Entry {} has {} linked departments with {} allowed specimen types", entryId, departmentIds.size(),
                allowedDescriptions.size());

        // Validate each manifest row's specimen type against allowed types
        for (TBManifestRow row : manifest.rows()) {
            String specimenType = row.specimenType();
            if (specimenType == null || specimenType.isBlank()) {
                continue; // Required field validation handled elsewhere
            }

            String normalizedType = specimenType.toLowerCase().trim();
            if (!allowedDescriptions.contains(normalizedType)) {
                errors.add(new ParseError(row.rowNumber(), "specimenType",
                        "Specimen type '" + specimenType + "' is not allowed for the linked departments"));
            }
        }

        return errors;
    }

    @Override
    public TBManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId) {
        List<SampleItem> createdSamples = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        // Verify entry exists
        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new TBManifestImportResult(0, 0, createdSamples, createdAccessionNumbers, errors);
        }

        int totalRequested = 0;

        // Process each row in its own transaction using REQUIRES_NEW
        for (TBManifestRow row : manifest.rows()) {
            if (row.numOfSamples() <= 0) {
                continue;
            }

            totalRequested += row.numOfSamples();

            logger.info("Processing row {} with {} samples", row.rowNumber(), row.numOfSamples());

            // Each row is processed in its own transaction
            TBSampleCreationService.RowCreationResult result = tbSampleCreationService.createSamplesForRow(entryId, row,
                    sysUserId);

            if (result.success()) {
                createdSamples.addAll(result.createdSamples());
                createdAccessionNumbers.addAll(result.accessionNumbers());
                logger.info("Row {} succeeded, created {} samples", row.rowNumber(), result.createdSamples().size());
            } else {
                errors.add(new ParseError(row.rowNumber(), "sample", result.errorMessage()));
                logger.warn("Row {} failed: {}", row.rowNumber(), result.errorMessage());
            }
        }

        // Link samples to notebook entry
        if (!createdSamples.isEmpty()) {
            try {
                List<Integer> createdIds = createdSamples.stream().map(s -> Integer.parseInt(s.getId()))
                        .collect(Collectors.toList());
                notebookSampleEntryService.linkSamplesToNotebook(entryId, createdIds);
            } catch (Exception e) {
                errors.add(new ParseError(0, "linking", "Failed to link samples to notebook: " + e.getMessage()));
            }
        }

        return new TBManifestImportResult(totalRequested, createdSamples.size(), createdSamples,
                createdAccessionNumbers.stream().distinct().collect(Collectors.toList()), errors);
    }

    @Override
    public String generateExternalId(String sampleId, int sequenceNumber) {
        return String.format("TB-%s-%03d", sampleId, sequenceNumber);
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
}
