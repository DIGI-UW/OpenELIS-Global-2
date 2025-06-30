package org.openelisglobal.patient.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.openelisglobal.patient.saving.IAccessioner;
import org.openelisglobal.patient.saving.RequestType;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;
import org.springframework.validation.Errors;

public abstract class BasePatientEntryByProject extends BaseController {

    private static final String[] BASE_ALLOWED_FIELDS = new String[] { "patientUpdateStatus", "patientPK", "samplePK",
            "receivedDateForDisplay", "receivedTimeForDisplay", "interviewDate", "interviewTime", "subjectNumber",
            "siteSubjectNumber", "labNo", "centerName", "centerCode", "lastName", "firstName", "gender", "upidCode",
            "birthDateForDisplay", "observations.projectFormName", "observations.hivStatus",
            "observations.educationLevel", "observations.maritalStatus", "observations.nationality",
            "observations.nationalityOther", "observations.legalResidence", "observations.nameOfDoctor",
            "observations.anyPriorDiseases", "observations.priorDiseases", "observations.priorDiseasesValue",
            "observations.arvProphylaxisBenefit", "observations.arvProphylaxis", "observations.currentARVTreatment",
            "observations.priorARVTreatment", "observations.priorARVTreatmentINNsList*",
            "observations.cotrimoxazoleTreatment", "observations.aidsStage", "observations.anyCurrentDiseases",
            "ProjectData.dbsTaken", "ProjectData.dbsvlTaken", "ProjectData.pscvlTaken", "ProjectData.edtaTubeTaken",
            "ProjectData.viralLoadTest", "observations.currentDiseases", "observations.currentDiseasesValue",
            "observations.currentOITreatment", "observations.patientWeight", "observations.karnofskyScore",
            "observations.underInvestigation", "projectData.underInvestigationNote",
            //
            "observations.cd4Count", "observations.cd4Percent", "observations.priorCd4Date",
            "observations.antiTbTreatment", "observations.interruptedARVTreatment",
            "observations.arvTreatmentAnyAdverseEffects", "observations.arvTreatmentAdverseEffects*.type",
            "observations.arvTreatmentAdverseEffects*.grade", "observations.arvTreatmentChange",
            "observations.arvTreatmentNew", "observations.arvTreatmentRegime",
            "observations.futureARVTreatmentINNsList*", "observations.cotrimoxazoleTreatmentAnyAdverseEffects",
            "observations.cotrimoxazoleTreatmentAdverseEffects*.type",
            "observations.cotrimoxazoleTreatmentAdverseEffects*.grade", "observations.anySecondaryTreatment",
            "observations.secondaryTreatment", "observations.clinicVisits",
            //
            "projectData.EIDSiteName", "projectData.EIDsiteCode", "observations.whichPCR",
            "observations.reasonForSecondPCRTest", "observations.nameOfRequestor", "observations.nameOfSampler",
            "observations.eidInfantPTME", "observations.eidTypeOfClinic", "observations.eidTypeOfClinicOther",
            "observations.eidHowChildFed", "observations.eidStoppedBreastfeeding", "observations.eidInfantSymptomatic",
            "observations.eidInfantsARV", "observations.eidInfantCotrimoxazole", "observations.eidMothersHIVStatus",
            "observations.eidMothersARV",
            //
            "projectData.ARVcenterName", "projectData.ARVcenterCode", "observations.vlPregnancy",
            "observations.vlSuckle", "observations.arvTreatmentInitDate", "observations.vlReasonForRequest",
            "observations.vlOtherReasonForRequest", "observations.initcd4Count", "observations.initcd4Percent",
            "observations.initcd4Date", "observations.demandcd4Count", "observations.demandcd4Percent",
            "observations.demandcd4Date", "observations.vlBenefit", "observations.priorVLLab",
            "observations.priorVLValue", "observations.priorVLDate",
            //
            "observations.service", "observations.hospitalPatient", "observations.reason", "ProjectData.asanteTest",
            "ProjectData.plasmaTaken", "ProjectData.serumTaken" };

