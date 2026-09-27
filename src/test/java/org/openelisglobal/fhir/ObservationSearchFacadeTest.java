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
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Observation search answered from the OpenELIS database (result-facade.xml:
 * result 3 on analysis 1, technically accepted, patient 1 / provider 1 / sample
 * item 601; result 4 on analysis 2, finalized, patient 2 / sample item 602;
 * both analyses released on 2025-07-07).
 */
public class ObservationSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";
    private static final String RESULT_4_UUID = "550e8400-e29b-41d4-a716-446655440004";
    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String ANALYSIS_2_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
    private static final String PATIENT_1_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PROVIDER_1_UUID = "550e8400-e29b-41d4-a716-446655441004";
    private static final String SAMPLE_ITEM_601_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";

    @Autowired
    private ObservationProvider observationProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        resyncSequence("clinlims.result_seq", "clinlims.result");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(observationProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_byIdPatientOrderAndSpecimen() throws Exception {
        assertEquals(2, search().get("total").asInt());
        assertEquals(List.of(RESULT_4_UUID), idsOf(search("_id", RESULT_4_UUID), "Observation"));
        assertEquals(List.of(RESULT_3_UUID), idsOf(search("patient", "Patient/" + PATIENT_1_UUID), "Observation"));
        assertEquals(List.of(RESULT_3_UUID), idsOf(search("subject", PATIENT_1_UUID), "Observation"));
        assertEquals(List.of(RESULT_3_UUID),
                idsOf(search("based-on", "ServiceRequest/" + ANALYSIS_1_UUID), "Observation"));
        assertEquals(List.of(RESULT_3_UUID),
                idsOf(search("specimen", "Specimen/" + SAMPLE_ITEM_601_UUID), "Observation"));
        assertEquals(0, search("based-on", "00000000-0000-0000-0000-000000000000").get("total").asInt());
    }

    @Test
    public void search_byCodeStatusAndDate() throws Exception {
        assertEquals(2, search("code", "http://loinc.org|123456").get("total").asInt());
        assertEquals(0, search("code", "http://loinc.org|000000").get("total").asInt());
        assertEquals(List.of(RESULT_4_UUID), idsOf(search("status", "final"), "Observation"));
        assertEquals(List.of(RESULT_3_UUID), idsOf(search("status", "preliminary"), "Observation"));
        assertEquals(0, search("status", "cancelled").get("total").asInt());
        assertEquals(2, search("date", "2025-07-07").get("total").asInt());
        assertEquals(2, search("date", "ge2025-07-01").get("total").asInt());
        assertEquals(0, search("date", "lt2025-07-07").get("total").asInt());
        assertEquals(0, search("date", "gt2025-07-07").get("total").asInt());
    }

    @Test
    public void search_includes_addLinkedResources() throws Exception {
        JsonNode bundle = search(RESULT_3_UUID, "_include", "Observation:patient");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(PATIENT_1_UUID), idsOf(bundle, "Patient"));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());

        bundle = search(RESULT_3_UUID, "_include", "Observation:based-on");
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));

        bundle = search(RESULT_3_UUID, "_include", "Observation:specimen");
        assertEquals(List.of(SAMPLE_ITEM_601_UUID), idsOf(bundle, "Specimen"));

        bundle = search(RESULT_3_UUID, "_include", "Observation:performer");
        assertEquals(List.of(PROVIDER_1_UUID), idsOf(bundle, "Practitioner"));

        bundle = search(RESULT_4_UUID, "_revinclude", "DiagnosticReport:result");
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(bundle, "DiagnosticReport"));
    }

    @Test
    public void search_returnsTransformedObservations() throws Exception {
        JsonNode resource = search("_id", RESULT_4_UUID).get("entry").get(0).get("resource");
        assertEquals("Observation", resource.get("resourceType").asText());
        assertEquals("final", resource.get("status").asText());
        assertTrue(resource.has("valueQuantity"));
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Observation"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode search(String id, String includeParam, String includeValue) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");
        request.setQueryString("_id=" + id + "&" + includeParam + "=" + includeValue);
        request.addParameter("_id", id);
        request.addParameter(includeParam, includeValue);
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

    @Test
    public void search_byLastUpdated_andExposesMetaLastUpdated() throws Exception {
        assertEquals(List.of(RESULT_4_UUID), idsOf(search("_lastUpdated", "ge2025-05-01"), "Observation"));
        assertEquals(List.of(RESULT_3_UUID), idsOf(search("_lastUpdated", "lt2025-05-01"), "Observation"));
        assertEquals(2, search("_lastUpdated", "ge2025-01-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2025-01-01").get("total").asInt());
        JsonNode resource = search("_id", RESULT_3_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2025-04-04"));
    }
}
