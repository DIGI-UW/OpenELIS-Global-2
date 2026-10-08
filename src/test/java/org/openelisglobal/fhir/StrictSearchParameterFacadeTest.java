package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
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
    public void codeOutsideTheRequiredValueSet_isRejected() throws Exception {
        assertRejected("Patient", "gender=xyz", "'xyz' is not a valid code for search parameter 'gender'");
        assertRejected("Patient", "gender=male,M", "'M' is not a valid code");
        assertRejected("ServiceRequest", "status=bogus", "not a valid code for search parameter 'status'");
        assertRejected("Specimen", "status=bogus", "not a valid code");
        assertRejected("Observation", "status=bogus", "not a valid code");
        assertRejected("DiagnosticReport", "status=bogus", "not a valid code");
        assertRejected("Location", "status=bogus", "not a valid code");
        assertRejected("Device", "status=bogus", "not a valid code");
        assertRejected("Organization", "active=maybe", "not a valid code");
    }

    @Test
    public void codeInsideTheRequiredValueSet_stillSucceeds() throws Exception {
        for (String query : List.of("Patient?gender=male,female,other,unknown",
                "Patient?gender=http://hl7.org/fhir/administrative-gender%7Cfemale", "ServiceRequest?status=active",
                "ServiceRequest?status=completed,revoked", "Specimen?status=available", "Observation?status=final",
                "DiagnosticReport?status=final,preliminary", "Location?status=active", "Device?status=active",
                "Organization?active=true", "Organization?active=false")) {
            String[] parts = query.split("\\?");
            assertEquals(query, 200, serve(parts[0], parts[1], false).getStatus());
        }
    }

    @Test
    public void referenceToATypeTheParameterCannotHold_isRejected() throws Exception {
        assertRejected("ServiceRequest", "patient=Organization/1", "cannot reference a Organization");
        assertRejected("ServiceRequest", "subject=Foo/1", "'Foo', which is not a FHIR resource type");
        assertRejected("DiagnosticReport", "result=Patient/1", "cannot reference a Patient");
        assertRejected("Organization", "partof=Patient/1", "cannot reference a Patient");
        assertRejected("Specimen", "patient=Practitioner/1", "cannot reference a Practitioner");
    }

    @Test
    public void referenceWithoutAnId_isRejectedInsteadOfMatchingEverything() throws Exception {
        assertRejected("ServiceRequest", "subject=Patient/", "names no id");
        assertRejected("Observation", "patient=Patient/", "names no id");
    }

    @Test
    public void wellFormedReferences_stillSucceed() throws Exception {
        for (String query : List.of("ServiceRequest?subject=Patient/b479ab79-5f53-4d1f-bc9b-10f19ce04635",
                "ServiceRequest?patient=b479ab79-5f53-4d1f-bc9b-10f19ce04635",
                "ServiceRequest?requester=Practitioner/1",
                "ServiceRequest?subject=http://example.org/fhir/Patient/b479ab79-5f53-4d1f-bc9b-10f19ce04635",
                "DiagnosticReport?result=Observation/b479ab79-5f53-4d1f-bc9b-10f19ce04635",
                "Organization?partof=Organization/1")) {
            String[] parts = query.split("\\?");
            assertEquals(query, 200, serve(parts[0], parts[1], false).getStatus());
        }
    }

    @Test
    public void malformedTokenAndNegativeCount_areRejected() throws Exception {
        assertRejected("ServiceRequest", "code=a%7Cb%7Cc", "malformed token");
        assertRejected("Patient", "_count=-1", "_count must be zero or greater");
    }

    @Test
    public void readWithMalformedId_isAClientErrorOnEveryResource() throws Exception {
        for (String type : resourceTypes) {
            MockHttpServletRequest request = buildFhirRequest("GET", "/" + type + "/not-a-uuid");
            MockHttpServletResponse response = new MockHttpServletResponse();
            fhirServlet.service(request, response);
            assertTrue(type + " -> " + response.getStatus() + " " + response.getContentAsString(),
                    response.getStatus() == 400 || response.getStatus() == 404);
        }
        for (String type : List.of("Patient", "Practitioner", "Organization", "Observation")) {
            MockHttpServletRequest request = buildFhirRequest("GET", "/" + type + "/not-a-uuid");
            MockHttpServletResponse response = new MockHttpServletResponse();
            fhirServlet.service(request, response);
            assertEquals(type, 400, response.getStatus());
            assertTrue(response.getContentAsString().contains(type + " ID must be a valid UUID"));
        }
    }

    @Test
    public void offsetWithoutCount_pagesEveryResourceAndANegativeOffsetIsAClientError() throws Exception {
        for (String type : resourceTypes) {
            assertEquals(type, 200, serve(type, "_offset=0", false).getStatus());
            assertEquals(type, 200, serve(type, "_offset=3", false).getStatus());
            assertEquals(type, 400, serve(type, "_offset=-1", false).getStatus());
        }
    }

    @Test
    public void updateAndDeleteWithMalformedId_areClientErrors() throws Exception {
        for (String type : List.of("Patient", "Practitioner", "Organization", "ServiceRequest", "Observation")) {
            MockHttpServletRequest delete = buildFhirRequest("DELETE", "/" + type + "/not-a-uuid");
            MockHttpServletResponse deleted = new MockHttpServletResponse();
            fhirServlet.service(delete, deleted);
            assertEquals(type + " DELETE -> " + deleted.getContentAsString(), 400, deleted.getStatus());
        }
        for (String type : List.of("Patient", "Practitioner", "Organization")) {
            MockHttpServletRequest put = buildFhirRequest("PUT", "/" + type + "/not-a-uuid");
            put.setContent(("{\"resourceType\":\"" + type + "\",\"id\":\"not-a-uuid\"}").getBytes());
            MockHttpServletResponse updated = new MockHttpServletResponse();
            fhirServlet.service(put, updated);
            assertEquals(type + " PUT -> " + updated.getContentAsString(), 400, updated.getStatus());
        }
        String unknown = "00000000-0000-0000-0000-000000000000";
        MockHttpServletRequest put = buildFhirRequest("PUT", "/Practitioner/" + unknown);
        put.setContent(("{\"resourceType\":\"Practitioner\",\"id\":\"" + unknown + "\"}").getBytes());
        MockHttpServletResponse updated = new MockHttpServletResponse();
        fhirServlet.service(put, updated);
        assertEquals(updated.getContentAsString(), 404, updated.getStatus());
    }

    @Test
    public void lenientHandling_restoresTheOldIgnoringBehaviour() throws Exception {
        for (String type : resourceTypes) {
            assertEquals(type, 200, serve(type, "_foo=bar&_sort=_lastUpdated", true).getStatus());
            assertEquals(type, 200, serve(type, "nonsense=1", true).getStatus());
            assertEquals(type, 200, serve(type, "gender:missing=true&subject.name=x&_has=x", true).getStatus());
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
