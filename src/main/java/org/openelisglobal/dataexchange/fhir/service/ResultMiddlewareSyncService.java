package org.openelisglobal.dataexchange.fhir.service;

import org.hl7.fhir.r4.model.Bundle;

/**
 * Service responsible for publishing validated result FHIR Bundles to external middleware.
 *
 * This is intentionally kept small and config-driven so that it can be safely
 * disabled by default and enabled only in environments that integrate with
 * Nidan or other middleware components.
 */
public interface ResultMiddlewareSyncService {

    /**
     * Publish a FHIR Bundle containing validated results to the configured
     * middleware endpoint. Implementations must be best-effort only and must not
     * throw exceptions that would interfere with core ELIS workflows.
     *
     * @param bundle FHIR Bundle with DiagnosticReport/Observation/etc. for validated results
     */
    void sendValidatedResultsBundle(Bundle bundle);
}
