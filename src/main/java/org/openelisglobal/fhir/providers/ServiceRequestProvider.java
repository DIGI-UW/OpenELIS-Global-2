package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
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
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.StaleObjectStateException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.action.util.SampleUtil;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SampleEditService;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.validator.SampleEditFormValidator;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

@Component
public class ServiceRequestProvider implements IResourceProvider {

    @Autowired
    private SamplePatientEntryService samplePatientService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private SampleUtil sampleUtil;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleEditService sampleEditService;

    @Autowired
    private FhirUtil util;

    @Autowired
    public SampleEditFormValidator formValidator;

        @Autowired
    private TestService testService;

    @Autowired
    private UserService userService;

    @Autowired
    private SampleItemService sampleItemService;

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

    try {
        if (request == null) {
            throw new InvalidRequestException("HttpServletRequest cannot be null");
        }

        String sysuserId = FhirProviderUtils.getSysUserId(request);
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

        if (updateData.getSampleItemsTests() == null || updateData.getSampleItemsTests().isEmpty()) {
            throw new InvalidRequestException("No sample items/tests found in ServiceRequest");
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
        
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.NO_ACTION);
        PatientManagementUpdate patientUpdate = SpringContext.getBean(PatientManagementUpdate.class);

        if (patientUpdate == null) {
            throw new InternalErrorException("PatientManagementUpdate bean not found");
        }

        patientUpdate.setSysUserIdFromRequest(request);
        patientUpdate.setPatientUpdateStatus(patientInfo);

        String originalPatientId = updateData.getPatientId();
        samplePatientService.persistData(updateData, patientUpdate, patientInfo, form, request);

        SampleHuman lookupSampleHuman = new SampleHuman();
        lookupSampleHuman.setSampleId(updateData.getSample().getId());
        SampleHuman existingSampleHuman = sampleHumanService.getDataBySample(lookupSampleHuman);
        
        if (existingSampleHuman != null) {
            if (existingSampleHuman.getPatientId() == null || !originalPatientId.equals(existingSampleHuman.getPatientId())) {
                LogEvent.logInfo(this.getClass().getSimpleName(), method, 
                    "Setting SampleHuman patientId to: " + originalPatientId);
                existingSampleHuman.setPatientId(originalPatientId);
                existingSampleHuman.setSysUserId(sysuserId);
                sampleHumanService.update(existingSampleHuman);
            }
        } 

        fhirTransformService.transformPersistOrderEntryFhirObjects(updateData, patientInfo, false, null);

        // Get the created ServiceRequest to return
        ServiceRequest createdServiceRequest = null;
        if (updateData.getSampleItemsTests() != null && !updateData.getSampleItemsTests().isEmpty()) {
            SampleTestCollection firstCollection = updateData.getSampleItemsTests().get(0);
            if (firstCollection.analysises != null && !firstCollection.analysises.isEmpty()) {
                Analysis createdAnalysis = firstCollection.analysises.get(0);
                createdServiceRequest = fhirTransformService.transformToServiceRequest(createdAnalysis.getId());
            }
        }
        
        if (createdServiceRequest == null) {
            throw new InternalErrorException("Failed to transform created Analysis to ServiceRequest");
        }

        MethodOutcome outcome = new MethodOutcome();
        outcome.setCreated(true);
        outcome.setResource(createdServiceRequest);
        return outcome;

    } catch (InvalidRequestException | ResourceNotFoundException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));
        throw e;

    } catch (InternalErrorException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Internal error: " + safeMessage(e));
        throw e;

    } catch (Exception e) {
        e.printStackTrace();
        LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));
        throw new InternalErrorException("Unexpected server error while creating ServiceRequest: " + e.getMessage(), e);
    }
}

