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
import org.openelisglobal.fhir.providers.DiagnosticReportProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * DiagnosticReport search answered from the OpenELIS database
 * (result-facade.xml: analysis 1, technically accepted, patient 1 / sample item
 * 601 / result 3; analysis 2, finalized, patient 2 / sample item 602 / result
 * 4; both released on 2025-07-07).
 */
public class DiagnosticReportSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String ANALYSIS_2_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
    private static final String PATIENT_1_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PATIENT_2_UUID = "550e8400-e29b-41d4-a716-446655440004";
    private static final String SAMPLE_ITEM_601_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";
    private static final String RESULT_4_UUID = "550e8400-e29b-41d4-a716-446655440004";

    @Autowired
    private DiagnosticReportProvider diagnosticReportProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        resyncSequence("clinlims.result_seq", "clinlims.result");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(diagnosticReportProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_byIdPatientOrderResultAndSpecimen() throws Exception {
        assertEquals(2, search().get("total").asInt());
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(search("_id", ANALYSIS_2_UUID), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_1_UUID),
                idsOf(search("patient", "Patient/" + PATIENT_1_UUID), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(search("subject", PATIENT_2_UUID), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_1_UUID),
                idsOf(search("based-on", "ServiceRequest/" + ANALYSIS_1_UUID), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_2_UUID),
                idsOf(search("result", "Observation/" + RESULT_4_UUID), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_1_UUID),
                idsOf(search("specimen", "Specimen/" + SAMPLE_ITEM_601_UUID), "DiagnosticReport"));
        assertEquals(0, search("result", "00000000-0000-0000-0000-000000000000").get("total").asInt());
    }

    @Test
    public void search_byCodeStatusAndIssued() throws Exception {
        assertEquals(2, search("code", "http://loinc.org|123456").get("total").asInt());
        assertEquals(0, search("code", "http://loinc.org|000000").get("total").asInt());
        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(search("status", "final"), "DiagnosticReport"));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(search("status", "preliminary"), "DiagnosticReport"));
        assertEquals(0, search("status", "partial").get("total").asInt());
        assertEquals(2, search("issued", "2025-07-07").get("total").asInt());
        assertEquals(0, search("issued", "lt2025-07-07").get("total").asInt());
    }

    @Test
    public void search_includes_addLinkedResources() throws Exception {
        JsonNode bundle = searchWithInclude(ANALYSIS_1_UUID, "DiagnosticReport:patient");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(PATIENT_1_UUID), idsOf(bundle, "Patient"));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());

        bundle = searchWithInclude(ANALYSIS_1_UUID, "DiagnosticReport:based-on");
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));

        bundle = searchWithInclude(ANALYSIS_1_UUID, "DiagnosticReport:result");
        assertEquals(List.of(RESULT_3_UUID), idsOf(bundle, "Observation"));

        bundle = searchWithInclude(ANALYSIS_1_UUID, "DiagnosticReport:specimen");
        assertEquals(List.of(SAMPLE_ITEM_601_UUID), idsOf(bundle, "Specimen"));
    }

    @Test
    public void search_returnsTransformedReports() throws Exception {
        JsonNode resource = search("_id", ANALYSIS_2_UUID).get("entry").get(0).get("resource");
        assertEquals("DiagnosticReport", resource.get("resourceType").asText());
        assertEquals("final", resource.get("status").asText());
        assertEquals(1, resource.get("result").size());
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/DiagnosticReport"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode searchWithInclude(String id, String include) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport");
        request.setQueryString("_id=" + id + "&_include=" + include);
        request.addParameter("_id", id);
        request.addParameter("_include", include);
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
        assertEquals(0, search("_lastUpdated", "lt2025-07-07").get("total").asInt());
        JsonNode resource = search("_id", ANALYSIS_2_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2025-07-07"));
    }
}
