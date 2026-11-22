package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.search.service.ServiceRequestSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestProvider implements IResourceProvider {

    /**
     * The sample list of an update, which never adds a test. The edit form
     * validator rejects a missing list as malformed XML, so without it every update
     * that kept the requested test, including an unchanged round trip, answered
     * 400.
     */
    private static final String NO_SAMPLES_ADDED_XML = "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples></samples>";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private ServiceRequestSearchService serviceRequestSearchService;

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
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("ServiceRequest", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading ServiceRequest: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading ServiceRequest", e);
        }
    }

    @Search
    public IBundleProvider searchServiceRequests(@OptionalParam(name = ServiceRequest.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = ServiceRequest.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = ServiceRequest.SP_PATIENT) ReferenceAndListParam patient,
            @OptionalParam(name = ServiceRequest.SP_SUBJECT) ReferenceAndListParam subject,
            @OptionalParam(name = ServiceRequest.SP_REQUESTER) ReferenceAndListParam requester,
            @OptionalParam(name = ServiceRequest.SP_SPECIMEN) ReferenceAndListParam specimen,
            @OptionalParam(name = ServiceRequest.SP_CODE) TokenAndListParam code,
            @OptionalParam(name = ServiceRequest.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.SERVICE_REQUEST_PATIENT_INCLUDE,
                    FhirConstants.SERVICE_REQUEST_SUBJECT_INCLUDE, FhirConstants.SERVICE_REQUEST_REQUESTER_INCLUDE,
                    FhirConstants.SERVICE_REQUEST_SPECIMEN_INCLUDE }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = { FhirConstants.OBSERVATION_BASED_ON_REV_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_BASED_ON_REV_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String method = "searchServiceRequests";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Searching for ServiceRequests");

        try {
            ServiceRequestSearchParams params = new ServiceRequestSearchParams(id, identifier,
                    FhirProviderUtils.merge(patient, subject), requester, specimen, code, status, lastUpdated, sort,
                    includes, revIncludes);
            return FhirProviderUtils.withPaging(serviceRequestSearchService.searchServiceRequests(params), offset,
                    count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Invalid ServiceRequest search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid ServiceRequest search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("ServiceRequest", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching ServiceRequest: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching ServiceRequest");
        }
    }

}
