package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * FHIR R4 Resource Provider for Observation resources.
 *
 * <p>
 * Exposes lab results from OpenELIS directly via the native FHIR facade,
 * querying the OpenELIS database without proxying to the external HAPI FHIR
 * store. This ensures Observation data is always consistent with the source of
 * truth in OpenELIS.
 *
 * <p>
 * Supported operations:
 * <ul>
 * <li>READ: GET /fhir/Observation/{uuid}</li>
 * <li>SEARCH: GET /fhir/Observation?patient={uuid}</li>
 * </ul>
 */
@Component
public class ObservationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private ResultService resultService;

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

            // Verify Result exists with FHIR UUID
            org.openelisglobal.result.valueholder.Result result = resultService.getResultByFhirUuid(uuid);
            if (result == null) {
                throw new ResourceNotFoundException("Observation not found: " + uuid);
            }

            // Use existing transformation logic
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
    public List<Observation> search(@OptionalParam(name = Observation.SP_PATIENT) ReferenceParam patient) {
        String method = "search";
        try {
            if (patient == null) {
                return Collections.emptyList();
            }
            String patientUuid = patient.getIdPart();
            List<Result> results = resultService.getResultsByPatientUuid(patientUuid);
            if (results == null) {
                return Collections.emptyList();
            }
            return results.stream().map(result -> {
                try {
                    return fhirTransformService.transformResultToObservation(result);
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getSimpleName(), method,
                            "Error transforming result: " + e.getMessage());
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error searching observations: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error searching Observations");
        }
    }
}