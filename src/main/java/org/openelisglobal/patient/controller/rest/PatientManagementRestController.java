package org.openelisglobal.patient.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientPhotoService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class PatientManagementRestController extends BaseRestController {
    @Autowired
    SearchResultsService searchService;
    @Autowired
    PatientIdentityService patientIdentityService;
    @Autowired
    PatientService patientService;
    @Autowired
    FhirTransformService fhirTransformService;
    @Autowired
    PatientPhotoService photoService;

    @PostMapping(value = "PatientManagement", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void savepatient(HttpServletRequest request,
            @Validated(SamplePatientEntryForm.SamplePatientEntry.class) @RequestBody PatientManagementInfo patientInfo,
            BindingResult bindingResult) throws Exception {

        if (StringUtils.isNotBlank(patientInfo.getPatientPK())) {
            patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);
        } else {
            patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        }
        Patient patient = new Patient();

        if (patientInfo.getPatientUpdateStatus() != PatientUpdateStatus.NO_ACTION) {
            preparePatientData(bindingResult, request, patientInfo, patient);
            if (bindingResult.hasErrors()) {
                try {
                    throw new BindException(bindingResult);
                } catch (BindException e) {
                    LogEvent.logError(e);
                }
            }
            try {
                patientService.persistPatientData(patientInfo, patient, getSysUserId(request));
                fhirTransformService.transformPersistPatient(patientInfo,
                        (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD));
                photoService.savePhoto(patient.getId(), patientInfo.getPhoto());
            } catch (LIMSRuntimeException e) {

                if (e.getCause() instanceof StaleObjectStateException) {

                } else {
                    LogEvent.logDebug(e);
                }
                request.setAttribute(ALLOW_EDITS_KEY, "false");

            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
            }
        }
    }

    @PostMapping(value = "CredentialPatientManagement", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCredentialPatient(HttpServletRequest request,
            @Validated(SamplePatientEntryForm.SamplePatientEntry.class) @RequestBody PatientManagementInfo patientInfo,
            BindingResult bindingResult) throws Exception {

        if (StringUtils.isNotBlank(patientInfo.getPatientPK())) {
            patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);
        } else if (StringUtils.isNotBlank(patientInfo.getGuid())) {
            Patient existingByGuid = patientService.getPatientForGuid(patientInfo.getGuid());
            if (existingByGuid != null) {
                patientInfo.setPatientPK(existingByGuid.getId());
                patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);
            } else {
                patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
            }
        } else {
            patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        }
        Patient patient = new Patient();

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.NO_ACTION) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "ERROR", "message", "No action specified for patient"));
        }
        try {
            preparePatientData(bindingResult, request, patientInfo, patient);
            if (bindingResult.hasErrors()) {
                try {
                    throw new BindException(bindingResult);
                } catch (BindException e) {
                    LogEvent.logError(e);
                }
                java.util.Map<String, Object> body = new java.util.HashMap<>();
                java.util.List<java.util.Map<String, String>> errorList = new java.util.ArrayList<>();
                bindingResult.getFieldErrors().forEach(error -> {
                    errorList.add(java.util.Map.of(
                            "field", error.getField(),
                            "code", error.getCode() == null ? "" : error.getCode(),
                            "message", error.getDefaultMessage() == null ? "" : error.getDefaultMessage()));
                });
                body.put("status", "ERROR");
                body.put("errors", errorList);
                return ResponseEntity.badRequest().body(body);
            }

            patientService.persistPatientData(patientInfo, patient, getSysUserId(request), true);
            fhirTransformService.transformPersistPatient(patientInfo,
                    (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD));
            photoService.savePhoto(patient.getId(), patientInfo.getPhoto());
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("status",
                    patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD ? "CREATED" : "UPDATED");
            body.put("patientId", patient.getId());
            body.put("guid", patientInfo.getGuid());
            body.put("fhirUuid", patient.getFhirUuid() != null ? patient.getFhirUuid().toString() : null);
            return ResponseEntity.ok(body);
        } catch (LIMSRuntimeException e) {

            if (e.getCause() instanceof StaleObjectStateException) {
                LogEvent.logDebug(e);
                request.setAttribute(ALLOW_EDITS_KEY, "false");
                return ResponseEntity.status(409).body(
                        Map.of("status", "ERROR", "message", "Stale object state while saving patient"));
            } else {
                LogEvent.logDebug(e);
                request.setAttribute(ALLOW_EDITS_KEY, "false");
                return ResponseEntity.status(500).body(
                        Map.of("status", "ERROR", "message", "Unexpected error while saving patient"));
            }

        } catch (FhirTransformationException | FhirPersistanceException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(500)
                    .body(Map.of("status", "ERROR", "message", "FHIR transformation/persistence error"));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Unexpected server error",
                    "exception", e.getClass().getSimpleName()));
        }
    }

    @GetMapping("patient-photos/{id}/{isThumbnail}")
    public ResponseEntity<Map<String, String>> getPhoto(@PathVariable String id, @PathVariable boolean isThumbnail)
            throws LIMSRuntimeException {
        String photo = photoService.getPhotoByPatientId(id, isThumbnail);
        return ResponseEntity.ok(Map.of("data", photo));
    }

    private void preparePatientData(Errors errors, HttpServletRequest request, PatientManagementInfo patientInfo,
            Patient patient) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        ValidatePatientInfo.validatePatientInfo(errors, patientInfo);
        if (errors.hasErrors()) {
            return;
        }

        initMembers(patient);
        patientInfo.setPatientIdentities(new ArrayList<PatientIdentity>());

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE) {
            Patient dbPatient = loadForUpdate(patientInfo);
            PropertyUtils.copyProperties(patient, dbPatient);
        }

        copyFormBeanToValueHolders(patientInfo, patient);

        setSystemUserID(patientInfo, patient, request);

        setLastUpdatedTimeStamps(patientInfo, patient);
    }

    private void copyFormBeanToValueHolders(PatientManagementInfo patientInfo, Patient patient)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PropertyUtils.copyProperties(patient, patientInfo);
        PropertyUtils.copyProperties(patient.getPerson(), patientInfo);
    }

    private void setSystemUserID(PatientManagementInfo patientInfo, Patient patient, HttpServletRequest request) {
        patient.setSysUserId(getSysUserId(request));
        patient.getPerson().setSysUserId(getSysUserId(request));

        for (PatientIdentity identity : patientInfo.getPatientIdentities()) {
            identity.setSysUserId(getSysUserId(request));
        }
        if (patientInfo.getPatientContact() != null) {
            patientInfo.getPatientContact().setSysUserId(getSysUserId(request));
        }
    }

    private void initMembers(Patient patient) {
        patient.setPerson(new Person());
    }

    private void setLastUpdatedTimeStamps(PatientManagementInfo patientInfo, Patient patient) {
        String patientUpdate = patientInfo.getPatientLastUpdated();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(patientUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(patientUpdate);
            patient.setLastupdated(timeStamp);
        }

        String personUpdate = patientInfo.getPersonLastUpdated();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(personUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(personUpdate);
            patient.getPerson().setLastupdated(timeStamp);
        }
    }

    private Patient loadForUpdate(PatientManagementInfo patientInfo) {
        Patient patient = patientService.get(patientInfo.getPatientPK());
        patientInfo.setPatientIdentities(patientIdentityService.getPatientIdentitiesForPatient(patient.getId()));
        return patient;
    }
}
