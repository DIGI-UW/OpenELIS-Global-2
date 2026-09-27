package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.PractitionerProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Practitioner reverse includes over result-facade.xml: provider 1 requested
 * analysis 1 (result 3); analysis 2's sample has no provider.
 */
public class PractitionerRevIncludeFacadeTest extends BaseWebContextSensitiveTest {

    private static final String PROVIDER_1_UUID = "550e8400-e29b-41d4-a716-446655441004";
    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";

    @Autowired
    private PractitionerProvider practitionerProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(practitionerProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void revIncludeObservationPerformer_addsTheProvidersResults() throws Exception {
        JsonNode bundle = search(PROVIDER_1_UUID, "Observation:performer");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(PROVIDER_1_UUID), idsOf(bundle, "Practitioner"));
        assertEquals(List.of(RESULT_3_UUID), idsOf(bundle, "Observation"));
        assertEquals("match", bundle.get("entry").get(0).get("search").get("mode").asText());
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());
        assertEquals("Practitioner/" + PROVIDER_1_UUID,
                bundle.get("entry").get(1).get("resource").get("performer").get(0).get("reference").asText());
    }

    @Test
    public void revIncludeServiceRequestRequester_addsTheProvidersOrders() throws Exception {
        JsonNode bundle = search(PROVIDER_1_UUID, "ServiceRequest:requester");
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));
    }

    @Test
    public void lastUpdated_isSearchableAndExposed() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");
        request.setQueryString("_lastUpdated=ge2023-11-01");
        request.addParameter("_lastUpdated", "ge2023-11-01");
        JsonNode bundle = serve(request);
        assertTrue(idsOf(bundle, "Practitioner").contains(PROVIDER_1_UUID));
        assertTrue(bundle.get("entry").get(0).get("resource").get("meta").get("lastUpdated").asText()
                .startsWith("2023-11-01"));

        request = buildFhirRequest("GET", "/Practitioner");
        request.setQueryString("_lastUpdated=lt2023-11-01");
        request.addParameter("_lastUpdated", "lt2023-11-01");
        assertEquals(0, serve(request).get("total").asInt());
    }

    private JsonNode search(String id, String revInclude) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Practitioner");
        request.setQueryString("_id=" + id + "&_revinclude=" + revInclude);
        request.addParameter("_id", id);
        request.addParameter("_revinclude", revInclude);
        return serve(request);
    }

    private JsonNode serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private static List<String> idsOf(JsonNode bundle, String resourceType) {
        List<String> ids = new ArrayList<>();
        JsonNode entries = bundle.get("entry");
        if (entries == null) {
            return ids;
        }
        for (JsonNode entry : entries) {
            JsonNode resource = entry.get("resource");
            if (resourceType.equals(resource.get("resourceType").asText())) {
                ids.add(resource.get("id").asText());
            }
        }
        return ids;
    }
}
