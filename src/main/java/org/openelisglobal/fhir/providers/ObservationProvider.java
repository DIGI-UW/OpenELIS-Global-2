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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ObservationSearchParams;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.search.service.ObservationSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Read and search Observation resources from OpenELIS. */
@Component
public class ObservationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationSearchService observationSearchService;

    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
    }

    @Read
    public Observation read(@IdParam IdType id) {
        String method = "read";
        try {
            String uuid = FhirProviderUtils.requireUuidId(id, "Observation").toString();

            Result result = resultService.getResultByFhirUuid(uuid);
            if (result == null) {
                throw new ResourceNotFoundException("Observation not found: " + uuid);
            }

            Observation observation = fhirTransformService.transformResultToObservation(result);
            if (observation == null) {
                throw new ResourceNotFoundException("Failed to transform result to observation: " + uuid);
            }

            return observation;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Observation", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error reading observation: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error retrieving Observation");
        }
    }

    @Search
    public IBundleProvider searchObservations(@OptionalParam(name = Observation.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = Observation.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Observation.SP_PATIENT) ReferenceAndListParam patient,
            @OptionalParam(name = Observation.SP_SUBJECT) ReferenceAndListParam subject,
            @OptionalParam(name = Observation.SP_BASED_ON) ReferenceAndListParam basedOn,
            @OptionalParam(name = Observation.SP_SPECIMEN) ReferenceAndListParam specimen,
            @OptionalParam(name = Observation.SP_CODE) TokenAndListParam code,
            @OptionalParam(name = Observation.SP_STATUS) TokenAndListParam status,
            @OptionalParam(name = Observation.SP_DATE) DateRangeParam date,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.OBSERVATION_PATIENT_INCLUDE,
                    FhirConstants.OBSERVATION_SUBJECT_INCLUDE, FhirConstants.OBSERVATION_BASED_ON_INCLUDE,
                    FhirConstants.OBSERVATION_SPECIMEN_INCLUDE,
                    FhirConstants.OBSERVATION_PERFORMER_INCLUDE }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = {
                    FhirConstants.DIAGNOSTIC_REPORT_RESULT_REV_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String method = "searchObservations";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Searching for Observations");

        try {
            ObservationSearchParams params = new ObservationSearchParams(id, identifier,
                    FhirProviderUtils.merge(patient, subject), basedOn, specimen, code, status, date, lastUpdated, sort,
                    includes, revIncludes);
            return FhirProviderUtils.withPaging(observationSearchService.searchObservations(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Invalid Observation search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Observation search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Observation", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Error searching Observations: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching Observations");
        }
    }

}