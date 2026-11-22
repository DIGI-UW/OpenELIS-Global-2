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
import org.openelisglobal.fhir.providers.SpecimenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Specimen search answered from the OpenELIS database. facade-specimen.xml
 * seeds two sample items without a patient; result-facade.xml adds the patient,
 * order and result links used for includes.
 */
public class SpecimenSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String SERUM_ITEM_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String URINE_ITEM_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93271";
    private static final String PATIENT_1_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";

    @Autowired
    private SpecimenProvider specimenProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(specimenProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_byIdAndIdentifiers_findTheSampleItem() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-specimen.xml");

        assertEquals(2, search().get("total").asInt());
        assertEquals(List.of(SERUM_ITEM_UUID), idsOf(search("_id", SERUM_ITEM_UUID), "Specimen"));
        assertEquals(List.of(URINE_ITEM_UUID), idsOf(search("identifier", URINE_ITEM_UUID), "Specimen"));
        assertEquals(List.of(SERUM_ITEM_UUID),
                idsOf(search("identifier", "http://openelis-global.org/sampleItem_labNo|DEV01260000000000001-1"),
                        "Specimen"));
        assertEquals(List.of(URINE_ITEM_UUID), idsOf(search("identifier", "DEV01260000000000002"), "Specimen"));
        assertEquals(List.of(URINE_ITEM_UUID), idsOf(search("accession", "DEV01260000000000002"), "Specimen"));
        assertEquals(0, search("identifier", "http://example.org/other|DEV01260000000000002").get("total").asInt());
    }

    @Test
    public void search_byTypeStatusAndCollectionDate() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-specimen.xml");

        assertEquals(List.of(URINE_ITEM_UUID), idsOf(search("type", "urine sample"), "Specimen"));
        assertEquals(0, search("type", "Plasma").get("total").asInt());
        assertEquals(2, search("status", "available").get("total").asInt());
        assertEquals(0, search("status", "unsatisfactory").get("total").asInt());
        assertEquals(2, search("collected", "2023-11-15").get("total").asInt());
        assertEquals(0, search("collected", "lt2023-11-15").get("total").asInt());
        assertEquals(2, search("collected", "ge2023-11-01").get("total").asInt());
    }

    @Test
    public void search_byPatient_andIncludes_followSampleHuman() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        resyncSequence("clinlims.result_seq", "clinlims.result");

        assertEquals(List.of(SERUM_ITEM_UUID), idsOf(search("patient", "Patient/" + PATIENT_1_UUID), "Specimen"));
        assertEquals(List.of(SERUM_ITEM_UUID), idsOf(search("subject", PATIENT_1_UUID), "Specimen"));
        assertEquals(0, search("patient", "00000000-0000-0000-0000-000000000000").get("total").asInt());

        JsonNode bundle = search(SERUM_ITEM_UUID, "_include", "Specimen:patient");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(PATIENT_1_UUID), idsOf(bundle, "Patient"));

        bundle = search(SERUM_ITEM_UUID, "_revinclude", "ServiceRequest:specimen");
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));

        bundle = search(SERUM_ITEM_UUID, "_revinclude", "Observation:specimen");
        assertEquals(List.of(RESULT_3_UUID), idsOf(bundle, "Observation"));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());
    }

    @Test
    public void search_returnsTransformedSpecimens() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-specimen.xml");

        JsonNode resource = search("_id", SERUM_ITEM_UUID).get("entry").get(0).get("resource");
        assertEquals("Specimen", resource.get("resourceType").asText());
        assertEquals("DEV01260000000000001-1", resource.get("accessionIdentifier").get("value").asText());
        assertTrue(resource.get("type").get("coding").size() >= 1);
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Specimen"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Specimen");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode search(String id, String includeParam, String includeValue) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Specimen");
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
    public void search_byLastUpdated_andRevIncludeDiagnosticReport() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        assertEquals(2, search("_lastUpdated", "ge2023-12-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2023-12-01").get("total").asInt());
        JsonNode bundle = search(SERUM_ITEM_UUID, "_revinclude", "DiagnosticReport:specimen");
        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "DiagnosticReport"));
        assertEquals("include", bundle.get("entry").get(1).get("search").get("mode").asText());
        assertTrue(bundle.get("entry").get(0).get("resource").get("meta").get("lastUpdated").asText()
                .startsWith("2023-12-01"));
    }
}
