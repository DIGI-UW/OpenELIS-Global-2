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
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
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
        LogEvent.logInfo(
            this.getClass().getSimpleName(),
            "getSampleEntryByProject",
            "Received GET request for type: " +
                type +
                (externalOrderNumber != null
                    ? ", externalOrderNumber: " + externalOrderNumber
                    : "")
        );

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

            LogEvent.logInfo(
                this.getClass().getSimpleName(),
                "getSampleEntryByProject",
                "Returning form with display lists"
            );

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
                LogEvent.logInfo(
                    this.getClass().getSimpleName(),
                    "loadElectronicOrder",
                    "Loaded electronic order: " + externalOrderNumber
                );
            } else {
                LogEvent.logWarn(
                    this.getClass().getSimpleName(),
                    "loadElectronicOrder",
                    "Electronic order not found: " + externalOrderNumber
                );
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
        LogEvent.logInfo(
            this.getClass().getSimpleName(),
            "postSampleEntryByProject",
            "Received POST request for type: " + type
        );

        // ========== DEBUG LOGGING FOR TESTING VL SAVE FLOW ==========
        System.out.println("\n========== VL SAMPLE ENTRY DEBUG ==========");
        System.out.println("Lab Number: " + form.getLabNo());
        System.out.println("Project: " + form.getProject());
        System.out.println("Patient PK: " + form.getPatientPK());
        System.out.println("Subject Number: " + form.getSubjectNumber());
        System.out.println(
            "Site Subject Number: " + form.getSiteSubjectNumber()
        );
        System.out.println("Gender: " + form.getGender());
        System.out.println("Birth Date: " + form.getBirthDateForDisplay());

        if (form.getProjectData() != null) {
            System.out.println("\n--- ProjectData ---");
            System.out.println(
                "ARVcenterName: " + form.getProjectData().getARVcenterName()
            );
            System.out.println(
                "ARVcenterCode: " + form.getProjectData().getARVcenterCode()
            );
            System.out.println(
                "underInvestigationNote: " +
                    form.getProjectData().getUnderInvestigationNote()
            );
            System.out.println(
                "edtaTubeTaken: " + form.getProjectData().getEdtaTubeTaken()
            );
            System.out.println(
                "dbsvlTaken: " + form.getProjectData().isDbsvlTaken()
            );
            System.out.println(
                "pscvlTaken: " + form.getProjectData().isPscvlTaken()
            );
            System.out.println(
                "viralLoadTest: " + form.getProjectData().getViralLoadTest()
            );
            System.out.println(
                "cd4cd8Test: " + form.getProjectData().getCd4cd8Test()
            );
            System.out.println(
                "cd4CountTest: " + form.getProjectData().getCd4CountTest()
            );
            System.out.println(
                "cd3CountTest: " + form.getProjectData().getCd3CountTest()
            );
            System.out.println(
                "glycemiaTest: " + form.getProjectData().getGlycemiaTest()
            );
            System.out.println(
                "creatinineTest: " + form.getProjectData().getCreatinineTest()
            );
            System.out.println(
                "transaminaseTest: " +
                    form.getProjectData().getTransaminaseTest()
            );
            System.out.println(
                "nfsTest: " + form.getProjectData().getNfsTest()
            );
        } else {
            System.out.println("\n--- ProjectData: NULL ---");
        }

        if (form.getObservations() != null) {
            System.out.println("\n--- Observations ---");
            System.out.println(
                "nameOfDoctor: " + form.getObservations().getNameOfDoctor()
            );
            System.out.println(
                "nameOfSampler: " + form.getObservations().getNameOfSampler()
            );
            System.out.println(
                "hivStatus: " + form.getObservations().getHivStatus()
            );
            System.out.println(
                "underInvestigation: " +
                    form.getObservations().getUnderInvestigation()
            );
            System.out.println(
                "vlPregnancy: " + form.getObservations().getVlPregnancy()
            );
            System.out.println(
                "vlSuckle: " + form.getObservations().getVlSuckle()
            );
            System.out.println(
                "currentARVTreatment: " +
                    form.getObservations().getCurrentARVTreatment()
            );
            System.out.println(
                "arvTreatmentInitDate: " +
                    form.getObservations().getArvTreatmentInitDate()
            );
            System.out.println(
                "arvTreatmentRegime: " +
                    form.getObservations().getArvTreatmentRegime()
            );
            System.out.println(
                "currentARVTreatmentINNs: " +
                    form.getObservations().getCurrentARVTreatmentINNsList()
            );
            System.out.println(
                "vlReasonForRequest: " +
                    form.getObservations().getVlReasonForRequest()
            );
            System.out.println(
                "vlOtherReasonForRequest: " +
                    form.getObservations().getVlOtherReasonForRequest()
            );
            System.out.println(
                "initcd4Count: " + form.getObservations().getInitcd4Count()
            );
            System.out.println(
                "initcd4Percent: " + form.getObservations().getInitcd4Percent()
            );
            System.out.println(
                "initcd4Date: " + form.getObservations().getInitcd4Date()
            );
            System.out.println(
                "demandcd4Count: " + form.getObservations().getDemandcd4Count()
            );
            System.out.println(
                "demandcd4Percent: " +
                    form.getObservations().getDemandcd4Percent()
            );
            System.out.println(
                "demandcd4Date: " + form.getObservations().getDemandcd4Date()
            );
            System.out.println(
                "vlBenefit: " + form.getObservations().getVlBenefit()
            );
            System.out.println(
                "priorVLLab: " + form.getObservations().getPriorVLLab()
            );
            System.out.println(
                "priorVLValue: " + form.getObservations().getPriorVLValue()
            );
            System.out.println(
                "priorVLDate: " + form.getObservations().getPriorVLDate()
            );
        } else {
            System.out.println("\n--- Observations: NULL ---");
        }
        System.out.println("==========================================\n");
        // ========== END DEBUG LOGGING ==========

        try {
            Map<String, Object> response = new HashMap<>();

            // Validate type parameter
            if (!("initial".equals(type) || "verify".equals(type))) {
                LogEvent.logWarn(
                    this.getClass().getSimpleName(),
                    "postSampleEntryByProject",
                    "Invalid type parameter: " + type
                );

                response.put("success", false);
                response.put(
                    "message",
                    "Invalid type parameter. Must be 'initial' or 'verify'."
                );
                return ResponseEntity.badRequest().body(response);
            }

            if (result.hasErrors()) {
                LogEvent.logWarn(
                    this.getClass().getSimpleName(),
                    "postSampleEntryByProject",
                    "Form validation errors: " + result.getAllErrors().size()
                );

                response.put("success", false);
                response.put("errors", result.getAllErrors());
                return ResponseEntity.badRequest().body(response);
            }

            // Implement save logic using existing services
            String forward = null;

            if ("verify".equals(type)) {
                // Handle double entry / verification
                ISampleSecondEntry sampleSecondEntry = SpringContext.getBean(
                    ISampleSecondEntry.class
                );
                sampleSecondEntry.setFieldsFromForm(form);
                sampleSecondEntry.setSysUserId(getSysUserId(request));
                sampleSecondEntry.setRequest(request);
                if (sampleSecondEntry.canAccession()) {
                    forward = handleSave(request, sampleSecondEntry, form);
                }
            } else {
                // Handle initial entry - try different entry types
                ISampleSecondEntry sampleSecondEntry = SpringContext.getBean(
                    ISampleSecondEntry.class
                );
                sampleSecondEntry.setFieldsFromForm(form);
                sampleSecondEntry.setSysUserId(getSysUserId(request));
                sampleSecondEntry.setRequest(request);
                if (sampleSecondEntry.canAccession()) {
                    forward = handleSave(request, sampleSecondEntry, form);
                } else {
                    ISampleEntry sampleEntry = SpringContext.getBean(
                        ISampleEntry.class
                    );
                    sampleEntry.setFieldsFromForm(form);
                    sampleEntry.setSysUserId(getSysUserId(request));
                    sampleEntry.setRequest(request);
                    if (sampleEntry.canAccession()) {
                        forward = handleSave(request, sampleEntry, form);
                    } else {
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
                            forward = handleSave(
                                request,
                                sampleEntryAfterPatientEntry,
                                form
                            );
                        }
                    }
                }
            }

            if (forward != null && forward.contains("success")) {
                LogEvent.logInfo(
                    this.getClass().getSimpleName(),
                    "postSampleEntryByProject",
                    "Sample saved successfully. Lab Number: " + form.getLabNo()
                );

                // ========== DATABASE VERIFICATION ==========
                System.out.println(
                    "\n========== DATABASE SAVE VERIFICATION =========="
                );
                System.out.println("✓ Sample saved successfully!");
                System.out.println("✓ Lab Number: " + form.getLabNo());
                System.out.println("✓ Project: " + form.getProject());
                System.out.println("✓ Type: " + type);
                System.out.println("\nTo verify in database, run:");
                System.out.println(
                    "  SELECT * FROM sample WHERE accession_number = '" +
                        form.getLabNo() +
                        "';"
                );
                System.out.println(
                    "================================================\n"
                );
                // ========== END VERIFICATION ==========

                response.put("success", true);
                response.put("message", "Sample saved successfully");
                response.put("labNumber", form.getLabNo());
                response.put("type", type);
                return ResponseEntity.ok(response);
            } else {
                LogEvent.logError(
                    this.getClass().getSimpleName(),
                    "postSampleEntryByProject",
                    "Failed to save sample - no valid accessioner found"
                );

                response.put("success", false);
                response.put(
                    "message",
                    "Unable to save sample - validation failed or no valid entry type found"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    response
                );
            }
        } catch (Exception e) {
            LogEvent.logError(
                this.getClass().getSimpleName(),
                "postSampleEntryByProject",
                "Error processing POST request: " + e.getMessage()
            );
            LogEvent.logError(e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                errorResponse
            );
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

        LogEvent.logInfo(
            this.getClass().getSimpleName(),
            "setDisplayLists",
            "Display lists populated: genders=" +
                (genders != null ? genders.size() : 0) +
                ", organizationTypes=" +
                organizationTypeMapOfLists.size() +
                ", dictionaryLists=" +
                observationHistoryMapOfLists.size()
        );
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
