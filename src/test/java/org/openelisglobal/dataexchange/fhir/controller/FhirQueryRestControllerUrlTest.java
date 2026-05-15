package org.openelisglobal.dataexchange.fhir.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * OGC-739 — the FHIR proxy was concatenating
 * {@code fhirConfig.getLocalFhirStorePath()} (configured with a trailing slash,
 * e.g. {@code https://fhir.openelis.org:8443/fhir/}) with
 * {@code "/" + resourceType}, producing {@code .../fhir//metadata?} which the
 * upstream HAPI server rejects.
 *
 * <p>
 * Locks the trailing-slash normalization in
 * {@link FhirQueryRestController#normalizeFhirBaseUrl(String)}.
 */
public class FhirQueryRestControllerUrlTest {

    @Test
    public void stripsTrailingSlashFromConfiguredBaseUrl() {
        assertEquals("https://fhir.openelis.org:8443/fhir",
                FhirQueryRestController.normalizeFhirBaseUrl("https://fhir.openelis.org:8443/fhir/"));
    }

    @Test
    public void leavesBaseUrlAloneWhenNoTrailingSlash() {
        assertEquals("https://fhir.openelis.org:8443/fhir",
                FhirQueryRestController.normalizeFhirBaseUrl("https://fhir.openelis.org:8443/fhir"));
    }

    @Test
    public void returnsNullUnchanged() {
        assertNull(FhirQueryRestController.normalizeFhirBaseUrl(null));
    }

    @Test
    public void returnsEmptyUnchanged() {
        assertEquals("", FhirQueryRestController.normalizeFhirBaseUrl(""));
    }
}