@Update
public MethodOutcome updateServiceRequest(
        @IdParam IdType theId,
        @ResourceParam ServiceRequest serviceRequest,
        HttpServletRequest request) {

    final String method = "updateServiceRequest";

    try {
        // ===== VALIDATIONS =====
        if (theId == null || theId.getIdPart() == null) {
            throw new InvalidRequestException("Missing ServiceRequest ID in URL");
        }

        if (request == null) {
            throw new InvalidRequestException("HttpServletRequest cannot be null");
        }

        if (!serviceRequest.hasCode() || !serviceRequest.getCode().hasCoding()) {
            throw new InvalidRequestException("ServiceRequest.code.coding is required");
        }

        String sysUserId = FhirProviderUtils.getSysUserId(request);
        String analysisUuid = theId.getIdPart();

        List<Analysis> existingAnalyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));

        if (existingAnalyses.isEmpty()) {
            throw new ResourceNotFoundException("Analysis not found with UUID: " + analysisUuid);
        }

        Analysis existingAnalysis = existingAnalyses.get(0);
        Sample existingSample = existingAnalysis.getSampleItem().getSample();

        // ===== BUILD FORM =====
        SampleEditForm form = new SampleEditForm();
        
        // Set basic form fields
        form.setAccessionNumber(existingSample.getAccessionNumber());
        form.setCurrentDate(DateUtil.getCurrentDateAsText());
        form.setIsEditable(true);
        form.setSearchFinished(true);
        form.setNoSampleFound(false);
        
        // Build and set SampleOrderItem
        SampleOrderItem orderItem = fhirTransformService.buildSampleOrderItemFromServiceRequest(serviceRequest, sysUserId);
        form.setSampleOrderItems(orderItem);
        
        // Build SampleEditItem list from ServiceRequest
        List<SampleEditItem> editItems = fhirTransformService.buildSampleEditItemsListFromServiceRequest(serviceRequest, sysUserId);
        
        // Separate existing and new tests
        List<SampleEditItem> existingTests = editItems.stream()
                .filter(item -> !item.isAdd() && !item.isCanceled())
                .collect(Collectors.toList());
        
        List<SampleEditItem> possibleTests = editItems.stream()
                .filter(item -> item.isAdd())
                .collect(Collectors.toList());
        
        form.setExistingTests(existingTests);
        form.setPossibleTests(possibleTests);
        
        // Build sample XML if there are new tests
        if (!possibleTests.isEmpty()) {
            // Get all tests to add (both existing and new)
            List<Test> allTests = new ArrayList<>();
            
            // Add existing tests
            for (Analysis analysis : existingAnalyses) {
                if (analysis.getTest() != null && 
                    !statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)) {
                    allTests.add(analysis.getTest());
                }
            }
            
            // Add new tests
            for (SampleEditItem newItem : possibleTests) {
                Test test = testService.get(newItem.getTestId());
                if (test != null) {
                    allTests.add(test);
                }
            }
            
            // Build XML with sampleItemId
            SampleItem sampleItem = existingAnalysis.getSampleItem();
            String sampleXml = FhirUtil.buildSampleXml(allTests, sampleItem, sampleItem.getId());
            form.setSampleXML(sampleXml);
        }

            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(existingSample.getId());
    if (sampleItems != null && !sampleItems.isEmpty()) {
        // Get the highest sort order
        int maxSortOrder = sampleItems.stream()
                .mapToInt(item -> {
                    try {
                        return Integer.parseInt(item.getSortOrder());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        form.setMaxAccessionNumber(existingSample.getAccessionNumber() + "-" + maxSortOrder);
    } else {
        form.setMaxAccessionNumber(existingSample.getAccessionNumber() + "-0");
    }
        
        // Set sample types for the form
        List<IdValuePair> sampleTypes = userService.getUserSampleTypes(sysUserId, Constants.ROLE_RECEPTION);
        form.setSampleTypes(sampleTypes);
        
        // Set test section list
        form.setTestSectionList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        
        // Set reject reason list
        form.setRejectReasonList(DisplayListService.getInstance().getList(ListType.REJECTION_REASONS));
        
        // ===== VALIDATE FORM =====
        Errors result = new BindException(form, "form");
        formValidator.validate(form, result);

        if (result.hasErrors()) {
            throw new InvalidRequestException(formatErrors(result));
        }

        // ===== SAMPLE UPDATE =====
        boolean sampleChanged = sampleUtil.accessionNumberChanged(form);
        Sample updatedSample = null;

        if (sampleChanged) {
            sampleUtil.validateNewAccessionNumber(form.getNewAccessionNumber(), result);

            if (result.hasErrors()) {
                throw new InvalidRequestException(formatErrors(result));
            }

            updatedSample = sampleUtil.updateAccessionNumberInSample(form, FhirProviderUtils.getSysUserId(request));
        }

        try {
            sampleEditService.editSample(form, request, updatedSample, sampleChanged, sysUserId);

        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof StaleObjectStateException) {
                throw new InvalidRequestException("Optimistic locking failed - resource was modified by another user");
            }

            LogEvent.logDebug(e);
            throw new InternalErrorException("Error updating sample: " + e.getMessage(), e);
        }

        // ===== POST PROCESSING =====
        try {
            fhirTransformService.transformAnalysisByIds(sampleEditService.getUpdatedAnalysisList());
        } catch (Exception e) {
            LogEvent.logError(e);
            // Don't throw - transformation failure shouldn't rollback the update
        }

        // ===== BUILD RESPONSE =====
        ServiceRequest updatedServiceRequest = fhirTransformService.transformToServiceRequest(existingAnalysis.getId());

        if (updatedServiceRequest == null) {
            throw new InternalErrorException("Failed to transform updated Analysis to ServiceRequest");
        }

        MethodOutcome outcome = new MethodOutcome();
        outcome.setCreated(false);
        outcome.setId(new IdType("ServiceRequest", theId.getIdPart()));
        outcome.setResource(updatedServiceRequest);

        return outcome;

    } catch (InvalidRequestException | ResourceNotFoundException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));
        throw e;

    } catch (Exception e) {
        e.printStackTrace();
        LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));
        throw new InternalErrorException("Unexpected server error while updating ServiceRequest: " + e.getMessage(), e);
    }
}
@Delete
public MethodOutcome deleteServiceRequest(@IdParam IdType theId, HttpServletRequest request) {
    final String method = "deleteServiceRequest";

    try {
        if (theId == null || theId.getIdPart() == null) {
            throw new InvalidRequestException("Missing ServiceRequest ID in URL");
        }

        String sysUserId = FhirProviderUtils.getSysUserId(request);

        // Get existing analysis by FHIR ID
        String analysisUuid = theId.getIdPart();
        List<Analysis> existingAnalyses = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUuid));
        
        if (existingAnalyses.isEmpty()) {
            throw new ResourceNotFoundException("Analysis not found with UUID: " + analysisUuid);
        }
        
        Analysis analysis = existingAnalyses.get(0);
                analysis.setSysUserId(sysUserId);
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled));
        
