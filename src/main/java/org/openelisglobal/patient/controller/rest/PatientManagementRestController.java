package org.openelisglobal.patient.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.fhir.exception.FhirPersistanceException;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientIdDocumentInfo;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientIdDocumentService;
import org.openelisglobal.patient.service.PatientPhotoService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientIdDocument;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.search.service.SearchResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @Autowired
    PatientIdDocumentService idDocumentService;

    @PostMapping(value = "PatientManagement", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> savepatient(HttpServletRequest request,
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
                // Surface validation errors instead of falling through to
                // persist with a half-built entity (which would later throw
                // "attempt to create event with null entity").
                LogEvent.logError(new BindException(bindingResult));
                org.springframework.validation.FieldError fe = bindingResult.getFieldError();
                String message = fe != null
                        ? fe.getField() + ": " + StringUtils.defaultIfBlank(fe.getDefaultMessage(), "invalid value")
                        : "Validation failed";
                return ResponseEntity.badRequest().body(Map.of("error", message));
            }
            try {
                String sysUserId = getSysUserId(request);
                patientService.persistPatientData(patientInfo, patient, sysUserId);
                fhirTransformService.transformPersistPatient(patientInfo,
                        (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD));
                photoService.savePhoto(patient.getId(), patientInfo.getPhoto(), sysUserId);
                if (patientInfo.getIdDocuments() != null) {
                    for (PatientIdDocumentInfo docInfo : patientInfo.getIdDocuments()) {
                        if (docInfo.getId() == null && docInfo.getData() != null) {
                            idDocumentService.saveDocument(patient.getId(), docInfo.getData(), docInfo.getCategory(),
                                    docInfo.getDescription(), sysUserId);
                        }
                    }
                }
            } catch (LIMSRuntimeException e) {
                // Previously this exception was logged and silently swallowed,
                // so the client got HTTP 200 even when the save failed. Now we
                // surface the actual message so the UI can display it.
                LogEvent.logError(e);
                request.setAttribute(ALLOW_EDITS_KEY, "false");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", StringUtils.defaultIfBlank(e.getMessage(), "Failed to save patient")));
            } catch (FhirTransformationException | FhirPersistanceException e) {
                LogEvent.logError(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", StringUtils.defaultIfBlank(e.getMessage(), "Failed to save patient")));
            } catch (Exception e) {
                // Catch-all for unchecked exceptions (e.g. Hibernate
                // IllegalArgumentException on a null entity). Without this
                // they bubbled to Spring's default handler which returned
                // an empty 500 body, so the UI fell back to "Check server
                // logs" instead of the real message.
                LogEvent.logError(e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", StringUtils.defaultIfBlank(e.getMessage(), "Failed to save patient")));
            }
        }
        // Return the saved patient id so the frontend can navigate to
        // the saved record's results page (or skip the redirect for the
        // NO_ACTION path where the patient row wasn't actually written).
        if (patient.getId() != null) {
            return ResponseEntity.ok(Map.of("status", "success", "patientId", patient.getId()));
        }
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("patient-photos/{id}/{isThumbnail}")
    public ResponseEntity<Map<String, String>> getPhoto(@PathVariable String id, @PathVariable boolean isThumbnail)
            throws LIMSRuntimeException {
        String photo = photoService.getPhotoByPatientId(id, isThumbnail);
        if (photo == null) {
            return ResponseEntity.ok(Map.of("data", ""));
        }
        return ResponseEntity.ok(Map.of("data", photo));
    }

    @GetMapping("patient-id-documents/{patientId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getIdDocuments(@PathVariable String patientId)
            throws LIMSRuntimeException {
        List<PatientIdDocument> documents = idDocumentService.getDocumentsByPatientId(patientId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (PatientIdDocument doc : documents) {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doc.getId());
            docMap.put("thumbnail", "data:" + doc.getDocumentType() + ";base64," + doc.getThumbnailData());
            docMap.put("category", doc.getDocumentCategory());
            docMap.put("description", doc.getDescription());
            docMap.put("lastUpdated", doc.getLastupdated());
            result.add(docMap);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("patient-id-documents/{patientId}/{documentId}/full")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getIdDocumentFull(@PathVariable String patientId,
            @PathVariable Integer documentId) throws LIMSRuntimeException {
        List<PatientIdDocument> documents = idDocumentService.getDocumentsByPatientId(patientId);
        for (PatientIdDocument doc : documents) {
            if (doc.getId().equals(documentId)) {
                String fullData = "data:" + doc.getDocumentType() + ";base64," + doc.getDocumentData();
                return ResponseEntity.ok(Map.of("data", fullData));
            }
        }
        return ResponseEntity.ok(Map.of("data", ""));
    }

    @PutMapping("patient-id-documents/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateIdDocument(HttpServletRequest request,
            @PathVariable Integer documentId, @RequestBody PatientIdDocumentInfo docInfo) throws LIMSRuntimeException {
        PatientIdDocument updated = idDocumentService.updateDocument(documentId, docInfo.getData(),
                docInfo.getCategory(), docInfo.getDescription(), getSysUserId(request));
        if (updated != null) {
            return ResponseEntity.ok(Map.of("status", "success"));
        }
        return ResponseEntity.ok(Map.of("status", "not_found"));
    }

    @DeleteMapping("patient-id-documents/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteIdDocument(HttpServletRequest request,
            @PathVariable Integer documentId) throws LIMSRuntimeException {
        idDocumentService.softDeleteDocument(documentId, getSysUserId(request));
        return ResponseEntity.ok(Map.of("status", "success"));
    }

}
