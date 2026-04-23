package org.openelisglobal.patient.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller that serves the Patient → Study → View (read-only) page.
 *
 * <p>
 * Single endpoint: GET /rest/patient-study-view?patientID={id}
 *
 * <p>
 * Returns: <br>
 * - referenceLists: all dropdown data needed by the 6 study sub-forms <br>
 * - formData: patient demographics + observations + project flags for the most
 * recent sample
 */
@Controller
@RequestMapping(value = "/rest/")
public class PatientStudyViewRestController extends BaseRestController {

    // ── Observation-history type names as stored in
    // observation_history_type.type_name ──
    // These match the strings used in Accessioner / BaseProjectFormMapper
    // throughout the codebase.
    private static final String OHT_PROJECT_FORM_NAME = "projectFormName";
    private static final String OHT_SUBJECT_NUMBER = "subjectNumber";
    private static final String OHT_SITE_SUBJECT_NUMBER = "siteSubjectNumber";
    private static final String OHT_UPID_CODE = "upidCode";
    private static final String OHT_HIV_STATUS = "hivStatus";
    private static final String OHT_EDUCATION_LEVEL = "educationLevel";
    private static final String OHT_MARITAL_STATUS = "maritalStatus";
    private static final String OHT_NATIONALITY = "nationality";
    private static final String OHT_NATIONALITY_OTHER = "nationalityOther";
    private static final String OHT_LEGAL_RESIDENCE = "legalResidence";
    private static final String OHT_NAME_OF_DOCTOR = "nameOfDoctor";
    private static final String OHT_NAME_OF_SAMPLER = "nameOfSampler";
    private static final String OHT_NAME_OF_REQUESTOR = "nameOfRequestor";
    private static final String OHT_ARV_PROPHYLAXIS_BENEFIT = "arvProphylaxisBenefit";
    private static final String OHT_ARV_PROPHYLAXIS = "arvProphylaxis";
    private static final String OHT_CURRENT_ARV_TREATMENT = "currentARVTreatment";
    private static final String OHT_PRIOR_ARV_TREATMENT = "priorARVTreatment";
    private static final String OHT_INTERRUPTED_ARV_TREATMENT = "interruptedARVTreatment";
    private static final String OHT_AIDS_STAGE = "aidsStage";
    private static final String OHT_ANY_PRIOR_DISEASES = "anyPriorDiseases";
    private static final String OHT_PRIOR_DISEASES = "priorDiseases";
    private static final String OHT_PRIOR_DISEASES_VALUE = "priorDiseasesValue";
    private static final String OHT_ANY_CURRENT_DISEASES = "anyCurrentDiseases";
    private static final String OHT_CURRENT_DISEASES = "currentDiseases";
    private static final String OHT_CURRENT_DISEASES_VALUE = "currentDiseasesValue";
    private static final String OHT_CURRENT_OI_TREATMENT = "currentOITreatment";
    private static final String OHT_COTRIMOXAZOLE_TREATMENT = "cotrimoxazoleTreatment";
    private static final String OHT_PATIENT_WEIGHT = "patientWeight";
    private static final String OHT_KARNOFSKY_SCORE = "karnofskyScore";
    private static final String OHT_CD4_COUNT = "cd4Count";
    private static final String OHT_CD4_PERCENT = "cd4Percent";
    private static final String OHT_INIT_CD4_COUNT = "initcd4Count";
    private static final String OHT_INIT_CD4_PERCENT = "initcd4Percent";
    private static final String OHT_INIT_CD4_DATE = "initcd4Date";
    private static final String OHT_DEMAND_CD4_COUNT = "demandcd4Count";
    private static final String OHT_DEMAND_CD4_PERCENT = "demandcd4Percent";
    private static final String OHT_DEMAND_CD4_DATE = "demandcd4Date";
    private static final String OHT_PRIOR_CD4_DATE = "priorCd4Date";
    private static final String OHT_ANTI_TB_TREATMENT = "antiTbTreatment";
    private static final String OHT_ARV_TREATMENT_ANY_ADVERSE = "arvTreatmentAnyAdverseEffects";
    private static final String OHT_ARV_TREATMENT_CHANGE = "arvTreatmentChange";
    private static final String OHT_ARV_TREATMENT_NEW = "arvTreatmentNew";
    private static final String OHT_ARV_TREATMENT_REGIME = "arvTreatmentRegime";
    private static final String OHT_ARV_TREATMENT_INIT_DATE = "arvTreatmentInitDate";
    private static final String OHT_COTRI_ANY_ADVERSE = "cotrimoxazoleTreatmentAnyAdverseEffects";
    private static final String OHT_ANY_SECONDARY_TREATMENT = "anySecondaryTreatment";
    private static final String OHT_SECONDARY_TREATMENT = "secondaryTreatment";
    private static final String OHT_CLINIC_VISITS = "clinicVisits";
    private static final String OHT_UNDER_INVESTIGATION = "underInvestigation";
    private static final String OHT_UNDER_INVESTIGATION_NOTE = "underInvestigationNote";
    private static final String OHT_VL_PREGNANCY = "vlPregnancy";
    private static final String OHT_VL_SUCKLE = "vlSuckle";
    private static final String OHT_VL_REASON = "vlReasonForRequest";
    private static final String OHT_VL_OTHER_REASON = "vlOtherReasonForRequest";
    private static final String OHT_VL_BENEFIT = "vlBenefit";
    private static final String OHT_PRIOR_VL_LAB = "priorVLLab";
    private static final String OHT_PRIOR_VL_VALUE = "priorVLValue";
    private static final String OHT_PRIOR_VL_DATE = "priorVLDate";
    private static final String OHT_HOSPITAL_PATIENT = "hospitalPatient";
    private static final String OHT_SERVICE = "service";
    private static final String OHT_REASON = "reason";
    private static final String OHT_EID_TYPE_OF_CLINIC = "eidTypeOfClinic";
    private static final String OHT_EID_TYPE_OF_CLINIC_OTHER = "eidTypeOfClinicOther";
    private static final String OHT_EID_HOW_CHILD_FED = "eidHowChildFed";
    private static final String OHT_EID_STOPPED_BREASTFEEDING = "eidStoppedBreastfeeding";
    private static final String OHT_EID_INFANT_SYMPTOMATIC = "eidInfantSymptomatic";
    private static final String OHT_EID_MOTHERS_HIV_STATUS = "eidMothersHIVStatus";
    private static final String OHT_EID_MOTHERS_ARV = "eidMothersARV";
    private static final String OHT_EID_INFANTS_ARV = "eidInfantsARV";
    private static final String OHT_EID_INFANT_COTRIMOXAZOLE = "eidInfantCotrimoxazole";
    private static final String OHT_EID_INFANT_PTME = "eidInfantPTME";
    private static final String OHT_WHICH_PCR = "whichPCR";
    private static final String OHT_REASON_SECOND_PCR = "reasonForSecondPCRTest";
    private static final String OHT_CENTER_CODE = "centerCode";
    private static final String OHT_CENTER_NAME = "centerName";
    private static final String OHT_INTERVIEW_DATE = "interviewDate";
    private static final String OHT_INTERVIEW_TIME = "interviewTime";
    // ProjectData boolean flags
    private static final String OHT_EDTA_TUBE_TAKEN = "edtaTubeTaken";
    private static final String OHT_DBS_TAKEN = "dbsTaken";
    private static final String OHT_DBS_VL_TAKEN = "dbsvlTaken";
    private static final String OHT_PSC_VL_TAKEN = "pscvlTaken";
    private static final String OHT_VIRAL_LOAD_TEST = "viralLoadTest";
    private static final String OHT_ASANTE_TEST = "asanteTest";
    private static final String OHT_PLASMA_TAKEN = "plasmaTaken";
    private static final String OHT_SERUM_TAKEN = "serumTaken";
    private static final String OHT_EID_SITE_NAME = "EIDsiteName";
    private static final String OHT_EID_SITE_CODE = "EIDsiteCode";
    private static final String OHT_ARV_CENTER_CODE = "ARVcenterCode";
    private static final String OHT_ARV_CENTER_NAME = "ARVcenterName";

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    // ─────────────────────────────────────────────────────────────────────────────
    // Endpoint
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns all data needed to render the Patient Study View page.
     *
     * <p>
     * When patientID is omitted only the referenceLists are returned so the UI can
     * render empty dropdowns before any patient is selected.
     */
    @GetMapping(value = "patient-study-view", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPatientStudyView(HttpServletRequest request,
            @RequestParam(required = false) String patientID) {
        Map<String, Object> response = new HashMap<>();
        response.put("referenceLists", buildReferenceLists());

        if (GenericValidator.isBlankOrNull(patientID)) {
            response.put("formData", new HashMap<>());
            return ResponseEntity.ok(response);
        }

        try {
            Patient patient = patientService.getData(patientID);
            if (patient == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getPatientStudyView",
                        "Patient not found for ID: " + patientID);
                response.put("formData", new HashMap<>());
                response.put("error", "Patient not found");
                return ResponseEntity.ok(response);
            }

            personService.getData(patient.getPerson());
            Person person = patient.getPerson();

            // Find the most recent sample for this patient
            List<Sample> samples = sampleHumanService.getSamplesForPatient(patientID);
            Sample mostRecentSample = null;
            if (samples != null && !samples.isEmpty()) {
                mostRecentSample = samples.get(samples.size() - 1);
            }

            // Build a typeId → value map from all ObservationHistory rows for this sample
            Map<String, String> ohByTypeId = new HashMap<>();
            if (mostRecentSample != null) {
                List<ObservationHistory> ohs = observationHistoryService
                        .getObservationHistoriesBySampleId(mostRecentSample.getId());
                if (ohs != null) {
                    for (ObservationHistory oh : ohs) {
                        if (oh.getObservationHistoryTypeId() != null && oh.getValue() != null) {
                            ohByTypeId.put(oh.getObservationHistoryTypeId(), oh.getValue());
                        }
                    }
                }
            }

            // Collect all unique projectFormName values across every sample for this
            // patient
            List<String> availableStudyTypes = new ArrayList<>();
            if (samples != null) {
                ObservationHistoryType pfnType = observationHistoryTypeService.getByName(OHT_PROJECT_FORM_NAME);
                if (pfnType != null) {
                    for (Sample sample : samples) {
                        List<ObservationHistory> sampleOhs = observationHistoryService
                                .getObservationHistoriesBySampleId(sample.getId());
                        if (sampleOhs != null) {
                            for (ObservationHistory oh : sampleOhs) {
                                if (pfnType.getId().equals(oh.getObservationHistoryTypeId()) && oh.getValue() != null
                                        && !oh.getValue().isEmpty() && !availableStudyTypes.contains(oh.getValue())) {
                                    availableStudyTypes.add(oh.getValue());
                                }
                            }
                        }
                    }
                }
            }
            Map<String, Object> formData = new HashMap<>();

            // ── Demographics ──────────────────────────────────────────────────────────
            formData.put("patientPK", nvl(patient.getId()));
            formData.put("firstName", nvl(person.getFirstName()));
            formData.put("lastName", nvl(person.getLastName()));
            formData.put("gender", nvl(patient.getGender()));
            formData.put("birthDateForDisplay", nvl(patient.getBirthDateForDisplay()));
            formData.put("nationalId", nvl(patient.getNationalId()));

            // ── Sample ────────────────────────────────────────────────────────────────
            if (mostRecentSample != null) {
                formData.put("samplePK", nvl(mostRecentSample.getId()));
                formData.put("labNo", nvl(mostRecentSample.getAccessionNumber()));
                formData.put("receivedDateForDisplay", nvl(mostRecentSample.getReceivedDateForDisplay()));
                formData.put("receivedTimeForDisplay", nvl(mostRecentSample.getReceivedTimeForDisplay()));
            } else {
                formData.put("samplePK", "");
                formData.put("labNo", "");
                formData.put("receivedDateForDisplay", "");
                formData.put("receivedTimeForDisplay", "");
            }

            // ── Age (calculated from date of birth) ──────────────────────────────────
            if (patient.getBirthDate() != null) {
                int age = java.time.Period.between(
                        patient.getBirthDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        java.time.LocalDate.now()).getYears();
                formData.put("age", String.valueOf(age));
            } else {
                formData.put("age", "");
            }

            // ── Observation history fields (keyed by type name) ───────────────────────
            formData.put("subjectNumber", getOHValue(ohByTypeId, OHT_SUBJECT_NUMBER));
            formData.put("siteSubjectNumber", getOHValue(ohByTypeId, OHT_SITE_SUBJECT_NUMBER));
            formData.put("upidCode", getOHValue(ohByTypeId, OHT_UPID_CODE));
            // centerCode/centerName: prefer generic OH value; fall back to ARV-specific
            // keys
            String centerCode = getOHValue(ohByTypeId, OHT_CENTER_CODE);
            if (centerCode.isEmpty()) {
                centerCode = getOHValue(ohByTypeId, OHT_ARV_CENTER_CODE);
            }
            formData.put("centerCode", centerCode);
            String centerName = getOHValue(ohByTypeId, OHT_CENTER_NAME);
            if (centerName.isEmpty()) {
                centerName = getOHValue(ohByTypeId, OHT_ARV_CENTER_NAME);
            }
            formData.put("centerName", centerName);
            formData.put("interviewDate", getOHValue(ohByTypeId, OHT_INTERVIEW_DATE));
            formData.put("interviewTime", getOHValue(ohByTypeId, OHT_INTERVIEW_TIME));

            // ── Observations map (mirrors the JSP form.observations.* bindings) ───────
            Map<String, Object> observations = new HashMap<>();
            observations.put("projectFormName", getOHValue(ohByTypeId, OHT_PROJECT_FORM_NAME));
            observations.put("hivStatus", getOHValue(ohByTypeId, OHT_HIV_STATUS));
            observations.put("educationLevel", getOHValue(ohByTypeId, OHT_EDUCATION_LEVEL));
            observations.put("maritalStatus", getOHValue(ohByTypeId, OHT_MARITAL_STATUS));
            observations.put("nationality", getOHValue(ohByTypeId, OHT_NATIONALITY));
            observations.put("nationalityOther", getOHValue(ohByTypeId, OHT_NATIONALITY_OTHER));
            observations.put("legalResidence", getOHValue(ohByTypeId, OHT_LEGAL_RESIDENCE));
            observations.put("nameOfDoctor", getOHValue(ohByTypeId, OHT_NAME_OF_DOCTOR));
            observations.put("nameOfSampler", getOHValue(ohByTypeId, OHT_NAME_OF_SAMPLER));
            observations.put("nameOfRequestor", getOHValue(ohByTypeId, OHT_NAME_OF_REQUESTOR));
            observations.put("arvProphylaxisBenefit", getOHValue(ohByTypeId, OHT_ARV_PROPHYLAXIS_BENEFIT));
            observations.put("arvProphylaxis", getOHValue(ohByTypeId, OHT_ARV_PROPHYLAXIS));
            observations.put("currentARVTreatment", getOHValue(ohByTypeId, OHT_CURRENT_ARV_TREATMENT));
            observations.put("priorARVTreatment", getOHValue(ohByTypeId, OHT_PRIOR_ARV_TREATMENT));
            observations.put("interruptedARVTreatment", getOHValue(ohByTypeId, OHT_INTERRUPTED_ARV_TREATMENT));
            observations.put("aidsStage", getOHValue(ohByTypeId, OHT_AIDS_STAGE));
            observations.put("anyPriorDiseases", getOHValue(ohByTypeId, OHT_ANY_PRIOR_DISEASES));
            observations.put("priorDiseases", getOHValue(ohByTypeId, OHT_PRIOR_DISEASES));
            observations.put("priorDiseasesValue", getOHValue(ohByTypeId, OHT_PRIOR_DISEASES_VALUE));
            observations.put("anyCurrentDiseases", getOHValue(ohByTypeId, OHT_ANY_CURRENT_DISEASES));
            observations.put("currentDiseases", getOHValue(ohByTypeId, OHT_CURRENT_DISEASES));
            observations.put("currentDiseasesValue", getOHValue(ohByTypeId, OHT_CURRENT_DISEASES_VALUE));
            observations.put("currentOITreatment", getOHValue(ohByTypeId, OHT_CURRENT_OI_TREATMENT));
            observations.put("cotrimoxazoleTreatment", getOHValue(ohByTypeId, OHT_COTRIMOXAZOLE_TREATMENT));
            observations.put("patientWeight", getOHValue(ohByTypeId, OHT_PATIENT_WEIGHT));
            observations.put("karnofskyScore", getOHValue(ohByTypeId, OHT_KARNOFSKY_SCORE));
            observations.put("cd4Count", getOHValue(ohByTypeId, OHT_CD4_COUNT));
            observations.put("cd4Percent", getOHValue(ohByTypeId, OHT_CD4_PERCENT));
            observations.put("initcd4Count", getOHValue(ohByTypeId, OHT_INIT_CD4_COUNT));
            observations.put("initcd4Percent", getOHValue(ohByTypeId, OHT_INIT_CD4_PERCENT));
            observations.put("initcd4Date", getOHValue(ohByTypeId, OHT_INIT_CD4_DATE));
            observations.put("demandcd4Count", getOHValue(ohByTypeId, OHT_DEMAND_CD4_COUNT));
            observations.put("demandcd4Percent", getOHValue(ohByTypeId, OHT_DEMAND_CD4_PERCENT));
            observations.put("demandcd4Date", getOHValue(ohByTypeId, OHT_DEMAND_CD4_DATE));
            observations.put("priorCd4Date", getOHValue(ohByTypeId, OHT_PRIOR_CD4_DATE));
            observations.put("antiTbTreatment", getOHValue(ohByTypeId, OHT_ANTI_TB_TREATMENT));
            observations.put("arvTreatmentAnyAdverseEffects", getOHValue(ohByTypeId, OHT_ARV_TREATMENT_ANY_ADVERSE));
            observations.put("arvTreatmentChange", getOHValue(ohByTypeId, OHT_ARV_TREATMENT_CHANGE));
            observations.put("arvTreatmentNew", getOHValue(ohByTypeId, OHT_ARV_TREATMENT_NEW));
            observations.put("arvTreatmentRegime", getOHValue(ohByTypeId, OHT_ARV_TREATMENT_REGIME));
            observations.put("arvTreatmentInitDate", getOHValue(ohByTypeId, OHT_ARV_TREATMENT_INIT_DATE));
            observations.put("cotrimoxazoleTreatmentAnyAdverseEffects", getOHValue(ohByTypeId, OHT_COTRI_ANY_ADVERSE));
            observations.put("anySecondaryTreatment", getOHValue(ohByTypeId, OHT_ANY_SECONDARY_TREATMENT));
            observations.put("secondaryTreatment", getOHValue(ohByTypeId, OHT_SECONDARY_TREATMENT));
            observations.put("clinicVisits", getOHValue(ohByTypeId, OHT_CLINIC_VISITS));
            observations.put("underInvestigation", getOHValue(ohByTypeId, OHT_UNDER_INVESTIGATION));
            // priorARVTreatmentINNsList: up to 4 free-text INN entries stored per index
            List<String> priorARVTreatmentINNsList = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                priorARVTreatmentINNsList.add(getOHValue(ohByTypeId, "priorARVTreatmentINNs" + i));
            }
            observations.put("priorARVTreatmentINNsList", priorARVTreatmentINNsList);
            // futureARVTreatmentINNsList: up to 4 prescribed ARV INN entries
            List<String> futureARVTreatmentINNsList = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                futureARVTreatmentINNsList.add(getOHValue(ohByTypeId, "futureARVTreatmentINNs" + i));
            }
            observations.put("futureARVTreatmentINNsList", futureARVTreatmentINNsList);
            observations.put("vlPregnancy", getOHValue(ohByTypeId, OHT_VL_PREGNANCY));
            observations.put("vlSuckle", getOHValue(ohByTypeId, OHT_VL_SUCKLE));
            observations.put("vlReasonForRequest", getOHValue(ohByTypeId, OHT_VL_REASON));
            observations.put("vlOtherReasonForRequest", getOHValue(ohByTypeId, OHT_VL_OTHER_REASON));
            observations.put("vlBenefit", getOHValue(ohByTypeId, OHT_VL_BENEFIT));
            observations.put("priorVLLab", getOHValue(ohByTypeId, OHT_PRIOR_VL_LAB));
            observations.put("priorVLValue", getOHValue(ohByTypeId, OHT_PRIOR_VL_VALUE));
            observations.put("priorVLDate", getOHValue(ohByTypeId, OHT_PRIOR_VL_DATE));
            observations.put("hospitalPatient", getOHValue(ohByTypeId, OHT_HOSPITAL_PATIENT));
            observations.put("service", getOHValue(ohByTypeId, OHT_SERVICE));
            observations.put("reason", getOHValue(ohByTypeId, OHT_REASON));
            observations.put("eidTypeOfClinic", getOHValue(ohByTypeId, OHT_EID_TYPE_OF_CLINIC));
            observations.put("eidTypeOfClinicOther", getOHValue(ohByTypeId, OHT_EID_TYPE_OF_CLINIC_OTHER));
            observations.put("eidHowChildFed", getOHValue(ohByTypeId, OHT_EID_HOW_CHILD_FED));
            observations.put("eidStoppedBreastfeeding", getOHValue(ohByTypeId, OHT_EID_STOPPED_BREASTFEEDING));
            observations.put("eidInfantSymptomatic", getOHValue(ohByTypeId, OHT_EID_INFANT_SYMPTOMATIC));
            observations.put("eidMothersHIVStatus", getOHValue(ohByTypeId, OHT_EID_MOTHERS_HIV_STATUS));
            observations.put("eidMothersARV", getOHValue(ohByTypeId, OHT_EID_MOTHERS_ARV));
            observations.put("eidInfantsARV", getOHValue(ohByTypeId, OHT_EID_INFANTS_ARV));
            observations.put("eidInfantCotrimoxazole", getOHValue(ohByTypeId, OHT_EID_INFANT_COTRIMOXAZOLE));
            observations.put("eidInfantPTME", getOHValue(ohByTypeId, OHT_EID_INFANT_PTME));
            observations.put("whichPCR", getOHValue(ohByTypeId, OHT_WHICH_PCR));
            observations.put("reasonForSecondPCRTest", getOHValue(ohByTypeId, OHT_REASON_SECOND_PCR));
            formData.put("observations", observations);

