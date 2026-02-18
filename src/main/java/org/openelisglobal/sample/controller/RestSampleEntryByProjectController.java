package org.openelisglobal.sample.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.common.services.StatusSet;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.saving.IAccessioner;
import org.openelisglobal.patient.saving.ISampleEntry;
import org.openelisglobal.patient.saving.ISampleEntryAfterPatientEntry;
import org.openelisglobal.patient.saving.ISampleSecondEntry;
import org.openelisglobal.sample.form.SampleEntryByProjectForm;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/SampleEntryByProject")
public class RestSampleEntryByProjectController
    extends BaseSampleEntryController
{

    /** Holds the last save-failure detail so the REST endpoint can report it. */
    private final ThreadLocal<String> lastSaveError = new ThreadLocal<>();

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @GetMapping
    public ResponseEntity<SampleEntryByProjectForm> getSampleEntryByProject(
        HttpServletRequest request,
        @RequestParam(
            value = "type",
            required = false,
            defaultValue = "initial"
        ) String type,
        @RequestParam(
            value = "externalOrderNumber",
            required = false
        ) String externalOrderNumber
    )
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            SampleEntryByProjectForm form = new SampleEntryByProjectForm();

            // Set current date
            Date today = Calendar.getInstance().getTime();
            String dateAsText = DateUtil.formatDateAsText(today);
            form.setReceivedDateForDisplay(dateAsText);
            form.setInterviewDate(dateAsText);
            form.setCurrentDate(dateAsText);

            // Load electronic order if externalOrderNumber is provided
            if (!GenericValidator.isBlankOrNull(externalOrderNumber)) {
                loadElectronicOrder(form, externalOrderNumber);
            }

            // Set display lists
            setDisplayLists(form, request);

            return ResponseEntity.ok(form);
        } catch (Exception e) {
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "getSampleEntryByProject",
                "Error processing GET request: " + e.getMessage()
            );
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                null
            );
        }
    }

    private void loadElectronicOrder(
        SampleEntryByProjectForm form,
        String externalOrderNumber
    ) {
        try {
            List<ElectronicOrder> eOrders =
                electronicOrderService.getElectronicOrdersByExternalId(
                    externalOrderNumber
                );
            if (eOrders != null && !eOrders.isEmpty()) {
                ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);
                form.setElectronicOrder(eOrder);
            } else {
            }
        } catch (Exception e) {
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "loadElectronicOrder",
                "Error loading electronic order: " + e.getMessage()
            );
            LogEvent.logError(e);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> postSampleEntryByProject(
        HttpServletRequest request,
        @RequestBody @Valid SampleEntryByProjectForm form,
        BindingResult result,
        @RequestParam(
            value = "type",
            required = false,
            defaultValue = "initial"
        ) String type
    )
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try {
            Map<String, Object> response = new HashMap<>();

            // ---- Early duplicate accession number check ----
            String labNo = form.getLabNo();
            if (!GenericValidator.isBlankOrNull(labNo)) {
                StatusSet statusSet = SpringContext.getBean(
                    IStatusService.class
                ).getStatusSetForAccessionNumber(labNo);
                RecordStatus sampleRecordStatus =
                    statusSet.getSampleRecordStatus();

                if ("initial".equals(type)) {
                    // For initial entry the sample must not exist yet.
                    // sampleRecordStatus == null means no sample found.
                    if (sampleRecordStatus != null) {
                        response.put("success", false);
                        response.put(
                            "message",
                            "Lab number " +
                                labNo +
                                " already exists. Please use a different lab number."
                        );
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                            response
                        );
                    }
                } else if ("verify".equals(type)) {
                    // For double entry the sample must already exist at InitialRegistration.
                    if (sampleRecordStatus == null) {
                        response.put("success", false);
                        response.put(
                            "message",
                            "Lab number " +
                                labNo +
                                " does not exist. Please perform initial entry first."
                        );
                        return ResponseEntity.status(
                            HttpStatus.BAD_REQUEST
                        ).body(response);
                    }
                    if (
                        sampleRecordStatus != RecordStatus.InitialRegistration
                    ) {
                        response.put("success", false);
                        response.put(
                            "message",
                            "Lab number " +
                                labNo +
                                " is not eligible for double entry (status: " +
                                sampleRecordStatus +
                                ")."
                        );
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                            response
                        );
                    }
                }
            }
            // ---- End duplicate check ----

            // Validate type parameter
            if (!("initial".equals(type) || "verify".equals(type))) {
                response.put("success", false);
                response.put(
                    "message",
                    "Invalid type parameter. Must be 'initial' or 'verify'."
                );
                return ResponseEntity.badRequest().body(response);
            }

            if (result.hasErrors()) {
                String fieldSummary = result
                    .getFieldErrors()
                    .stream()
                    .map(e -> e.getField() + "=" + e.getDefaultMessage())
                    .collect(Collectors.joining(", "));
                String globalSummary = result
                    .getGlobalErrors()
                    .stream()
                    .map(e -> e.getCode())
                    .collect(Collectors.joining(", "));
                response.put("success", false);
                response.put("errors", result.getAllErrors());
                return ResponseEntity.badRequest().body(response);
            }

            // Implement save logic using existing services
            String forward = null;

            lastSaveError.remove();

            if ("verify".equals(type)) {
                // Handle double entry / verification
                ISampleSecondEntry sampleSecondEntry = SpringContext.getBean(
                    ISampleSecondEntry.class
                );
                sampleSecondEntry.setFieldsFromForm(form);
                sampleSecondEntry.setSysUserId(getSysUserId(request));
                sampleSecondEntry.setRequest(request);
                if (sampleSecondEntry.canAccession()) {
                    forward = handleSaveWithLogging(
                        request,
                        sampleSecondEntry,
                        form
                    );
                } else {
                    lastSaveError.set(
                        "Sample with lab number " +
                            form.getLabNo() +
                            " is not eligible for double entry. " +
                            "It may not exist or may already be verified."
                    );
                }
            } else {
                // Handle initial entry.
                // SampleEntry: brand-new patient + brand-new sample (both statuses null).
                ISampleEntry sampleEntry = SpringContext.getBean(
                    ISampleEntry.class
                );
                sampleEntry.setFieldsFromForm(form);
                sampleEntry.setSysUserId(getSysUserId(request));
                sampleEntry.setRequest(request);
                if (sampleEntry.canAccession()) {
                    forward = handleSaveWithLogging(request, sampleEntry, form);
                } else {
                    // SampleEntryAfterPatientEntry: patient already exists,
                    // sample is new (sampleRecordStatus == NotRegistered).
                    ISampleEntryAfterPatientEntry sampleEntryAfterPatientEntry =
                        SpringContext.getBean(
                            ISampleEntryAfterPatientEntry.class
                        );
                    sampleEntryAfterPatientEntry.setFieldsFromForm(form);
                    sampleEntryAfterPatientEntry.setSysUserId(
                        getSysUserId(request)
                    );
                    sampleEntryAfterPatientEntry.setRequest(request);
                    if (sampleEntryAfterPatientEntry.canAccession()) {
                        forward = handleSaveWithLogging(
                            request,
                            sampleEntryAfterPatientEntry,
                            form
                        );
                    } else {
                        lastSaveError.set(
                            "No matching entry type found for patient/sample status combination."
                        );
                    }
                }
            }

            // Log what forward value we got
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "postSampleEntryByProject",
                "DEBUG: Lab number " +
                    form.getLabNo() +
                    " - forward value = " +
                    (forward != null ? "'" + forward + "'" : "null")
            );

            if (forward != null && forward.contains("success")) {
                response.put("success", true);
                response.put("message", "Sample saved successfully");
                response.put("labNumber", form.getLabNo());
                response.put("type", type);
                return ResponseEntity.ok(response);
            } else {
                // Check database as last resort before reporting failure
                boolean sampleInDB = verifySampleInDatabase(form.getLabNo());

                if (sampleInDB) {
                    // Sample is actually saved - report success
                    LogEvent.logError(
                        this.getClass().getSimpleName(),
                        "postSampleEntryByProject",
                        "Lab number " +
                            form.getLabNo() +
                            " was saved to database despite forward='" +
                            forward +
                            "' - returning success"
                    );
                    response.put("success", true);
                    response.put("message", "Sample saved successfully");
                    response.put("labNumber", form.getLabNo());
                    response.put("type", type);
                    return ResponseEntity.ok(response);
                }

                String errorDetail = lastSaveError.get();
                if (errorDetail == null) {
                    errorDetail =
                        "Unable to save sample. Please verify that all required tests and sample types are configured in the system.";
                }

                LogEvent.logError(
                    this.getClass().getSimpleName(),
                    "postSampleEntryByProject",
                    "Save failed for lab number " +
                        form.getLabNo() +
                        ": " +
                        errorDetail
                );

                response.put("success", false);
                response.put("message", errorDetail);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    response
                );
            }
        } finally {
            lastSaveError.remove();
        }
    }

    /**
     * Wraps {@link #handleSave} to capture the actual exception message and store
     * it in {@link #lastSaveError} so the REST endpoint can surface it to the
     * caller instead of the generic "no valid accessioner found" message.
     */
    private String handleSaveWithLogging(
        HttpServletRequest request,
        IAccessioner accessioner,
        org.openelisglobal.patient.saving.form.IAccessionerForm form
    ) {
        try {
            String forward = accessioner.save();
            return forward;
        } catch (Exception e) {
            // Check if the sample was actually saved to the database despite the exception.
            // This handles cases where the transaction commits but post-processing fails.
            boolean sampleExistsInDB = checkIfSampleExists(form.getLabNo());

            if (sampleExistsInDB) {
                // Sample was saved successfully - treat as success
                LogEvent.logError(
                    this.getClass().getSimpleName(),
                    "handleSaveWithLogging",
                    "Sample " +
                        form.getLabNo() +
                        " was saved to database but post-processing threw exception: " +
                        e.getMessage()
                );
                LogEvent.logError(e);
                // Return success since the data is in the database
                return org.openelisglobal.common.action.IActionConstants.FWD_SUCCESS_INSERT;
            }

            // Sample was not saved - this is a real failure
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "handleSaveWithLogging",
                "Save failed for lab number " +
                    form.getLabNo() +
                    ": " +
                    e.getMessage()
            );
            LogEvent.logError(e);

            // Build a human-readable error from the accessioner's message list
            String accessionerMessages = "";
            try {
                org.springframework.validation.Errors errors =
                    accessioner.getMessages();
                if (errors != null && errors.hasErrors()) {
                    accessionerMessages = StreamSupport.stream(
                        errors.getAllErrors().spliterator(),
                        false
                    )
                        .map(err ->
                            err.getDefaultMessage() != null
                                ? err.getDefaultMessage()
                                : err.getCode()
                        )
                        .collect(Collectors.joining("; "));
                }
            } catch (Exception ignored) {}

            String detail = !accessionerMessages.isEmpty()
                ? accessionerMessages
                : (e.getMessage() != null
                      ? e.getMessage()
                      : e.getClass().getSimpleName());

            lastSaveError.set(detail);
            return "insertFail";
        }
    }

    /**
     * Checks if a sample with the given accession number exists in the database.
     * Uses a new transaction to query the database after a potential commit.
     */
    private boolean checkIfSampleExists(String accessionNumber) {
        try {
            // Small delay to allow transaction to commit
            Thread.sleep(100);

            org.openelisglobal.sample.service.SampleService sampleService =
                SpringContext.getBean(
                    org.openelisglobal.sample.service.SampleService.class
                );
            org.openelisglobal.sample.valueholder.Sample sample =
                sampleService.getSampleByAccessionNumber(accessionNumber);
            return sample != null;
        } catch (Exception e) {
            // If verification fails, assume sample was not saved
            return false;
        }
    }

    /**
     * Verifies if a sample exists in the database (used by postSampleEntryByProject).
     * Waits longer to ensure transaction has committed.
     */
    private boolean verifySampleInDatabase(String accessionNumber) {
        try {
            // Wait for transaction to commit
            Thread.sleep(200);

            org.openelisglobal.sample.service.SampleService sampleService =
                SpringContext.getBean(
                    org.openelisglobal.sample.service.SampleService.class
                );
            org.openelisglobal.sample.valueholder.Sample sample =
                sampleService.getSampleByAccessionNumber(accessionNumber);

            boolean exists = sample != null;
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "verifySampleInDatabase",
                "Verification for " +
                    accessionNumber +
                    ": " +
                    (exists ? "FOUND in database" : "NOT FOUND")
            );

            return exists;
        } catch (Exception e) {
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "verifySampleInDatabase",
                "Exception during verification: " + e.getMessage()
            );
            return false;
        }
    }

    private void setDisplayLists(
        SampleEntryByProjectForm form,
        HttpServletRequest request
    ) {
        // Gender list - converted to Dictionary format for consistency
        Map<String, List<Dictionary>> formListsMapOfLists = new HashMap<>();
        List<Dictionary> listOfDictionary = new ArrayList<>();
        List<IdValuePair> genders = DisplayListService.getInstance().getList(
            ListType.GENDERS
        );

        for (IdValuePair i : genders) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(i.getId());
            dictionary.setDictEntry(i.getValue());
            listOfDictionary.add(dictionary);
        }

        formListsMapOfLists.put("GENDERS", listOfDictionary);
        form.setFormLists(formListsMapOfLists);

        // Also set genders list for backward compatibility
        form.setGenders(genders);

        // Dictionary lists - using ObservationHistoryList for comprehensive coverage
        Map<String, List<Dictionary>> observationHistoryMapOfLists =
            new HashMap<>();

        // EID-related lists
        observationHistoryMapOfLists.put(
            "EID_WHICH_PCR",
            ObservationHistoryList.EID_WHICH_PCR.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_SECOND_PCR_REASON",
            ObservationHistoryList.EID_SECOND_PCR_REASON.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_TYPE_OF_CLINIC",
            ObservationHistoryList.EID_TYPE_OF_CLINIC.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_HOW_CHILD_FED",
            ObservationHistoryList.EID_HOW_CHILD_FED.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_STOPPED_BREASTFEEDING",
            ObservationHistoryList.EID_STOPPED_BREASTFEEDING.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_INFANT_PROPHYLAXIS_ARV",
            ObservationHistoryList.EID_INFANT_PROPHYLAXIS_ARV.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_MOTHERS_HIV_STATUS",
            ObservationHistoryList.EID_MOTHERS_HIV_STATUS.getList()
        );
        observationHistoryMapOfLists.put(
            "EID_MOTHERS_ARV_TREATMENT",
            ObservationHistoryList.EID_MOTHERS_ARV_TREATMENT.getList()
        );

        // General Yes/No lists
        observationHistoryMapOfLists.put(
            "YES_NO",
            ObservationHistoryList.YES_NO.getList()
        );
        observationHistoryMapOfLists.put(
            "YES_NO_UNKNOWN",
            ObservationHistoryList.YES_NO_UNKNOWN.getList()
        );

        // HIV-related lists
        observationHistoryMapOfLists.put(
            "HIV_STATUSES",
            ObservationHistoryList.HIV_STATUSES.getList()
        );
        observationHistoryMapOfLists.put(
            "HIV_TYPES",
            ObservationHistoryList.HIV_TYPES.getList()
        );

        // ARV-related lists
        observationHistoryMapOfLists.put(
            "ARV_REGIME",
            ObservationHistoryList.ARV_REGIME.getList()
        );
        observationHistoryMapOfLists.put(
            "ARV_REASON_FOR_VL_DEMAND",
            ObservationHistoryList.ARV_REASON_FOR_VL_DEMAND.getList()
        );

        // Special request and HPV
        observationHistoryMapOfLists.put(
            "SPECIAL_REQUEST_REASONS",
            ObservationHistoryList.SPECIAL_REQUEST_REASONS.getList()
        );
        observationHistoryMapOfLists.put(
            "HPV_SAMPLING_METHOD",
            ObservationHistoryList.HPV_SAMPLING_METHOD.getList()
        );

        form.setDictionaryLists(observationHistoryMapOfLists);

        // Organization lists - using OrganizationTypeList for proper organization
        // grouping
        Map<String, List<Organization>> organizationTypeMapOfLists =
            new HashMap<>();

        organizationTypeMapOfLists.put(
            "ARV_ORGS",
            OrganizationTypeList.ARV_ORGS.getList()
        );
        organizationTypeMapOfLists.put(
            "ARV_ORGS_BY_NAME",
            OrganizationTypeList.ARV_ORGS_BY_NAME.getList()
        );
        organizationTypeMapOfLists.put(
            "EID_ORGS_BY_NAME",
            OrganizationTypeList.EID_ORGS_BY_NAME.getList()
        );
        organizationTypeMapOfLists.put(
            "EID_ORGS",
            OrganizationTypeList.EID_ORGS.getList()
        );

        form.setOrganizationTypeLists(organizationTypeMapOfLists);
    }

    @Override
    protected String findLocalForward(String forward) {
        if ("success".equals(forward)) {
            return "success";
        } else if ("fail".equals(forward)) {
            return "fail";
        } else {
            return "error";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "sample.entry.by.project.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "sample.entry.by.project.subtitle";
    }
}
