/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.medlab.form.MedLabManifestImportForm;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO.SampleDomain;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of MedLabManifestImportService for CSV manifest processing.
 *
 * <p>
 * Handles the MedLab-specific manifest with 13 fields per spec FR-010 to
 * FR-014. Supports anonymous samples (patientId = NULL) and optional order
 * linking.
 */
@Service
public class MedLabManifestImportServiceImpl implements MedLabManifestImportService {

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private TestService testService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OrderSampleLinkService orderSampleLinkService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    // Valid container types per spec FR-014
    private static final Set<String> VALID_CONTAINER_TYPES = Set.of("vacutainer", "cryovial", "urine_cup", "stool_jar",
            "swab_tube", "edta", "sst", "heparin", "citrate", "plain");

    @Override
    public ParsedMedLabManifest parseManifestCsv(InputStream csvInput, MedLabManifestImportForm columnMapping) {
        List<MedLabManifestRow> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                errors.add(new ParseError(0, "file", "CSV file is empty"));
                return new ParsedMedLabManifest(rows, errors);
            }

            // Parse header and build column index map
            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Get column indices from mapping (required fields)
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());
            Integer sampleTypeIdx = getColumnIndex(columnIndex, columnMapping.getSampleTypeColumn());
            Integer containerTypeIdx = getColumnIndex(columnIndex, columnMapping.getContainerTypeColumn());
            Integer quantityIdx = getColumnIndex(columnIndex, columnMapping.getQuantityColumn());
            Integer unitOfMeasureIdx = getColumnIndex(columnIndex, columnMapping.getUnitOfMeasureColumn());
            Integer collectionSourceIdx = getColumnIndex(columnIndex, columnMapping.getCollectionSourceColumn());
            Integer collectorIdx = getColumnIndex(columnIndex, columnMapping.getCollectorColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer collectionTimeIdx = getColumnIndex(columnIndex, columnMapping.getCollectionTimeColumn());

            // Get column indices from mapping (optional fields)
            Integer customLabelIdx = getColumnIndex(columnIndex, columnMapping.getCustomLabelColumn());
            Integer orderIdIdx = getColumnIndex(columnIndex, columnMapping.getOrderIdColumn());
            Integer patientIdIdx = getColumnIndex(columnIndex, columnMapping.getPatientIdColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // Header is row 1
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                // Extract required values
                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String sampleType = getValueAtIndex(values, sampleTypeIdx);
                String containerType = getValueAtIndex(values, containerTypeIdx);
                String quantity = getValueAtIndex(values, quantityIdx);
                String unitOfMeasure = getValueAtIndex(values, unitOfMeasureIdx);
                String collectionSource = getValueAtIndex(values, collectionSourceIdx);
                String collector = getValueAtIndex(values, collectorIdx);
                String collectionDate = getValueAtIndex(values, collectionDateIdx);
                String collectionTime = getValueAtIndex(values, collectionTimeIdx);

                // Extract optional values
                String customLabel = getValueAtIndex(values, customLabelIdx);
                String orderId = getValueAtIndex(values, orderIdIdx);
                String patientId = getValueAtIndex(values, patientIdIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                boolean hasError = false;

                if (sampleId == null || sampleId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleId", "Sample ID is required"));
                    hasError = true;
                }

                if (sampleType == null || sampleType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sampleType", "Sample Type is required"));
                    hasError = true;
                }

                if (containerType == null || containerType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "containerType", "Container Type is required"));
                    hasError = true;
                }

                if (quantity == null || quantity.isBlank()) {
                    errors.add(new ParseError(rowNumber, "quantity", "Quantity is required"));
                    hasError = true;
                }

                if (unitOfMeasure == null || unitOfMeasure.isBlank()) {
                    errors.add(new ParseError(rowNumber, "unitOfMeasure", "Unit of Measure is required"));
                    hasError = true;
                }

                if (collectionSource == null || collectionSource.isBlank()) {
                    errors.add(new ParseError(rowNumber, "collectionSource", "Collection Source is required"));
                    hasError = true;
                }

                if (collector == null || collector.isBlank()) {
                    errors.add(new ParseError(rowNumber, "collector", "Collector is required"));
                    hasError = true;
                }

                if (collectionDate == null || collectionDate.isBlank()) {
                    errors.add(new ParseError(rowNumber, "collectionDate", "Collection Date is required"));
                    hasError = true;
                }

                if (collectionTime == null || collectionTime.isBlank()) {
                    errors.add(new ParseError(rowNumber, "collectionTime", "Collection Time is required"));
                    hasError = true;
                }

                // Always add the row to manifest (even with errors) so invalid rows can be
                // displayed
                // The controller will separate valid/invalid rows based on error row numbers
                rows.add(new MedLabManifestRow(rowNumber, sampleId != null ? sampleId.trim() : "",
                        sampleType != null ? sampleType.trim() : "", containerType != null ? containerType.trim() : "",
                        customLabel != null ? customLabel.trim() : null, quantity != null ? quantity.trim() : "",
                        unitOfMeasure != null ? unitOfMeasure.trim() : "",
                        collectionSource != null ? collectionSource.trim() : "",
                        collector != null ? collector.trim() : "", collectionDate != null ? collectionDate.trim() : "",
                        collectionTime != null ? collectionTime.trim() : "", orderId != null ? orderId.trim() : null,
                        patientId != null ? patientId.trim() : null, notes != null ? notes.trim() : null));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedMedLabManifest(rows, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateSampleTypes(ParsedMedLabManifest manifest) {
        List<ParseError> errors = new ArrayList<>();
        Set<String> invalidTypes = new HashSet<>();

        for (MedLabManifestRow row : manifest.rows()) {
            if (invalidTypes.contains(row.sampleType().toLowerCase())) {
                errors.add(new ParseError(row.rowNumber(), "sampleType", "Invalid sample type: " + row.sampleType()));
                continue;
            }

            TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(row.sampleType(),
                    "H");
            if (typeOfSample == null) {
                // Try by description using domain enum
                List<TypeOfSample> types = typeOfSampleService.getTypesForDomainBySortOrder(SampleDomain.HUMAN);
                boolean found = types != null && types.stream().anyMatch(t -> {
                    String desc = t.getDescription();
                    String abbrev = t.getLocalAbbreviation();
                    return (desc != null && desc.equalsIgnoreCase(row.sampleType()))
                            || (abbrev != null && abbrev.equalsIgnoreCase(row.sampleType()));
                });
                if (!found) {
                    invalidTypes.add(row.sampleType().toLowerCase());
                    errors.add(
                            new ParseError(row.rowNumber(), "sampleType", "Invalid sample type: " + row.sampleType()));
                }
            }
        }

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateContainerTypes(ParsedMedLabManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        for (MedLabManifestRow row : manifest.rows()) {
            String containerType = row.containerType().toLowerCase();
            if (!VALID_CONTAINER_TYPES.contains(containerType)) {
                errors.add(new ParseError(row.rowNumber(), "containerType",
                        "Invalid container type: " + row.containerType()
                                + ". Valid types: vacutainer, cryovial, urine_cup, stool_jar, swab_tube"));
            }
        }

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateDuplicateSampleIds(ParsedMedLabManifest manifest) {
        List<ParseError> errors = new ArrayList<>();
        Set<String> checkedIds = new HashSet<>();

        for (MedLabManifestRow row : manifest.rows()) {
            String sampleId = row.sampleId();
            if (sampleId == null || sampleId.isBlank()) {
                continue; // Required field validation is done elsewhere
            }

            // Check for duplicates within the manifest itself
            if (checkedIds.contains(sampleId)) {
                errors.add(new ParseError(row.rowNumber(), "sampleId", "Duplicate sample ID in manifest: " + sampleId));
                continue;
            }
            checkedIds.add(sampleId);

            // Check if sample ID already exists in database
            Sample existingSample = sampleService.getSampleByAccessionNumber(sampleId);
            if (existingSample != null) {
                errors.add(new ParseError(row.rowNumber(), "sampleId",
                        "Sample ID already exists in database: " + sampleId));
            }
        }

        return errors;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public ValidationResult validatePatientAndOrderReferences(ParsedMedLabManifest manifest) {
        List<ParseError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // Cache lookups to avoid repeated database calls
        Map<String, Patient> patientCache = new HashMap<>();
        Map<String, List<ElectronicOrder>> orderCache = new HashMap<>();
        Set<String> notFoundPatients = new HashSet<>();
        Set<String> notFoundOrders = new HashSet<>();

        for (MedLabManifestRow row : manifest.rows()) {
            String patientId = row.patientId();
            String orderId = row.orderId();

            Patient patient = null;
            List<ElectronicOrder> orders = null;

            // Validate patientId if provided - warn if not found (non-blocking)
            if (patientId != null && !patientId.isBlank()) {
                if (notFoundPatients.contains(patientId)) {
                    // Already checked, not found - warn but allow import
                    warnings.add(new ValidationWarning(row.rowNumber(), "patientId",
                            "Patient not found: " + patientId, ValidationWarning.WarningType.PATIENT_NOT_FOUND));
                } else if (patientCache.containsKey(patientId)) {
                    patient = patientCache.get(patientId);
                } else {
                    // Try to find patient by ID, external ID, or national ID
                    patient = findPatient(patientId);
                    if (patient != null) {
                        patientCache.put(patientId, patient);
                    } else {
                        notFoundPatients.add(patientId);
                        warnings.add(new ValidationWarning(row.rowNumber(), "patientId",
                                "Patient not found: " + patientId, ValidationWarning.WarningType.PATIENT_NOT_FOUND));
                    }
                }
            }

            // Validate orderId if provided - warn if not found (non-blocking)
            if (orderId != null && !orderId.isBlank()) {
                if (notFoundOrders.contains(orderId)) {
                    // Already checked, not found - warn but allow import
                    warnings.add(new ValidationWarning(row.rowNumber(), "orderId",
                            "Order not found: " + orderId, ValidationWarning.WarningType.ORDER_NOT_FOUND));
                } else if (orderCache.containsKey(orderId)) {
                    orders = orderCache.get(orderId);
                } else {
                    // Try to find order by external ID
                    try {
                        orders = electronicOrderService.getElectronicOrdersByExternalId(orderId);
                        if (orders != null && !orders.isEmpty()) {
                            orderCache.put(orderId, orders);
                        } else {
                            notFoundOrders.add(orderId);
                            warnings.add(new ValidationWarning(row.rowNumber(), "orderId",
                                    "Order not found: " + orderId, ValidationWarning.WarningType.ORDER_NOT_FOUND));
                        }
                    } catch (Exception e) {
                        LogEvent.logDebug(this.getClass().getSimpleName(), "validatePatientAndOrderReferences",
                                "Error looking up order: " + orderId);
                        notFoundOrders.add(orderId);
                        warnings.add(new ValidationWarning(row.rowNumber(), "orderId",
                                "Order not found: " + orderId, ValidationWarning.WarningType.ORDER_NOT_FOUND));
                    }
                }
            }

            // Validate patient-order consistency if both are provided and found
            if (patient != null && orders != null && !orders.isEmpty()) {
                try {
                    ElectronicOrder order = orders.get(0);
                    String orderPatientId = order.getPatient() != null ? order.getPatient().getId() : null;
                    if (orderPatientId != null && !orderPatientId.equals(patient.getId())) {
                        // Mismatch: order belongs to different patient - warn but allow import
                        String orderPatientName = "Unknown";
                        try {
                            if (order.getPatient() != null) {
                                orderPatientName = patientService.getLastFirstName(order.getPatient());
                            }
                        } catch (Exception e) {
                            // Use default "Unknown"
                        }
                        warnings.add(new ValidationWarning(row.rowNumber(), "patientId",
                                "Patient mismatch: Order " + orderId + " belongs to patient '" + orderPatientName
                                        + "', not '" + patientId + "'",
                                ValidationWarning.WarningType.PATIENT_ORDER_MISMATCH));
                    }
                } catch (Exception e) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "validatePatientAndOrderReferences",
                            "Error validating patient-order consistency: " + e.getMessage());
                }
            }
        }

        return new ValidationResult(errors, warnings);
    }

    /**
     * Find a patient by structured identifier (external ID or national ID). For
     * manifest imports, patients should ONLY be referenced by structured
     * identifiers, NOT by internal database PK. Attempting to look up by PK with an
     * alphanumeric structured ID causes exceptions that mark the transaction for
     * rollback.
     *
     * @param patientId the patient structured identifier to search for
     *                  (external_id or national_id)
     * @return the Patient if found, null otherwise
     */
    private Patient findPatient(String patientId) {
        // Try by external ID first (most common in manifest files)
        try {
            Patient patient = patientService.getPatientByExternalId(patientId);
            if (patient != null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "findPatient",
                        "Found patient by external ID: " + patientId);
                return patient;
            }
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findPatient",
                    "Patient not found by external ID: " + patientId);
        }

        // Try by national ID second
        try {
            Patient patient = patientService.getPatientByNationalId(patientId);
            if (patient != null) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "findPatient",
                        "Found patient by national ID: " + patientId);
                return patient;
            }
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findPatient",
                    "Patient not found by national ID: " + patientId);
        }

        // DO NOT look up by internal PK (getData) for manifest imports.
        // Manifest files should ONLY use structured identifiers (external_id,
        // national_id).
        // Calling getData() with alphanumeric IDs like "PAT-VALID-001" throws
        // exceptions when Hibernate tries to convert to numeric PK, marking the
        // transaction for rollback.

        LogEvent.logDebug(this.getClass().getSimpleName(), "findPatient",
                "Patient not found by any structured identifier: " + patientId);
        return null;
    }

    @Override
    @Transactional
    public ImportResult createSamplesForEntry(Integer entryId, ParsedMedLabManifest manifest, Integer createdBy,
            List<Integer> selectedTests, Integer pageId) {

        if (manifest.rows().isEmpty()) {
            return ImportResult.failure(List.of(new ParseError(0, "manifest", "No valid rows to import")));
        }

        NotebookEntry entry = notebookEntryService.get(entryId);
        if (entry == null) {
            return ImportResult.failure(List.of(new ParseError(0, "entryId", "Notebook entry not found: " + entryId)));
        }

        List<ParseError> errors = new ArrayList<>();
        int samplesCreated = 0;
        int analysesCreated = 0;
        List<SampleItem> createdSampleItems = new ArrayList<>();

        for (MedLabManifestRow row : manifest.rows()) {
            try {
                // Create Sample
                Sample sample = new Sample();
                sample.setAccessionNumber(row.sampleId());
                sample.setStatusId(statusService.getStatusID(SampleStatus.Entered));
                // setEnteredDate takes java.sql.Date
                sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
                sample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));

                // Store collection source in clientReference field (closest match)
                sample.setClientReference(row.collectionSource());

                // Set sysUserId for audit trail (REQUIRED)
                sample.setSysUserId(String.valueOf(createdBy));

                sampleService.insert(sample);

                // Create SampleItem
                SampleItem sampleItem = new SampleItem();
                sampleItem.setSample(sample);
                sampleItem.setSortOrder("1");
                sampleItem.setSysUserId(String.valueOf(createdBy));
                sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));

                // Store containerType in sourceOther (no collectionContainer field)
                sampleItem.setSourceOther(row.containerType());

                // Map customLabel to externalId
                if (row.customLabel() != null && !row.customLabel().isBlank()) {
                    sampleItem.setExternalId(row.customLabel());
                }

                // Map quantity (Double)
                try {
                    sampleItem.setQuantity(Double.parseDouble(row.quantity()));
                } catch (NumberFormatException e) {
                    errors.add(new ParseError(row.rowNumber(), "quantity", "Invalid quantity: " + row.quantity()));
                }

                // Store unitOfMeasure in unitOfMeasureName (String field)
                sampleItem.setUnitOfMeasureName(row.unitOfMeasure());

                // Map collector
                sampleItem.setCollector(row.collector());

                // Map collectionDate + collectionTime
                try {
                    Date collectionDateTime = parseDateTime(row.collectionDate(), row.collectionTime());
                    sampleItem.setCollectionDate(new Timestamp(collectionDateTime.getTime()));
                } catch (ParseException e) {
                    errors.add(new ParseError(row.rowNumber(), "collectionDate",
                            "Invalid date/time format: " + row.collectionDate() + " " + row.collectionTime()));
                }

                // Set sample type
                TypeOfSample typeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(row.sampleType(),
                        "H");
                if (typeOfSample == null) {
                    List<TypeOfSample> types = typeOfSampleService.getTypesForDomainBySortOrder(SampleDomain.HUMAN);
                    if (types != null) {
                        typeOfSample = types.stream().filter(t -> {
                            String desc = t.getDescription();
                            String abbrev = t.getLocalAbbreviation();
                            return (desc != null && desc.equalsIgnoreCase(row.sampleType()))
                                    || (abbrev != null && abbrev.equalsIgnoreCase(row.sampleType()));
                        }).findFirst().orElse(null);
                    }
                }
                if (typeOfSample != null) {
                    sampleItem.setTypeOfSample(typeOfSample);
                }

                sampleItemService.insert(sampleItem);

                // Collect sample items for notebook linking
                createdSampleItems.add(sampleItem);

                samplesCreated++;

                // Create Analysis records for selected tests
                if (selectedTests != null && !selectedTests.isEmpty()) {
                    for (Integer testId : selectedTests) {
                        Test test = testService.get(String.valueOf(testId));
                        if (test != null) {
                            Analysis analysis = new Analysis();
                            analysis.setSampleItem(sampleItem);
                            analysis.setTest(test);
                            analysis.setStatusId(statusService.getStatusID(AnalysisStatus.NotStarted));
                            analysis.setAnalysisType("M"); // Manual
                            analysis.setSysUserId(String.valueOf(createdBy));
                            analysisService.insert(analysis);
                            analysesCreated++;
                        }
                    }
                }

                // If orderId provided, create OrderSampleLink
                // Note: orderId in CSV is the external_id (string), not the internal ID
                if (row.orderId() != null && !row.orderId().isBlank()) {
                    try {
                        // Look up order by external ID (same as validation does)
                        List<ElectronicOrder> orders = electronicOrderService
                                .getElectronicOrdersByExternalId(row.orderId());
                        if (orders != null && !orders.isEmpty()) {
                            ElectronicOrder order = orders.get(0);
                            Integer electronicOrderId = Integer.parseInt(order.getId());
                            orderSampleLinkService.linkSampleToOrder(electronicOrderId,
                                    Integer.parseInt(sample.getId()), Integer.parseInt(sampleItem.getId()), null,
                                    createdBy);
                        } else {
                            // Order not found - this should have been caught by validation
                            LogEvent.logWarn("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                                    "Order not found for row " + row.rowNumber() + ": " + row.orderId());
                        }
                    } catch (Exception e) {
                        LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                                "Error linking sample to order for row " + row.rowNumber() + ": " + e.getMessage());
                        errors.add(new ParseError(row.rowNumber(), "orderId",
                                "Failed to link to order: " + e.getMessage()));
                    }
                }

                // If patientId provided, create SampleHuman link
                if (row.patientId() != null && !row.patientId().isBlank()) {
                    try {
                        Patient patient = findPatient(row.patientId());
                        if (patient != null) {
                            SampleHuman sampleHuman = new SampleHuman();
                            sampleHuman.setSampleId(sample.getId());
                            sampleHuman.setPatientId(patient.getId());
                            sampleHuman.setSysUserId(String.valueOf(createdBy));
                            sampleHumanService.insert(sampleHuman);
                        }
                        // Note: If patient not found, validation should have caught this earlier.
                        // We don't add an error here since validatePatientAndOrderReferences handles
                        // it.
                    } catch (Exception e) {
                        LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                                "Error linking sample to patient: " + e.getMessage());
                        // Don't fail the whole import if patient link fails
                    }
                }

            } catch (Exception e) {
                LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Error creating sample for row " + row.rowNumber() + ": " + e.getMessage());
                errors.add(new ParseError(row.rowNumber(), "sample", "Failed to create sample: " + e.getMessage()));
            }
        }

        // Link all samples to notebook entry using NotebookEntryService.addSamples()
        // This properly initializes the lazy samples collection before modification
        if (!createdSampleItems.isEmpty()) {
            try {
                notebookEntryService.addSamples(entryId, createdSampleItems, String.valueOf(createdBy));
                LogEvent.logInfo("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Successfully linked " + createdSampleItems.size() + " samples to notebook entry " + entryId);
            } catch (Exception e) {
                LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Error linking samples to notebook entry " + entryId + ": " + e.getClass().getName() + " - "
                                + e.getMessage());
                if (e.getCause() != null) {
                    LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                            "Root cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                }
                errors.add(new ParseError(0, "notebook", "Failed to link samples to notebook: " + e.getMessage()));
            }
        }

        // If pageId provided, link samples to that specific page
        // This enables the page to display the imported samples immediately
        if (pageId != null && !createdSampleItems.isEmpty()) {
            try {
                for (SampleItem sampleItem : createdSampleItems) {
                    notebookPageSampleService.createPageSampleForPage(pageId, Integer.parseInt(sampleItem.getId()),
                            Status.PENDING);
                }
                LogEvent.logInfo("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Successfully linked " + createdSampleItems.size() + " samples to notebook page " + pageId);
            } catch (Exception e) {
                LogEvent.logError("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Error linking samples to notebook page " + pageId + ": " + e.getClass().getName() + " - "
                                + e.getMessage());
                // Don't fail the import if page linking fails - samples are still created
                LogEvent.logWarn("MedLabManifestImportServiceImpl", "createSamplesForEntry",
                        "Samples created but not linked to page. They will appear when accessed via entry.");
            }
        }

        if (samplesCreated == 0) {
            return ImportResult.failure(errors);
        }

        return new ImportResult(errors.isEmpty() || samplesCreated > 0, samplesCreated, analysesCreated, errors);
    }

    /**
     * Parse a CSV line handling quoted fields.
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString().trim());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString().trim());

        return fields.toArray(new String[0]);
    }

    /**
     * Get column index from mapping.
     */
    private Integer getColumnIndex(Map<String, Integer> columnIndex, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        return columnIndex.get(columnName.trim().toLowerCase());
    }

    /**
     * Get value at index, returning null if out of bounds.
     */
    private String getValueAtIndex(String[] values, Integer index) {
        if (index == null || index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index].trim();
        // Remove surrounding quotes
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * Parse date and time strings into a Date object.
     */
    private Date parseDateTime(String dateStr, String timeStr) throws ParseException {
        String dateTimeStr = dateStr + " " + timeStr;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return formatter.parse(dateTimeStr);
    }
}
