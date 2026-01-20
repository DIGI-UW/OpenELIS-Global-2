package org.openelisglobal.test.service.fhir;

import org.hl7.fhir.r4.model.ObservationDefinition;
import org.openelisglobal.test.valueholder.Test;

/**
 * Service for transforming OpenELIS Test entities to FHIR ObservationDefinition
 * resources
 * and syncing them to remote FHIR servers (e.g., OpenMRS).
 */
public interface TestFhirTransformService {

    /**
     * Transform an OpenELIS Test to a FHIR R4 ObservationDefinition resource.
     * 
     * @param test the OpenELIS test to transform
     * @return FHIR ObservationDefinition resource
     */
    ObservationDefinition transformTestToObservationDefinition(Test test);

    /**
     * Sync a test to configured remote FHIR servers (e.g., OpenMRS).
     * This method is async and non-blocking.
     * 
     * @param test     the test to sync
     * @param isUpdate true if this is an update, false if creating new
     */
    void syncTestToFhir(Test test, boolean isUpdate);
}
