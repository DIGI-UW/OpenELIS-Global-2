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
import org.openelisglobal.fhir.providers.ServiceRequestProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * ServiceRequest search answered from the OpenELIS database (result-facade.xml:
 * analysis 1, technically accepted, for patient 1 / provider 1 / sample item
 * 601 with result 3; analysis 2, finalized, for patient 2 / sample item 602).
 */
public class ServiceRequestSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String ANALYSIS_2_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
    private static final String PATIENT_1_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PROVIDER_1_UUID = "550e8400-e29b-41d4-a716-446655441004";
    private static final String SAMPLE_ITEM_601_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";

    @Autowired
    private ServiceRequestProvider serviceRequestProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        resyncSequence("clinlims.result_seq", "clinlims.result");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(serviceRequestProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_byIdAndIdentifiers() throws Exception {
        assertEquals(2, search().get("total").asInt());
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(search("_id", ANALYSIS_2_UUID), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(
                search("identifier", "http://openelis-global.org/analysis_uuid|" + ANALYSIS_1_UUID), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(search("identifier", "DEV01260000000000001"), "ServiceRequest"));
        assertEquals(0, search("identifier", "http://example.org/other|DEV01260000000000001").get("total").asInt());
    }

    @Test
    public void search_byPatientRequesterAndSpecimen() throws Exception {
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(search("patient", "Patient/" + PATIENT_1_UUID), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(search("subject", PATIENT_1_UUID), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID),
                idsOf(search("requester", "Practitioner/" + PROVIDER_1_UUID), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID),
                idsOf(search("specimen", "Specimen/" + SAMPLE_ITEM_601_UUID), "ServiceRequest"));
        assertEquals(0, search("patient", "00000000-0000-0000-0000-000000000000").get("total").asInt());
    }

    @Test
    public void search_byCodeAndStatus() throws Exception {
        assertEquals(2, search("code", "http://loinc.org|123456").get("total").asInt());
        assertEquals(2, search("code", "123456").get("total").asInt());
        assertEquals(2, search("code", "complete blood count").get("total").asInt());
        assertEquals(2, search("code", "blood test").get("total").asInt());
        assertEquals(0, search("code", "http://loinc.org|999999").get("total").asInt());
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(search("status", "completed"), "ServiceRequest"));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(search("status", "active"), "ServiceRequest"));
        assertEquals(2, search("status", "active,completed").get("total").asInt());
        assertEquals(0, search("status", "revoked").get("total").asInt());
        assertEquals(0, search("status", "draft").get("total").asInt());
    }

    @Test
    public void search_includes_addLinkedResources() throws Exception {
        JsonNode bundle = search(ANALYSIS_1_UUID, "_include", "ServiceRequest:patient");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(PATIENT_1_UUID), idsOf(bundle, "Patient"));

        bundle = search(ANALYSIS_1_UUID, "_include", "ServiceRequest:requester");
        assertEquals(List.of(PROVIDER_1_UUID), idsOf(bundle, "Practitioner"));

        bundle = search(ANALYSIS_1_UUID, "_include", "ServiceRequest:specimen");
        assertEquals(List.of(SAMPLE_ITEM_601_UUID), idsOf(bundle, "Specimen"));

        bundle = search(ANALYSIS_1_UUID, "_revinclude", "Observation:based-on");
        assertEquals(List.of(RESULT_3_UUID), idsOf(bundle, "Observation"));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());

        bundle = search(ANALYSIS_2_UUID, "_revinclude", "DiagnosticReport:based-on");
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(bundle, "DiagnosticReport"));
    }

    @Test
    public void search_returnsTransformedServiceRequests() throws Exception {
        JsonNode resource = search("_id", ANALYSIS_2_UUID).get("entry").get(0).get("resource");
        assertEquals("ServiceRequest", resource.get("resourceType").asText());
        assertEquals("completed", resource.get("status").asText());
        assertTrue(resource.get("code").get("coding").size() >= 1);
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/ServiceRequest"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode search(String id, String includeParam, String includeValue) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/ServiceRequest");
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
        assertEquals(2, search("_lastUpdated", "ge2025-07-07").get("total").asInt());
        assertEquals(2, search("_lastUpdated", "2025-07-07").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2025-07-07").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "gt2025-07-08").get("total").asInt());
        JsonNode resource = search("_id", ANALYSIS_1_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2025-07-07"));
    }
}
