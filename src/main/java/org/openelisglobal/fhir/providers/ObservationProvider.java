package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.result.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Resource Provider for Observation resources.
 *
 * <p>
 * Exposes lab results from OpenELIS directly via the native FHIR facade. Read
 * queries OpenELIS DB directly for consistency with the source of truth. Search
 * forwards to the HAPI FHIR store to support the full FHIR search parameter
 * set.
 *
 * <p>
 * Supported operations:
 * <ul>
 * <li>READ: GET /fhir/Observation/{uuid}</li>
 * <li>SEARCH: GET /fhir/Observation?patient={uuid}&amp;...</li>
 * </ul>
 */
@Component
public class ObservationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private FhirUtil util;

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

            org.openelisglobal.result.valueholder.Result result = resultService.getResultByFhirUuid(uuid);
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