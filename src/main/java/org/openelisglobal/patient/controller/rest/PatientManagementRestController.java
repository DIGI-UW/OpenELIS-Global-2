package org.openelisglobal.patient.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
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
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
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

            PatientUtil.preparePatientData(bindingResult, request, patientInfo, patient);
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

    @GetMapping("patient-photos/{id}/{isThumbnail}")
    public ResponseEntity<Map<String, String>> getPhoto(@PathVariable String id, @PathVariable boolean isThumbnail)
            throws LIMSRuntimeException {
        String photo = photoService.getPhotoByPatientId(id, isThumbnail);
        return ResponseEntity.ok(Map.of("data", photo));
    }

}