            // ── Project data (sample-level boolean flags + org fields) ────────────────
            Map<String, Object> projectData = new HashMap<>();
            projectData.put("edtaTubeTaken", isTruthy(getOHValue(ohByTypeId, OHT_EDTA_TUBE_TAKEN)));
            projectData.put("dbsTaken", isTruthy(getOHValue(ohByTypeId, OHT_DBS_TAKEN)));
            projectData.put("dbsvlTaken", isTruthy(getOHValue(ohByTypeId, OHT_DBS_VL_TAKEN)));
            projectData.put("pscvlTaken", isTruthy(getOHValue(ohByTypeId, OHT_PSC_VL_TAKEN)));
            projectData.put("viralLoadTest", isTruthy(getOHValue(ohByTypeId, OHT_VIRAL_LOAD_TEST)));
            projectData.put("asanteTest", isTruthy(getOHValue(ohByTypeId, OHT_ASANTE_TEST)));
            projectData.put("plasmaTaken", isTruthy(getOHValue(ohByTypeId, OHT_PLASMA_TAKEN)));
            projectData.put("serumTaken", isTruthy(getOHValue(ohByTypeId, OHT_SERUM_TAKEN)));
            projectData.put("underInvestigationNote", getOHValue(ohByTypeId, OHT_UNDER_INVESTIGATION_NOTE));
            projectData.put("EIDsiteName", getOHValue(ohByTypeId, OHT_EID_SITE_NAME));
            projectData.put("EIDsiteCode", getOHValue(ohByTypeId, OHT_EID_SITE_CODE));
            projectData.put("ARVcenterCode", getOHValue(ohByTypeId, OHT_ARV_CENTER_CODE));
            projectData.put("ARVcenterName", getOHValue(ohByTypeId, OHT_ARV_CENTER_NAME));
            formData.put("projectData", projectData);
            formData.put("availableStudyTypes", availableStudyTypes);

