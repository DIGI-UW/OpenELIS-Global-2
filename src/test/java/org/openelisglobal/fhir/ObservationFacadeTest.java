package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class ObservationFacadeTest extends BaseWebContextSensitiveTest {

    /** result id=3: analysis 1, sample 1 — sample_human has provider_id=1. */
    private static final String RESULT_WITH_PROVIDER_UUID = "550e8400-e29b-41d4-a716-446655440003";
    /** result id=4: analysis 2, sample 2 — sample_human has no provider_id. */
    private static final String RESULT_WITHOUT_PROVIDER_UUID = "550e8400-e29b-41d4-a716-446655440004";
    private static final String PROVIDER_FHIR_UUID = "550e8400-e29b-41d4-a716-446655441004";
    private static final String PATIENT_FHIR_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String SPECIMEN_FHIR_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String ANALYSIS_FHIR_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

    private static final String LOINC_CODING = """
            "code": {
              "coding": [{
                "system": "http://loinc.org",
                "code": "123456",
                "display": "Complete Blood Count"
              }]
            },""";
    private static final String SUBJECT = """
            "subject": {
              "reference": "Patient/%s"
            },""".formatted(PATIENT_FHIR_UUID);
    private static final String SPECIMEN = """
            "specimen": {
              "reference": "Specimen/%s"
            },""".formatted(SPECIMEN_FHIR_UUID);
    private static final String BASED_ON = """
            "basedOn": [{
              "reference": "ServiceRequest/%s"
            }],""".formatted(ANALYSIS_FHIR_UUID);
    private static final String VALUE_QUANTITY = """
            "valueQuantity": {
              "value": 85.5,
              "unit": "g/L"
            }""";

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationProvider observationProvider;

    @Autowired
    private PanelService panelService;

    @Autowired
    private LocalizationService localizationSevice;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/result-facade.xml");
        // result-facade.xml seeds result id=3/4; advance result_seq past them so a
        // newly created Observation's result insert doesn't collide on result_pk
        // when this class runs before others have bumped the sequence.
        resyncSequence("clinlims.result_seq", "clinlims.result");

        // The fixture inserts result rows with explicit ids (3, 4) without
        // advancing result_seq, so whether createObservation's sequence-driven
        // insert collides with them depends on how many results earlier tests
        // happened to create — a suite-order coin flip. Resync the sequence
        // past the fixture's ids so this test is order-independent.
        new org.springframework.jdbc.core.JdbcTemplate(dataSource)
                .queryForObject("SELECT setval('clinlims.result_seq', (SELECT GREATEST(COALESCE(MAX(id), 1), 1)::bigint"
                        + " FROM clinlims.result))", Long.class);

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(observationProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");

        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();

    }

    @Test
    public void readObservation_shouldReturnSuccess() throws Exception {

        Result result = resultService.getResultByFhirUuid(RESULT_WITHOUT_PROVIDER_UUID);
        assertNotNull("Result not found in test data", result);

        MockHttpServletResponse response = serve(
                buildFhirRequest("GET", "/Observation/" + RESULT_WITHOUT_PROVIDER_UUID));

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("Observation", jsonResponse.get("resourceType").asText());

        assertEquals("final", jsonResponse.get("status").asText());
    }

    @Test
    public void readObservation_withOrderingProvider_shouldCarryPractitionerPerformer() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Observation/" + RESULT_WITH_PROVIDER_UUID));

        assertEquals(200, response.getStatus());

        JsonNode performer = objectMapper.readTree(response.getContentAsString()).get("performer");
        assertNotNull("performer should be present for a sample with an ordering provider", performer);
        assertEquals(1, performer.size());
        assertEquals("Practitioner/" + PROVIDER_FHIR_UUID, performer.get(0).get("reference").asText());
    }

    @Test
    public void readObservation_withoutOrderingProvider_shouldOmitPerformer() throws Exception {

        MockHttpServletResponse response = serve(
                buildFhirRequest("GET", "/Observation/" + RESULT_WITHOUT_PROVIDER_UUID));

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertFalse("performer must be omitted when sample_human.provider_id is null", jsonResponse.has("performer"));
    }

    @Test
    public void createObservation_withoutLoincCode_shouldReturn422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", """
                "code": {
                  "coding": [{
                    "system": "http://loinc.org"
                  }]
                },""", SUBJECT, SPECIMEN, BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withInvalidLoincCode_shouldReturn422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", """
                "code": {
                  "coding": [{
                    "system": "http://loinc.org",
                    "code": "999999",
                    "display": "Invalid LOINC"
                  }]
                },""", SUBJECT, SPECIMEN, BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutValueQuantity_shouldReturn422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", LOINC_CODING, SUBJECT, SPECIMEN, BASED_ON,
                "\"effectiveDateTime\": \"2026-03-09T10:00:00+03:00\"");

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutStatus_shouldReturn422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "", LOINC_CODING, SUBJECT, SPECIMEN, BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void readObservation_withNonExistentId_shouldReturn404() throws Exception {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Observation/" + nonExistentUuid));

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void updateObservation_shouldUpdateValue() throws Exception {

        Result result = resultService.getResultByFhirUuid(RESULT_WITH_PROVIDER_UUID);
        assertNotNull("Result not found in test data", result);
        attachPanelToAnalysis("1");

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", LOINC_CODING, SUBJECT,
                SPECIMEN, BASED_ON, """
                        "effectiveDateTime": "2026-03-05T00:00:00+03:00",
                        "valueQuantity": {
                          "value": 99.0,
                          "unit": "g/L"
                        }""");

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(200, response.getStatus());

        Result updatedResult = resultService.getResultByFhirUuid(RESULT_WITH_PROVIDER_UUID);

        assertEquals("99.0", updatedResult.getValue());
    }

    @Test
    public void deleteObservation_shouldReturn204() throws Exception {

        Result result = resultService.getResultByFhirUuid(RESULT_WITH_PROVIDER_UUID);
        assertNotNull("Result not found in test data", result);
        attachPanelToAnalysis("1");

        MockHttpServletResponse response = serve(
                buildFhirRequest("DELETE", "/Observation/" + RESULT_WITH_PROVIDER_UUID));

        assertEquals(204, response.getStatus());

        Result deletedResult = resultService.getResultByFhirUuid(RESULT_WITH_PROVIDER_UUID);

        assertNotNull(deletedResult);
    }

    @Test
    public void searchObservation_endpointExists_shouldNotReturn404() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");

        request.setQueryString("patient=" + PATIENT_FHIR_UUID);
        request.addParameter("patient", PATIENT_FHIR_UUID);

        MockHttpServletResponse response = serve(request);

        assertNotNull(response);
        assertTrue(response.getStatus() != 404);
    }

    @Test
    public void createObservation_shouldCreateNewResult() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", LOINC_CODING, SUBJECT, SPECIMEN, BASED_ON,
                VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(201, response.getStatus());

        String location = response.getHeader("Location");
        assertNotNull("Location header should contain the new Resource URL", location);
        String newUuid = location.substring(location.lastIndexOf("/") + 1);

        Result createdResult = resultService.getResultByFhirUuid(newUuid);
        assertNotNull("Result should be persisted in the database", createdResult);
        assertEquals("85.5", createdResult.getValue());
    }

    @Test
    public void createObservation_withoutServiceRequestShouldThrow422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", LOINC_CODING, SUBJECT, SPECIMEN, "",
                VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutSpecimenShouldThrow422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", LOINC_CODING, SUBJECT, "", BASED_ON,
                VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void createObservation_withoutSubjectShouldThrow422() throws Exception {
        attachPanelToAnalysis("1");

        String createJson = observationJson(null, "\"status\": \"final\",", LOINC_CODING, "", SPECIMEN, BASED_ON,
                VALUE_QUANTITY);

        MockHttpServletResponse response = serve(postObservation(createJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutServiceRequest_shouldReturn422() throws Exception {

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", LOINC_CODING, SUBJECT,
                SPECIMEN, "", VALUE_QUANTITY);

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutSubject_shouldReturn422() throws Exception {
        attachPanelToAnalysis("1");

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", LOINC_CODING, "",
                SPECIMEN, BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutSpecimen_shouldReturn422() throws Exception {

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", LOINC_CODING, SUBJECT,
                "", BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutLoincCode_shouldReturn422() throws Exception {

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", """
                "code": {
                  "coding": [{
                    "system": "http://loinc.org"
                  }]
                },""", SUBJECT, SPECIMEN, BASED_ON, VALUE_QUANTITY);

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(422, response.getStatus());
    }

    @Test
    public void updateObservation_withoutValue_shouldReturn422() throws Exception {

        String updateJson = observationJson(RESULT_WITH_PROVIDER_UUID, "\"status\": \"final\",", LOINC_CODING, SUBJECT,
                SPECIMEN, BASED_ON, "\"effectiveDateTime\": \"2026-03-05T00:00:00+03:00\"");

        MockHttpServletResponse response = serve(putObservation(RESULT_WITH_PROVIDER_UUID, updateJson));

        assertEquals(422, response.getStatus());
    }

    /**
     * The result workflow merges the analysis together with its panel, and
     * Hibernate treats a panel without a version (lastupdated) as transient. The
     * fixture's analyses have no panel, so tests that write through the result
     * workflow attach a freshly saved, versioned one first.
     */
    private void attachPanelToAnalysis(String analysisId) {
        Analysis analysis = analysisService.getAnalysisById(analysisId);
        assertNotNull("Analysis reference required for creating result", analysis);

        Localization localization = new Localization();
        localization.setDescription("Test Panel");
        localization.setLastupdated(new Timestamp(System.currentTimeMillis()));
        Localization savedLocalization = localizationSevice.save(localization);

        Panel newPanel = new Panel();
        newPanel.setPanelName("New Panel Name");
        newPanel.setDescription("A test panel from dataset.");
        newPanel.setLocalization(savedLocalization);
        Panel panel = panelService.save(newPanel);

        analysis.setPanel(panel);
        analysisService.save(analysis);
    }

    /**
     * Assembles an Observation body from optional fragments; an empty fragment
     * omits that element so each validation test states exactly what it leaves out.
     */
    private static String observationJson(String id, String status, String code, String subject, String specimen,
            String basedOn, String tail) {
        StringBuilder json = new StringBuilder("{\n\"resourceType\": \"Observation\",\n");
        if (id != null) {
            json.append("\"id\": \"").append(id).append("\",\n");
        }
        for (String fragment : new String[] { status, code, subject, specimen, basedOn }) {
            if (!fragment.isEmpty()) {
                json.append(fragment).append('\n');
            }
        }
        json.append(tail).append("\n}");
        return json.toString();
    }

    private MockHttpServletRequest postObservation(String body) {
        MockHttpServletRequest request = buildFhirRequest("POST", "/Observation");
        request.setContent(body.getBytes());
        return request;
    }

    private MockHttpServletRequest putObservation(String uuid, String body) {
        MockHttpServletRequest request = buildFhirRequest("PUT", "/Observation/" + uuid);
        request.setContent(body.getBytes());
        return request;
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
