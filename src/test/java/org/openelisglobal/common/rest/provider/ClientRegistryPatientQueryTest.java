package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;

/**
 * OGC-1401 (IG Known Issue 28): the client registry is searched with the
 * identifier systems OpenELIS publishes patients under, built from the
 * configured base. The systems were hard-coded, and lower-cased
 * ("pat_nationalid"), so no published patient could match.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ClientRegistryPatientQueryTest {

    private static final String SITE_SYSTEM = "https://lab.example.org/fhir";

    @Mock
    private FhirConfig fhirConfig;

    @InjectMocks
    private PatientSearchRestController controller;

    private String searchUrl(String stNumber, String subjectNumber, String nationalId) {
        when(fhirConfig.getOeFhirSystem()).thenReturn(SITE_SYSTEM);
        FhirContext fhirContext = FhirContext.forR4();
        fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        IGenericClient registry = fhirContext.newRestfulGenericClient("http://registry.invalid/fhir");
        StringBuilder url = new StringBuilder();
        registry.registerInterceptor(new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest request) {
                url.append(request.getUri());
                throw new IllegalStateException("captured");
            }

            @Override
            public void interceptResponse(IHttpResponse response) {
            }
        });
        try {
            controller.buildPatientSearchQuery(registry, null, null, stNumber, subjectNumber, nationalId, null, null, null)
                    .returnBundle(Bundle.class).execute();
        } catch (FhirClientConnectionException e) {
        }
        return URLDecoder.decode(url.toString(), StandardCharsets.UTF_8);
    }

    @Test
    public void searchesTheNationalIdUnderThePublishedSystem() {
        String url = searchUrl(null, null, "NID-1401");

        assertTrue(url, url.contains("identifier=" + SITE_SYSTEM + "/pat_nationalId|NID-1401"));
    }

    @Test
    public void searchesStAndSubjectNumbersUnderThePublishedSystems() {
        String url = searchUrl("ST-1401", "SUBJ-1401", null);

        assertTrue(url, url.contains(SITE_SYSTEM + "/pat_stNumber|ST-1401"));
        assertTrue(url, url.contains(SITE_SYSTEM + "/pat_subjectNumber|SUBJ-1401"));
    }
}