            response.put("formData", formData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "getPatientStudyView",
                    "Error loading patient study view for patientID=" + patientID + ": " + e.getMessage());
            LogEvent.logError(e);
            response.put("formData", new HashMap<>());
            response.put("error", "Error loading patient data");
            return ResponseEntity.status(500).body(response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Reference list builder
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Assembles every dropdown/reference list the 6 study sub-forms need into one
     * map returned alongside the form data. The frontend loads this once and
     * populates all selects from it.
     */
    private Map<String, Object> buildReferenceLists() {
        Map<String, Object> lists = new HashMap<>();

        lists.put("genders", DisplayListService.getInstance().getFreshList(ListType.GENDERS));
        lists.put("educationLevels", dictionaryListToIdValuePairs(ObservationHistoryList.EDUCATION_LEVELS));
        lists.put("maritalStatuses", dictionaryListToIdValuePairs(ObservationHistoryList.MARITAL_STATUSES));
        lists.put("nationalities", dictionaryListToIdValuePairs(ObservationHistoryList.SIMPLIFIED_NATIONALITIES));
        lists.put("nationalitiesFull", dictionaryListToIdValuePairs(ObservationHistoryList.NATIONALITIES));
        lists.put("hivTypes", dictionaryListToIdValuePairs(ObservationHistoryList.HIV_TYPES));
        lists.put("hivStatuses", dictionaryListToIdValuePairs(ObservationHistoryList.HIV_STATUSES));
        lists.put("yesNo", dictionaryListToIdValuePairs(ObservationHistoryList.YES_NO));
        lists.put("yesNoUnknownNaNotSpec",
                dictionaryListToIdValuePairs(ObservationHistoryList.YES_NO_UNKNOWN_NA_NOTSPEC));
        lists.put("arvRegimes", dictionaryListToIdValuePairs(ObservationHistoryList.ARV_REGIME));
        lists.put("arvProphylaxis", dictionaryListToIdValuePairs(ObservationHistoryList.ARV_PROPHYLAXIS));
        lists.put("aidsStages", dictionaryListToIdValuePairs(ObservationHistoryList.AIDS_STAGES));
        lists.put("arvOrgs", organizationListToMap(OrganizationTypeList.ARV_ORGS));
        lists.put("arvOrgsByName", organizationListToMap(OrganizationTypeList.ARV_ORGS_BY_NAME));
        lists.put("rtnHospitals", organizationListToMap(OrganizationTypeList.RTN_HOSPITALS));
        lists.put("rtnServices", organizationListToMap(OrganizationTypeList.RTN_SERVICES));

        // Disease lists used by ARV Initial / Followup ARV / RTN sub-forms
        org.openelisglobal.patient.valueholder.ObservationData tmpObs = new org.openelisglobal.patient.valueholder.ObservationData();
        lists.put("priorDiseasesList", diseasePairsToList(tmpObs.getPriorDiseasesList()));
        lists.put("currentDiseasesList", diseasePairsToList(tmpObs.getCurrentDiseasesList()));
        lists.put("rtnPriorDiseasesList", diseasePairsToList(tmpObs.getRtnPriorDiseasesList()));
        lists.put("rtnCurrentDiseasesList", diseasePairsToList(tmpObs.getRtnCurrentDiseasesList()));
        lists.put("yesNoUnknown", dictionaryListToIdValuePairs(ObservationHistoryList.YES_NO_UNKNOWN));
        lists.put("yesNoNa", dictionaryListToIdValuePairs(ObservationHistoryList.YES_NO_NA));
        lists.put("arvProphylaxis2", dictionaryListToIdValuePairs(ObservationHistoryList.ARV_PROPHYLAXIS_2));
        lists.put("arvReasonForVlDemand",
                dictionaryListToIdValuePairs(ObservationHistoryList.ARV_REASON_FOR_VL_DEMAND));
        lists.put("eidWhichPcr", dictionaryListToIdValuePairs(ObservationHistoryList.EID_WHICH_PCR));
        lists.put("eidSecondPcrReason", dictionaryListToIdValuePairs(ObservationHistoryList.EID_SECOND_PCR_REASON));
        lists.put("eidTypeOfClinic", dictionaryListToIdValuePairs(ObservationHistoryList.EID_TYPE_OF_CLINIC));
        lists.put("eidHowChildFed", dictionaryListToIdValuePairs(ObservationHistoryList.EID_HOW_CHILD_FED));
        lists.put("eidStoppedBreastfeeding",
                dictionaryListToIdValuePairs(ObservationHistoryList.EID_STOPPED_BREASTFEEDING));
        lists.put("eidMothersHivStatus", dictionaryListToIdValuePairs(ObservationHistoryList.EID_MOTHERS_HIV_STATUS));
        lists.put("eidMothersArvTreatment",
                dictionaryListToIdValuePairs(ObservationHistoryList.EID_MOTHERS_ARV_TREATMENT));
        lists.put("eidInfantProphylaxisArv",
                dictionaryListToIdValuePairs(ObservationHistoryList.EID_INFANT_PROPHYLAXIS_ARV));
        lists.put("eidOrgs", organizationListToMap(OrganizationTypeList.EID_ORGS));
        lists.put("eidOrgsByName", organizationListToMap(OrganizationTypeList.EID_ORGS_BY_NAME));

        return lists;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Observation history lookup helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the database typeId for the given type name, then looks it up in the
     * pre-built typeId→value map. Returns empty string if not found.
     */
    private String getOHValue(Map<String, String> ohByTypeId, String typeName) {
        try {
            ObservationHistoryType oht = observationHistoryTypeService.getByName(typeName);
            if (oht == null) {
                return "";
            }
            String val = ohByTypeId.get(oht.getId());
            return val != null ? val : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Conversion helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private List<IdValuePair> dictionaryListToIdValuePairs(ObservationHistoryList ohl) {
        List<IdValuePair> result = new ArrayList<>();
        try {
            List<Dictionary> dictList = ohl.getList();
            if (dictList != null) {
                for (Dictionary d : dictList) {
                    String label = d.getLocalizedName() != null ? d.getLocalizedName() : d.getDictEntry();
                    result.add(new IdValuePair(d.getId(), label));
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "dictionaryListToIdValuePairs",
                    "Could not load ObservationHistoryList." + ohl.name() + ": " + e.getMessage());
        }
        return result;
    }

    private List<Map<String, String>> organizationListToMap(OrganizationTypeList otl) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            List<Organization> orgs = otl.getList();
            if (orgs != null) {
                for (Organization org : orgs) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("id", nvl(org.getId()));
                    entry.put("value", nvl(org.getDoubleName()));
                    entry.put("organizationName", nvl(org.getOrganizationName()));
                    entry.put("shortName", nvl(org.getShortName()));
                    result.add(entry);
                }
            }
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "organizationListToMap",
                    "Could not load OrganizationTypeList." + otl.name() + ": " + e.getMessage());
        }
        return result;
    }

    private List<Map<String, String>> diseasePairsToList(
            List<org.apache.commons.lang3.tuple.Pair<String, String>> pairs) {
        List<Map<String, String>> result = new ArrayList<>();
        if (pairs == null) {
            return result;
        }
        for (org.apache.commons.lang3.tuple.Pair<String, String> pair : pairs) {
            Map<String, String> entry = new HashMap<>();
            entry.put("name", nvl(pair.getKey()));
            entry.put("label", nvl(pair.getValue()));
            result.add(entry);
        }
        return result;
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private boolean isTruthy(String val) {
        return ("true".equalsIgnoreCase(val) || "Y".equalsIgnoreCase(val) || "1".equals(val));
    }

}