    protected List<String> getBasePatientEntryByProjectFields() {
        List<String> allowedFields = new ArrayList<>();
        allowedFields.addAll(Arrays.asList(BASE_ALLOWED_FIELDS));

        ObservationData observationData = new ObservationData();
        List<Pair<String, String>> priorDiseasesList = observationData.getPriorDiseasesList();
        for (Pair<String, String> priorDisease : priorDiseasesList) {
            allowedFields.add("observations." + priorDisease.getKey());
        }
        List<Pair<String, String>> currentDiseasesList = observationData.getCurrentDiseasesList();
        for (Pair<String, String> currentDisease : currentDiseasesList) {
            allowedFields.add("observations." + currentDisease.getKey());
        }
        List<Pair<String, String>> priorRTNDiseasesList = observationData.getRtnPriorDiseasesList();
        for (Pair<String, String> priorDisease : priorRTNDiseasesList) {
            allowedFields.add("observations." + priorDisease.getKey());
        }
        List<Pair<String, String>> currentRTNDiseasesList = observationData.getRtnCurrentDiseasesList();
        for (Pair<String, String> currentDisease : currentRTNDiseasesList) {
            allowedFields.add("observations." + currentDisease.getKey());
        }
        return allowedFields;
    }

    public BasePatientEntryByProject() {
        super();
    }

    protected void updateRequestType(HttpServletRequest httpRequest) {
        httpRequest.getSession().setAttribute("type", getRequestType(httpRequest).toString().toLowerCase());
    }

    protected RequestType getRequestType(HttpServletRequest httpRequest) {
        String requestTypeStr = httpRequest.getParameter("type");
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(requestTypeStr)) {
            try {
                return RequestType.valueOf(requestTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LogEvent.logWarn("BasePatientEntryByProject", "getRequestType",
                        "request type '" + requestTypeStr + "' invalid");
                LogEvent.logWarn(e);
                return RequestType.UNKNOWN;
            }
        }
        return RequestType.UNKNOWN;
    }

    /**
     * Put the projectFormName in the right place, so that ends up driving the next
     * patientEntry form to select the right form. The projectFormName is retained
     * even through form edit/entry(add) reselection, so that all forms know the
     * current projectForm, so that the user is presented with a good guess on what
     * they might want to keep doing.
     *
     * @param projectFormName @ all from property utils if we've coded things wrong
     *                        in the form def or in this class.
     */
    protected void setProjectFormName(PatientEntryByProjectForm form, String projectFormName)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ObservationData observations = form.getObservations();
        ProjectData projectData = form.getProjectData();
        if (observations == null) {
            observations = new ObservationData();
            form.setObservations(observations);
        }
        if (projectData == null) {
            projectData = new ProjectData();
            form.setProjectData(projectData);
        }
        observations.setProjectFormName(projectFormName);
    }

    /**
     * This method captures how we deal with the curious accession objects I
     * (PaHill) created. If someone comes up with a better API (Maybe inject a
     * messages list to be filled in?) then we can change this bit of plumbing.
     *
     * @param request     original request
     * @param accessioner the object to use to attempt to save.
     * @return a forward string or null; null => this attempt to save failed @ if
     *         things go really bad. Normally, the errors are caught internally an
     *         appropriate message is added and a forward fail is returned.
     */
    protected String handleSave(HttpServletRequest request, IAccessioner accessioner) {
        String forward;
        try {
            forward = accessioner.save();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LIMSException e) {
            Errors errors = accessioner.getMessages();
            if (errors.hasErrors()) {
                saveErrors(errors);
            }
            return FWD_FAIL_INSERT;
        }
        return forward;
    }

    /**
     * Just a utility routine for doing the obvious logging and and appending a
     * global message.
     */
    protected void logAndAddMessage(HttpServletRequest request, String methodName, String messageKey) {
        LogEvent.logError(this.getClass().getSimpleName(), methodName, "Unable to enter patient into system");
        Errors errors = new BaseErrors();
        errors.reject(messageKey, messageKey);
        saveErrors(errors);
    }

    /**
     * various maps full of a various lists used by the patient entry form
     * (typically for drop downs) and other forms who want to do patient entry.
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Map<String, Object> addAllPatientFormLists(PatientEntryByProjectForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Map<String, Object> resultMap = new HashMap<>();
        // resultMap.put("GENDERS", PatientUtil.findGenders());

        // below is more suitable for genders select forms as it is the one used in
        // others forms
        List<Dictionary> listOfDictionary = new ArrayList<>();
        List<IdValuePair> genders = DisplayListService.getInstance().getList(ListType.GENDERS);
        for (IdValuePair i : genders) {
            Dictionary dictionary = new Dictionary();
            dictionary.setId(i.getId());
            dictionary.setDictEntry(i.getValue());
            listOfDictionary.add(dictionary);
        }
        resultMap.put("GENDERS", listOfDictionary);

        form.setFormLists(resultMap);
        form.setDictionaryLists(ObservationHistoryList.MAP);
        form.setOrganizationTypeLists(OrganizationTypeList.MAP);

        return resultMap;
    }
}