Analysis updatedAnalysis = analysisService.update(analysis);
List<String> updatedAnalysisIds = new ArrayList<>();
updatedAnalysisIds.add(updatedAnalysis.getId());
fhirTransformService.transformAnalysisByIds(updatedAnalysisIds);
        
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(204);
        return outcome;

    } catch (InvalidRequestException | ResourceNotFoundException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Client error: " + safeMessage(e));
        throw e;

    } catch (InternalErrorException e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Internal error: " + safeMessage(e));
        throw e;

    } catch (Exception e) {
        LogEvent.logError(this.getClass().getSimpleName(), method, "Unhandled exception: " + safeMessage(e));
        throw new InternalErrorException("Unexpected server error while deleting ServiceRequest");
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

private String formatErrors(Errors errors) {
    if (!errors.hasErrors()) {
        return "";
    }
    
    // Log full details with stacktrace
    StringBuilder logMessage = new StringBuilder();
    logMessage.append("Validation failed with ").append(errors.getErrorCount()).append(" error(s)\n");
    
    for (Object error : errors.getAllErrors()) {
        if (error instanceof FieldError) {
            FieldError fieldError = (FieldError) error;
            logMessage.append("  - Field: '").append(fieldError.getField()).append("'")
                .append(", Rejected value: '").append(fieldError.getRejectedValue()).append("'")
                .append(", Message: ").append(fieldError.getDefaultMessage()).append("\n");
        } else {
            logMessage.append("  - ").append(error.toString()).append("\n");
        }
    }
    
    // Log with stacktrace
    LogEvent.logError(this.getClass().getSimpleName(), "formatErrors", 
        logMessage.toString());
    
    // Return formatted message for exception
    return errors.getAllErrors().stream()
            .map(e -> {
                if (e instanceof FieldError) {
                    FieldError fe = (FieldError) e;
                    return fe.getField() + ": " + fe.getDefaultMessage();
                }
                return e.getDefaultMessage();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("; "));
}

}
