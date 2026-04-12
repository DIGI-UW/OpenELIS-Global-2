package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.action.util.SampleUtil;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

@Component
public class ServiceRequestProvider implements IResourceProvider {

    @Autowired
    private SamplePatientEntryService samplePatientService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private FhirUtil util;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return ServiceRequest.class;

    }

    @Read
    public ServiceRequest readServiceRequest(@IdParam IdType theId) {
        String method = "readServiceRequest";
        try {
            FhirProviderUtils.validateIdParam(theId, "ServiceRequest", this.getClass().getSimpleName(), method);

            String analysisUuid = theId.getIdPart();
            List<Analysis> analyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));

            if (analyses == null || analyses.isEmpty()) {
                throw new ResourceNotFoundException("Analysis with FHIR ID: " + analysisUuid + " does not exist");
            }
            if (analyses.size() > 1) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "Duplicate Analysis records found for fhirUuid=" + analysisUuid);
                throw new InternalErrorException("Multiple Analysis records found for ServiceRequest UUID");
            }

            Analysis analysis = analyses.get(0);
            ServiceRequest serviceRequest = fhirTransformService.transformToServiceRequest(analysis.getId());
            if (serviceRequest == null) {
                throw new InternalErrorException("Failed to transform Analysis to ServiceRequest");
            }
            return serviceRequest;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("ServiceRequest ID must be a valid UUID");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading ServiceRequest: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading ServiceRequest", e);
        }
    }

    @Create
    public MethodOutcome createServiceRequest(@ResourceParam ServiceRequest serviceRequest,
            HttpServletRequest request) {

        final String method = "createServiceRequest";
        String sysuserId = null;

        try {

            if (request == null) {
                throw new InvalidRequestException("HttpServletRequest cannot be null");
            }

            sysuserId = FhirProviderUtils.getSysUserId(request);
            if (sysuserId == null || sysuserId.trim().isEmpty()) {
                throw new InvalidRequestException("Missing or invalid system user ID");
            }

            if (serviceRequest == null) {
                throw new InvalidRequestException("ServiceRequest resource cannot be null");
            }

            if (!serviceRequest.hasSubject()) {
                throw new InvalidRequestException("ServiceRequest.subject is required");
            }

            if (!serviceRequest.hasCode()) {
                throw new InvalidRequestException("ServiceRequest.code is required");
            }

            SamplePatientUpdateData updateData = fhirTransformService.createOrderItemFromServiceRequest(serviceRequest,
                    sysuserId);

            if (updateData == null) {
                throw new InternalErrorException("Failed to transform ServiceRequest into updateData");
            }

            if (updateData.getPatientId() == null) {
                throw new InvalidRequestException("Derived patientId is null after transformation");
            }

            SamplePatientEntryForm form = new SamplePatientEntryForm();
            PatientManagementInfo patientInfo;

            Patient fhirPatient = fhirTransformService.transformToFhirPatient(updateData.getPatientId());

            if (fhirPatient == null) {
                throw new ResourceNotFoundException("Patient not found for ID: " + updateData.getPatientId());
            }

            patientInfo = fhirTransformService.createOePatientManagementInfo(fhirPatient);
            Errors errors = new BindException(patientInfo, "patientInfo");
            ValidatePatientInfo.validatePatientInfo(errors, patientInfo);

            org.openelisglobal.patient.valueholder.Patient patient = new org.openelisglobal.patient.valueholder.Patient();
            PatientUtil.preparePatientData(errors, request, patientInfo, patient);

            if (patientInfo == null) {
                throw new InternalErrorException("Failed to create PatientManagementInfo");
            }
            if (patientInfo.getPatientContact() != null) {
                patient.getPerson().setSysUserId(sysuserId);
                patientInfo.getPatientContact().setPerson(patient.getPerson());
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), method, "No patient contact info provided");
            }

            PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);

            if (patientUpdate == null) {
                throw new InternalErrorException("PatientManagementUpdate bean not found");
            }

            patientUpdate.setSysUserIdFromRequest(request);

            SampleUtil.testAndInitializePatientForSaving(request, patientInfo, patientUpdate, updateData);

            samplePatientService.persistData(updateData, patientUpdate, patientInfo, form, request);

            fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, false, null);

            MethodOutcome outcome = new MethodOutcome();
            outcome.setCreated(true);
            outcome.setResource(serviceRequest);

            return outcome;

        } catch (InvalidRequestException | ResourceNotFoundException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));

            throw e;

        } catch (InternalErrorException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method, "Internal error: " + safeMessage(e));

            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));

            throw new InternalErrorException("Unexpected server error while creating ServiceRequest");
        }
    }

    @Search
    public Bundle searchForServiceRequests(
            @OptionalParam(name = ServiceRequest.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
                    Patient.SP_GIVEN, Patient.SP_FAMILY,
                    Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,

            @OptionalParam(name = ServiceRequest.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
                    Patient.SP_GIVEN, Patient.SP_FAMILY,
                    Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam subjectReference,

            @OptionalParam(name = ServiceRequest.SP_CODE) TokenAndListParam code,

            @OptionalParam(name = ServiceRequest.SP_REQUESTER, chainWhitelist = { "", Practitioner.SP_IDENTIFIER,
                    Practitioner.SP_GIVEN, Practitioner.SP_FAMILY,
                    Practitioner.SP_NAME }, targetTypes = Practitioner.class) ReferenceAndListParam participantReference,

            @OptionalParam(name = ServiceRequest.SP_OCCURRENCE) DateRangeParam occurrence,

            @OptionalParam(name = ServiceRequest.SP_RES_ID) TokenAndListParam uuid,

            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,

            @OptionalParam(name = ServiceRequest.SP_SPECIMEN, chainWhitelist = { "",
                    Specimen.SP_IDENTIFIER }, targetTypes = Specimen.class) ReferenceAndListParam specimenReference,

            @IncludeParam(allow = { "ServiceRequest:" + ServiceRequest.SP_PATIENT,
                    "ServiceRequest:" + ServiceRequest.SP_SUBJECT, "ServiceRequest:" + ServiceRequest.SP_REQUESTER,
                    "ServiceRequest:" + ServiceRequest.SP_SPECIMEN }) HashSet<Include> includes,

            @IncludeParam(reverse = true, allow = { "Observation:based-on" }) HashSet<Include> revIncludes,

            HttpServletRequest request) {

        String method = "search";

        try {
            Bundle resultBundle = util.forwardSearchToFhirStore(request);

            if (resultBundle == null) {
                resultBundle = new Bundle();
            }

            if (resultBundle.getType() == null) {
                resultBundle.setType(Bundle.BundleType.SEARCHSET);
            }

            if (resultBundle.getEntry() == null) {
                resultBundle.setEntry(new ArrayList<>());
            }

            return resultBundle;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching ServiceRequest: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching ServiceRequest");
        }
    }

    /**
     * Prevents NPE when logging exception messages
     */
    private String safeMessage(Exception e) {
        return (e == null || e.getMessage() == null) ? "No error message available" : e.getMessage();
    }

}
