package org.openelisglobal.patient.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.form.PatientEditByProjectForm;
import org.openelisglobal.patient.saving.IPatientEditUpdate;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST controller that serves the Patient → Study → Edit (read-write) page.
 *
 * <p>
 * Single endpoint: POST /rest/patient-study-edit
 *
 * <p>
 * Accepts a JSON payload assembled by the React {@code PatientStudyEditForm}
 * component, converts it into a {@link PatientEditByProjectForm}, and delegates
 * persistence to the existing {@link PatientEditUpdate} accessioner — the same
 * path used by the legacy JSP {@code PatientEditByProjectController} via
 * {@code IPatientEditUpdate}.
 *
 * <p>
 * Responsibilities of this controller are intentionally narrow:
 * <ul>
 * <li>Request/response mapping (JSON ↔ form)</li>
 * <li>Delegating to the accessioner layer</li>
 * <li>Returning a uniform success/error JSON envelope</li>
 * </ul>
 * No business logic, no DAO calls, no {@code @Transactional} — all of that
 * lives inside the accessioner and its downstream services.
 */
@Controller
@RequestMapping(value = "/rest/")
public class PatientStudyEditRestController extends BaseRestController {

    // ─────────────────────────────────────────────────────────────────────────
    // Save endpoint
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists an edited patient study record submitted from the React
     * PatientStudyEdit page.
     *
     * <p>
     * The JSON body mirrors the {@code handleSave()} payload assembled in
     * {@code PatientStudyEditForm.js}:
     * 
     * <pre>
     * {
     *   "patientPK":            "...",
     *   "samplePK":             "...",
     *   "projectFormName":      "InitialARV_Id",
     *   "lastName":             "...",
     *   "firstName":            "...",
     *   "gender":               "...",
     *   "birthDateForDisplay":  "dd/MM/yyyy",
     *   "nationalId":           "...",
     *   "subjectNumber":        "...",
     *   "siteSubjectNumber":    "...",
     *   "labNo":                "...",
     *   "receivedDateForDisplay":"...",
     *   "interviewDate":        "...",
     *   "centerCode":           "...",
     *   "centerName":           "...",
     *   "observations":         { ... },
     *   "projectData":          { ... }
     * }
     * </pre>
     *
     * <p>
     * Returns {@code {"success": true}} on success or {@code {"success": false,
     * "error": "..."}} on failure.
     */
    @PostMapping(value = "patient-study-edit", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> savePatientStudyEdit(HttpServletRequest request,
            @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String sysUserId = getSysUserId(request);
            PatientEditByProjectForm form = buildFormFromPayload(payload);

            // PatientEditUpdate.canAccession() inspects request.getParameter("type")
            // and requires the value "READWRITE". We wrap the incoming request so
            // callers do not need to supply this parameter explicitly.
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getParameter(String name) {
                    if ("type".equals(name)) {
                        return "READWRITE";
                    }
                    return super.getParameter(name);
                }
            };

            // Obtain via Spring context so that @Autowired fields on Accessioner
            // (in particular the prototype-scoped Errors messages bean) are injected.
            // Using plain new PatientEditUpdate(...) bypasses Spring and leaves
            // messages null, causing an NPE inside Accessioner.findSample().
            IPatientEditUpdate accessioner = SpringContext.getBean(IPatientEditUpdate.class);
            accessioner.setFieldsFromForm(form);
            accessioner.setSysUserId(sysUserId);
            accessioner.setRequest(wrappedRequest);

            if (accessioner.canAccession()) {
                accessioner.save();
                response.put("success", true);
            } else {
                response.put("success", false);
                response.put("error", "Cannot accession this record – access type not permitted");
            }
        } catch (Throwable t) {
            LogEvent.logError(this.getClass().getSimpleName(), "savePatientStudyEdit",
                    "Error saving patient study edit: " + t.getMessage());
            LogEvent.logError(t instanceof Exception ? (Exception) t : new RuntimeException(t));
            response.put("success", false);
            response.put("error", t.getMessage() != null ? t.getMessage() : "Unexpected server error");
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Payload → form conversion
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts the raw JSON payload map sent by the React form into a fully
     * populated {@link PatientEditByProjectForm} ready for the accessioner.
     *
     * <p>
     * Every field access uses null-safe helpers so that a missing key in the
     * payload leaves the corresponding form field at its default empty value rather
     * than throwing a {@link NullPointerException}.
     */
    @SuppressWarnings("unchecked")
    private PatientEditByProjectForm buildFormFromPayload(Map<String, Object> payload) {
        PatientEditByProjectForm form = new PatientEditByProjectForm();
        form.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);

        // ── Demographics ──────────────────────────────────────────────────────
        form.setPatientPK(asString(payload.get("patientPK")));
        form.setSamplePK(asString(payload.get("samplePK")));
        form.setLastName(asString(payload.get("lastName")));
        form.setFirstName(asString(payload.get("firstName")));
        form.setGender(asString(payload.get("gender")));
        form.setBirthDateForDisplay(asString(payload.get("birthDateForDisplay")));

        form.setSubjectNumber(asString(payload.get("subjectNumber")));
        form.setSiteSubjectNumber(asString(payload.get("siteSubjectNumber")));
        form.setLabNo(asString(payload.get("labNo")));
        form.setReceivedDateForDisplay(asString(payload.get("receivedDateForDisplay")));
        form.setReceivedTimeForDisplay(asString(payload.get("receivedTimeForDisplay")));
        form.setInterviewDate(asString(payload.get("interviewDate")));
        form.setInterviewTime(asString(payload.get("interviewTime")));
        form.setUpidCode(asString(payload.get("upidCode")));
        form.setCenterName(asString(payload.get("centerName")));

        // centerCode is stored as an Integer on the form
        String centerCodeRaw = asString(payload.get("centerCode"));
        if (!centerCodeRaw.isEmpty()) {
            try {
                form.setCenterCode(Integer.parseInt(centerCodeRaw));
            } catch (NumberFormatException ignored) {
                // Leave null – form validation will catch if the field is required
                // for the chosen study type.
            }
        }

        // ── Observations ──────────────────────────────────────────────────────
        Map<String, Object> obsMap = payload.get("observations") instanceof Map
                ? (Map<String, Object>) payload.get("observations")
                : new HashMap<>();

        ObservationData obs = new ObservationData();
        obs.setProjectFormName(asString(payload.get("projectFormName")));

        // Core demographics observations
        obs.setHivStatus(asString(obsMap.get("hivStatus")));
        obs.setEducationLevel(asString(obsMap.get("educationLevel")));
        obs.setMaritalStatus(asString(obsMap.get("maritalStatus")));
        obs.setNationality(asString(obsMap.get("nationality")));
        obs.setNationalityOther(asString(obsMap.get("nationalityOther")));
        obs.setLegalResidence(asString(obsMap.get("legalResidence")));
        obs.setNameOfDoctor(asString(obsMap.get("nameOfDoctor")));
        obs.setNameOfSampler(asString(obsMap.get("nameOfSampler")));
        obs.setNameOfRequestor(asString(obsMap.get("nameOfRequestor")));

        // ARV-specific observations
        obs.setArvProphylaxisBenefit(asString(obsMap.get("arvProphylaxisBenefit")));
        obs.setArvProphylaxis(asString(obsMap.get("arvProphylaxis")));
        obs.setCurrentARVTreatment(asString(obsMap.get("currentARVTreatment")));
        obs.setPriorARVTreatment(asString(obsMap.get("priorARVTreatment")));
        obs.setInterruptedARVTreatment(asString(obsMap.get("interruptedARVTreatment")));
        obs.setAidsStage(asString(obsMap.get("aidsStage")));
        obs.setArvTreatmentAnyAdverseEffects(asString(obsMap.get("arvTreatmentAnyAdverseEffects")));
        obs.setArvTreatmentChange(asString(obsMap.get("arvTreatmentChange")));
        obs.setArvTreatmentNew(asString(obsMap.get("arvTreatmentNew")));
        obs.setArvTreatmentRegime(asString(obsMap.get("arvTreatmentRegime")));
        obs.setArvTreatmentInitDate(asString(obsMap.get("arvTreatmentInitDate")));

        // Prior ARV treatment INN list (up to 4 indexed free-text entries)
        Object priorInnsRaw = obsMap.get("priorARVTreatmentINNsList");
        if (priorInnsRaw instanceof List) {
            List<?> priorInns = (List<?>) priorInnsRaw;
            for (int i = 0; i < 4; i++) {
                obs.setPriorARVTreatmentINNs(i, i < priorInns.size() ? asString(priorInns.get(i)) : "");
            }
        }

        // Current (ongoing) ARV treatment INN list (up to 4 indexed free-text entries)
        Object currentInnsRaw = obsMap.get("currentARVTreatmentINNsList");
        if (currentInnsRaw instanceof List) {
            List<?> currentInns = (List<?>) currentInnsRaw;
            for (int i = 0; i < 4; i++) {
                obs.setCurrentARVTreatmentINNs(i, i < currentInns.size() ? asString(currentInns.get(i)) : "");
            }
        }

        // Future/prescribed ARV treatment INN list (up to 4 indexed free-text entries)
        Object futureInnsRaw = obsMap.get("futureARVTreatmentINNsList");
        if (futureInnsRaw instanceof List) {
            List<?> futureInns = (List<?>) futureInnsRaw;
            for (int i = 0; i < 4; i++) {
                obs.setFutureARVTreatmentINNs(i, i < futureInns.size() ? asString(futureInns.get(i)) : "");
            }
        }

        // Disease history observations
        obs.setAnyPriorDiseases(asString(obsMap.get("anyPriorDiseases")));
        obs.setPriorDiseases(asString(obsMap.get("priorDiseases")));
        obs.setPriorDiseasesValue(asString(obsMap.get("priorDiseasesValue")));
        obs.setAnyCurrentDiseases(asString(obsMap.get("anyCurrentDiseases")));
        obs.setCurrentDiseases(asString(obsMap.get("currentDiseases")));
        obs.setCurrentDiseasesValue(asString(obsMap.get("currentDiseasesValue")));
        obs.setCurrentOITreatment(asString(obsMap.get("currentOITreatment")));
        obs.setCotrimoxazoleTreatment(asString(obsMap.get("cotrimoxazoleTreatment")));
        obs.setCotrimoxazoleTreatmentAnyAdverseEffects(asString(obsMap.get("cotrimoxazoleTreatmentAnyAdverseEffects")));
        obs.setAntiTbTreatment(asString(obsMap.get("antiTbTreatment")));
        obs.setAnySecondaryTreatment(asString(obsMap.get("anySecondaryTreatment")));
        obs.setSecondaryTreatment(asString(obsMap.get("secondaryTreatment")));

        // Vitals / clinical scores
        obs.setPatientWeight(asString(obsMap.get("patientWeight")));
        obs.setKarnofskyScore(asString(obsMap.get("karnofskyScore")));
        obs.setClinicVisits(asString(obsMap.get("clinicVisits")));

        // CD4 observations
        obs.setCd4Count(asString(obsMap.get("cd4Count")));
        obs.setCd4Percent(asString(obsMap.get("cd4Percent")));
        obs.setInitcd4Count(asString(obsMap.get("initcd4Count")));
        obs.setInitcd4Percent(asString(obsMap.get("initcd4Percent")));
        obs.setInitcd4Date(asString(obsMap.get("initcd4Date")));
        obs.setDemandcd4Count(asString(obsMap.get("demandcd4Count")));
        obs.setDemandcd4Percent(asString(obsMap.get("demandcd4Percent")));
        obs.setDemandcd4Date(asString(obsMap.get("demandcd4Date")));
        obs.setPriorCd4Date(asString(obsMap.get("priorCd4Date")));

        // Viral load observations
        obs.setVlPregnancy(asString(obsMap.get("vlPregnancy")));
        obs.setVlSuckle(asString(obsMap.get("vlSuckle")));
        obs.setVlReasonForRequest(asString(obsMap.get("vlReasonForRequest")));
        obs.setVlOtherReasonForRequest(asString(obsMap.get("vlOtherReasonForRequest")));
        obs.setVlBenefit(asString(obsMap.get("vlBenefit")));
        obs.setPriorVLLab(asString(obsMap.get("priorVLLab")));
        obs.setPriorVLValue(asString(obsMap.get("priorVLValue")));
        obs.setPriorVLDate(asString(obsMap.get("priorVLDate")));

        // Investigation status
        obs.setUnderInvestigation(asString(obsMap.get("underInvestigation")));

        // RTN-specific observations
        obs.setHospitalPatient(asString(obsMap.get("hospitalPatient")));
        obs.setService(asString(obsMap.get("service")));
        obs.setReason(asString(obsMap.get("reason")));

        // EID-specific observations
        obs.setEidTypeOfClinic(asString(obsMap.get("eidTypeOfClinic")));
        obs.setEidTypeOfClinicOther(asString(obsMap.get("eidTypeOfClinicOther")));
        obs.setEidHowChildFed(asString(obsMap.get("eidHowChildFed")));
        obs.setEidStoppedBreastfeeding(asString(obsMap.get("eidStoppedBreastfeeding")));
        obs.setEidInfantSymptomatic(asString(obsMap.get("eidInfantSymptomatic")));
        obs.setEidMothersHIVStatus(asString(obsMap.get("eidMothersHIVStatus")));
        obs.setEidMothersARV(asString(obsMap.get("eidMothersARV")));
        obs.setEidInfantsARV(asString(obsMap.get("eidInfantsARV")));
        obs.setEidInfantCotrimoxazole(asString(obsMap.get("eidInfantCotrimoxazole")));
        obs.setEidInfantPTME(asString(obsMap.get("eidInfantPTME")));
        obs.setWhichPCR(asString(obsMap.get("whichPCR")));
        obs.setReasonForSecondPCRTest(asString(obsMap.get("reasonForSecondPCRTest")));

        form.setObservations(obs);

        // ── Project data (sample-level boolean flags + org fields) ────────────
        Map<String, Object> projMap = payload.get("projectData") instanceof Map
                ? (Map<String, Object>) payload.get("projectData")
                : new HashMap<>();

        ProjectData projectData = new ProjectData();
        projectData.setEdtaTubeTaken(asBoolean(projMap.get("edtaTubeTaken")));
        projectData.setDbsTaken(asBoolean(projMap.get("dbsTaken")));
        projectData.setdbsvlTaken(asBoolean(projMap.get("dbsvlTaken")));
        projectData.setPscvlTaken(asBoolean(projMap.get("pscvlTaken")));
        projectData.setViralLoadTest(asBoolean(projMap.get("viralLoadTest")));
        projectData.setAsanteTest(asBoolean(projMap.get("asanteTest")));
        projectData.setPlasmaTaken(asBoolean(projMap.get("plasmaTaken")));
        projectData.setSerumTaken(asBoolean(projMap.get("serumTaken")));
        projectData.setUnderInvestigationNote(asString(projMap.get("underInvestigationNote")));
        projectData.setEIDSiteName(asString(projMap.get("EIDsiteName")));
        projectData.setEIDsiteCode(asString(projMap.get("EIDsiteCode")));
        projectData.setARVcenterCode(asString(projMap.get("ARVcenterCode")));
        projectData.setARVcenterName(asString(projMap.get("ARVcenterName")));

        form.setProjectData(projectData);

        return form;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Null-safe conversion helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts any object (typically deserialized from JSON) to a non-null, trimmed
     * {@link String}. Returns an empty string for {@code null}.
     */
    private String asString(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    /**
     * Converts any object to a primitive {@code boolean}. Recognises
     * {@link Boolean#TRUE} and the strings {@code "true"}, {@code "1"},
     * {@code "yes"} (all case-insensitive). Everything else — including
     * {@code null} — returns {@code false}.
     */
    private boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            String s = value.toString().trim().toLowerCase();
            return "true".equals(s) || "1".equals(s) || "yes".equals(s);
        }
        return false;
    }
}
