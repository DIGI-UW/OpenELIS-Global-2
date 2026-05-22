package org.openelisglobal.dataexchange.fhir.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IFetchConformanceTyped;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-739 — the FHIR proxy was concatenating
 * {@code fhirConfig.getLocalFhirStorePath()} (configured with a trailing slash,
 * e.g. {@code https://fhir.openelis.org:8443/fhir/}) with
 * {@code "/" + resourceType}, producing {@code .../fhir//metadata?} which the
 * upstream HAPI server rejects.
 *
 * <p>
 * Locks the trailing-slash normalization in
 * {@link FhirQueryRestController#normalizeFhirBaseUrl(String)} AND the
 * {@code /metadata}-to-{@link CapabilityStatement} routing branch added in the
 * same ticket.
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

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void metadataResourceTypeRoutesToCapabilities_notGenericSearch() {
        // Lock the OGC-739 follow-up: GET /rest/fhir/metadata must NOT go through
        // the generic search-URL builder (which would emit `.../fhir/metadata?`
        // and ClassCastException on the Bundle cast). It MUST call
        // client.capabilities().ofType(CapabilityStatement.class).execute().
        IGenericClient client = mock(IGenericClient.class);
        IFetchConformanceUntyped fetchUntyped = mock(IFetchConformanceUntyped.class);
        IFetchConformanceTyped fetchTyped = mock(IFetchConformanceTyped.class);
        CapabilityStatement expected = new CapabilityStatement();
        expected.setId("test-caps");

        when(client.capabilities()).thenReturn(fetchUntyped);
        when(fetchUntyped.ofType(CapabilityStatement.class)).thenReturn(fetchTyped);
        when(fetchTyped.execute()).thenReturn(expected);

        FhirConfig fhirConfig = mock(FhirConfig.class);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("https://fhir.example/r4");
        FhirUtil fhirUtil = mock(FhirUtil.class);
        when(fhirUtil.getLocalFhirClient()).thenReturn(client);

        FhirQueryRestController controller = new FhirQueryRestController();
        ReflectionTestUtils.setField(controller, "fhirUtil", fhirUtil);
        ReflectionTestUtils.setField(controller, "fhirConfig", fhirConfig);

        HttpServletRequest request = mock(HttpServletRequest.class);
        ResponseEntity<?> response = controller.queryFhirResources("metadata", null, false, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame("capabilities() result must flow back unmodified", expected, response.getBody());
        verify(client).capabilities();
        verify(client, never()).search();
    }
}
