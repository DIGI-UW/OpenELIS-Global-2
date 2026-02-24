package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.result.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            // Re-throw as-is — HAPI maps this to 404 correctly
            throw e;
        } catch (Exception e) {
            // Log only unexpected errors (e.g., DB failure, NPE)
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error reading observation: " + e.getMessage());
            throw new ResourceNotFoundException("Observation not found: " + (id != null ? id.getIdPart() : "null"));
        }
    }
}