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
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Resource Provider for Observation resources.
 *
 * <p>
 * Exposes lab results from OpenELIS directly via the native FHIR facade. Read
 * and Write operations query OpenELIS DB directly for consistency with the
 * source of truth. Search forwards to the HAPI FHIR store to support the full
 * FHIR search parameter set.
 *
 * Supported operations:
 * <ul>
 * <li>CREATE: POST /fhir/Observation (requires basedOn ServiceRequest
 * reference; rejects Finalized/Canceled analyses and duplicate results)</li>
 * <li>READ: GET /fhir/Observation/{uuid}</li>
 * <li>SEARCH: GET /fhir/Observation?patient={uuid}&amp;...</li>
 * <li>UPDATE: PUT /fhir/Observation/{uuid}</li>
 * <li>DELETE: DELETE /fhir/Observation/{uuid}</li>
 * </ul>
 */
@Component
public class ObservationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private FhirUtil util;
    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    @Autowired
    private IStatusService statusService;

    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
    }

    @Read
    public Observation read(@IdParam IdType id) {
        String method = "read";
        try {
            if (id == null || !id.hasIdPart()) {
                throw new ResourceNotFoundException("Missing Observation ID");
            }
            String uuid = id.getIdPart();

            Result result = resultService.getResultByFhirUuid(uuid);
            if (result == null) {
                throw new ResourceNotFoundException("Observation not found: " + uuid);
            }

            Observation observation = fhirTransformService.transformResultToObservation(result);
            if (observation == null) {
                throw new ResourceNotFoundException("Failed to transform result to observation: " + uuid);
            }

            return observation;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error reading observation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error retrieving Observation");
        }
    }

    @Create
    public MethodOutcome create(@ResourceParam Observation fhirObservation, HttpServletRequest request) {
        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Observation");
        try {
            if (fhirObservation == null) {
                throw new InvalidRequestException("Observation resource cannot be null");
            }

            if (!fhirObservation.hasBasedOn() || fhirObservation.getBasedOnFirstRep() == null) {
                throw new InvalidRequestException("Observation must reference a ServiceRequest via basedOn");
            }
            String basedOnRef = fhirObservation.getBasedOnFirstRep().getReference();
            String analysisUuid = basedOnRef.contains("/") ? basedOnRef.substring(basedOnRef.lastIndexOf("/") + 1)
                    : basedOnRef;

            Analysis analysis = analysisService.getAnalysisByFhirUuid(analysisUuid);
            if (analysis == null) {
                throw new ResourceNotFoundException("ServiceRequest not found: " + analysisUuid);
            }

            String analysisStatusId = analysis.getStatusId();
            if (statusService.matches(analysisStatusId, StatusService.AnalysisStatus.Finalized)
                    || statusService.matches(analysisStatusId, StatusService.AnalysisStatus.Canceled)
                    || statusService.matches(analysisStatusId, StatusService.AnalysisStatus.SampleRejected)) {
                throw new InvalidRequestException("Cannot submit result: Analysis is already in status "
                        + statusService.getStatusName(statusService.getAnalysisStatusForID(analysisStatusId)));
            }

            List<Result> existingResults = resultService.getResultsByAnalysis(analysis);
            if (existingResults != null && !existingResults.isEmpty()) {
                throw new InvalidRequestException("A result already exists for this Analysis: " + analysisUuid
                        + ". Use PUT to update the existing Observation.");
            }

            if (analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
                throw new InternalErrorException("Analysis " + analysisUuid + " has no associated sample");
            }
            Sample sample = analysis.getSampleItem().getSample();
            org.openelisglobal.patient.valueholder.Patient patient = sampleHumanService.getPatientForSample(sample);

            ResultSaveBean bean = new ResultSaveBean();
            bean.setTestId(analysis.getTest().getId());
            bean.setReportable("Y");

            if (fhirObservation.hasValueQuantity()) {
                bean.setResultValue(fhirObservation.getValueQuantity().getValue().toPlainString());
                bean.setResultType("N");
            } else if (fhirObservation.hasValueStringType()) {
                bean.setResultValue(fhirObservation.getValueStringType().getValue());
                bean.setResultType("T");
            } else {
                throw new InvalidRequestException("Observation must have either valueQuantity or valueString");
            }

            String sysUserId = FhirProviderUtils.getSysUserId(request);
            if (sysUserId == null) {
                throw new InvalidRequestException("Authentication required to submit a result");
            }

            ResultSaveService resultSaveService = new ResultSaveService(analysis, sysUserId);
            List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, new ArrayList<>());

            if (results.isEmpty()) {
                throw new InternalErrorException("Failed to create Result from Observation");
            }

            ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(sysUserId);
            for (Result result : results) {
                result.setFhirUuid(UUID.randomUUID());
                actionDataSet.getNewResults()
                        .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
            }
            logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                    sysUserId);

            Result savedResult = results.get(0);
            try {
                fhirTransformService.transformPersistResult(savedResult);
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed (continuing anyway): " + e.getMessage());
            }

            Observation resultObservation = fhirTransformService.transformResultToObservation(savedResult);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Observation for Analysis: " + analysisUuid);

            return FhirProviderUtils.buildCreateOutcome(resultObservation);

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error creating observation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error creating Observation", e);
        }
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Observation fhirObservation,
            HttpServletRequest request) {
        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Observation ID: " + (theId != null ? theId.getIdPart() : "null"));
        try {
            FhirProviderUtils.validateIdParam(theId, "Observation", this.getClass().getSimpleName(), method);

            Result existingResult = resultService.getResultByFhirUuid(theId.getIdPart());
            if (existingResult == null) {
                throw new ResourceNotFoundException("Observation/" + theId.getIdPart());
            }

            if (fhirObservation.hasValueQuantity()) {
                existingResult.setValue(fhirObservation.getValueQuantity().getValue().toPlainString());
                existingResult.setResultType("N");
            } else if (fhirObservation.hasValueStringType()) {
                existingResult.setValue(fhirObservation.getValueStringType().getValue());
                existingResult.setResultType("T");
            }

            existingResult.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Result updatedResult = resultService.save(existingResult);

            Observation resultObservation = fhirTransformService.transformResultToObservation(updatedResult);
            resultObservation.setId(theId);
            fhirTransformService.transformPersistResult(updatedResult);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Observation with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(resultObservation);

        } catch (ResourceNotFoundException | UnprocessableEntityException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error updating observation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error updating Observation");
        }
    }

    @Delete
    public MethodOutcome delete(@IdParam IdType theId, HttpServletRequest request) {
        String method = "delete";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR DELETE request for Observation ID: " + (theId != null ? theId.getIdPart() : "null"));
        try {
            FhirProviderUtils.validateIdParam(theId, "Observation", this.getClass().getSimpleName(), method);

            Result result = resultService.getResultByFhirUuid(theId.getIdPart());
            if (result == null) {
                throw new ResourceNotFoundException("Observation/" + theId.getIdPart());
            }

            result.setSysUserId(FhirProviderUtils.getSysUserId(request));
            resultService.save(result);

            Observation fhirObservation = fhirTransformService.transformResultToObservation(result);
            fhirObservation.setStatus(ObservationStatus.CANCELLED);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, fhirObservation, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Observation with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Observation");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error deleting observation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error deleting Observation");
        }
    }

    @Search
    public Bundle search(
            @OptionalParam(name = Observation.SP_ENCOUNTER, chainWhitelist = { "",
                    Encounter.SP_TYPE }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
            @OptionalParam(name = Observation.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
                    Patient.SP_GIVEN, Patient.SP_FAMILY,
                    Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
            @OptionalParam(name = Observation.SP_HAS_MEMBER, chainWhitelist = { "",
                    Observation.SP_CODE }, targetTypes = Observation.class) ReferenceAndListParam hasMemberReference,
            @OptionalParam(name = Observation.SP_VALUE_CONCEPT) TokenAndListParam valueConcept,
            @OptionalParam(name = Observation.SP_VALUE_DATE) DateRangeParam valueDateParam,
            @OptionalParam(name = Observation.SP_VALUE_QUANTITY) QuantityAndListParam valueQuantityParam,
            @OptionalParam(name = Observation.SP_VALUE_STRING) StringAndListParam valueStringParam,
            @OptionalParam(name = Observation.SP_DATE) DateRangeParam date,
            @OptionalParam(name = Observation.SP_CODE) TokenAndListParam code,
            @OptionalParam(name = Observation.SP_CATEGORY) TokenAndListParam category,
            @OptionalParam(name = Observation.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @OptionalParam(name = Observation.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
                    Patient.SP_GIVEN, Patient.SP_FAMILY,
                    Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientParam,
            @IncludeParam(allow = { "Observation:" + Observation.SP_ENCOUNTER, "Observation:" + Observation.SP_PATIENT,
                    "Observation:" + Observation.SP_HAS_MEMBER }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = { "Observation:" + Observation.SP_HAS_MEMBER,
                    "DiagnosticReport:" + DiagnosticReport.SP_RESULT }) HashSet<Include> revIncludes,
            HttpServletRequest request) {
        String method = "search";
        try {
            return util.forwardSearchToFhirStore(request);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching Observations: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching Observations");
        }
    }
}