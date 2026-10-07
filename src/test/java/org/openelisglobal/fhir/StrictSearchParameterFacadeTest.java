package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.SearchPreferHandlingInterceptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.servlets.StrictSearchParameterInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Every resource the facade serves answers a search it cannot honour with 400
 * and an OperationOutcome, instead of dropping the parameter and returning
 * every resource of the type.
 */
public class StrictSearchParameterFacadeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ApplicationContext applicationContext;

    private RestfulServer fhirServlet;
    private List<String> resourceTypes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        FhirContext fhirContext = FhirContext.forR4();
        List<IResourceProvider> providers = new ArrayList<>(
                applicationContext.getBeansOfType(IResourceProvider.class).values());
        fhirServlet = new RestfulServer(fhirContext);
        fhirServlet.setResourceProviders(providers);
        fhirServlet.registerInterceptor(new SearchPreferHandlingInterceptor());
        fhirServlet.registerInterceptor(new StrictSearchParameterInterceptor());
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        TreeMap<String, IResourceProvider> byType = new TreeMap<>();
        providers.forEach(provider -> byType.put(fhirContext.getResourceType(provider.getResourceType()), provider));
        resourceTypes = new ArrayList<>(byType.keySet());
    }

    @Test
    public void everyResourceIsCovered() {
        assertTrue(resourceTypes.toString(),
                resourceTypes.containsAll(List.of("Patient", "Practitioner", "Organization", "Location", "Device",
                        "Specimen", "ServiceRequest", "Observation", "DiagnosticReport")));
    }

    @Test
    public void unknownUnderscoreParameter_isRejectedOnEveryResource() throws Exception {
        for (String type : resourceTypes) {
            assertRejected(type, "_foo=bar", "Unknown search parameter '_foo'");
            assertRejected(type, "_has=Observation:patient:code=1", "Unknown search parameter '_has'");
            assertRejected(type, "_text=anything", "Unknown search parameter '_text'");
        }
    }

    @Test
    public void unknownParameter_isRejectedOnEveryResource() throws Exception {
        for (String type : resourceTypes) {
            assertEquals(type, 400, serve(type, "nonsense=1", false).getStatus());
        }
    }

    @Test
    public void sort_isRejectedOnEveryResourceBecauseNoDaoAppliesIt() throws Exception {
        for (String type : resourceTypes) {
            assertRejected(type, "_sort=_lastUpdated", "_sort parameter is not supported");
        }
    }

    @Test
    public void missingModifier_isRejectedOnEveryResource() throws Exception {
        for (String type : resourceTypes) {
            assertRejected(type, "_lastUpdated:missing=true", "Modifier ':missing' is not supported");
        }
    }

    @Test
    public void chainedParameter_isRejected() throws Exception {
        assertRejected("ServiceRequest", "subject.name=Doe", "Chained search parameter 'subject.name'");
        assertRejected("Observation", "patient.family=Doe", "Chained search parameter 'patient.family'");
    }

    @Test
    public void unsupportedDatePrefix_isRejectedOnEveryResource() throws Exception {
        for (String type : resourceTypes) {
            assertRejected(type, "_lastUpdated=ne2023-11-01", "Date prefix 'ne' is not supported");
        }
    }

    @Test
    public void tokenWithSystemAndNoCode_isRejected() throws Exception {
        assertRejected("Patient", "identifier=http://openelis-global.org/pat_nationalId%7C",
                "searching by system alone");
        assertRejected("Patient", "identifier=%7C", "has neither a system nor a code");
        assertRejected("Observation", "code=http://loinc.org%7C", "searching by system alone");
    }

    @Test
    public void tokenModifier_isRejected() throws Exception {
        assertRejected("Patient", "gender:text=male", "Modifier ':text' is not supported");
        assertRejected("ServiceRequest", "status:not=active", "");
    }

    @Test
    public void supportedSearches_stillSucceed() throws Exception {
        for (String type : resourceTypes) {
            assertEquals(type, 200, serve(type, "_lastUpdated=ge2000-01-01&_count=1", false).getStatus());
            assertEquals(type, 200, serve(type, "_lastUpdated=&_sort=&_summary=count", false).getStatus());
        }
        assertEquals(200, serve("Patient", "family:exact=Doe&name:contains=oh", false).getStatus());
        assertEquals(200, serve("Patient", "phone=phone%7C", false).getStatus());
        assertEquals(200,
                serve("Patient", "identifier=http://openelis-global.org/pat_nationalId%7C1234", false).getStatus());
    }

    @Test
    public void lenientHandling_restoresTheOldIgnoringBehaviour() throws Exception {
        for (String type : resourceTypes) {
            assertEquals(type, 200, serve(type, "_foo=bar&_sort=_lastUpdated", true).getStatus());
            assertEquals(type, 200, serve(type, "nonsense=1", true).getStatus());
        }
    }

    private void assertRejected(String type, String query, String expectedDiagnostics) throws Exception {
        MockHttpServletResponse response = serve(type, query, false);
        String body = response.getContentAsString();
        assertEquals(type + "?" + query + " -> " + body, 400, response.getStatus());
        JsonNode outcome = objectMapper.readTree(body);
        assertEquals("OperationOutcome", outcome.get("resourceType").asText());
        String diagnostics = outcome.get("issue").get(0).get("diagnostics").asText();
        assertTrue(type + "?" + query + " -> " + diagnostics, diagnostics.contains(expectedDiagnostics));
        assertFalse(body.contains("\"resourceType\":\"Bundle\""));
    }

    private MockHttpServletResponse serve(String type, String query, boolean lenient) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/" + type);
        request.setQueryString(query);
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            request.addParameter(pair.substring(0, equals),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
        }
        if (lenient) {
            request.addHeader("Prefer", "handling=lenient");
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }
}
