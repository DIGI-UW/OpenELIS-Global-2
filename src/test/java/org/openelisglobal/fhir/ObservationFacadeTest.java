package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisServiceImpl;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final String HISTORY_INSERT = "INSERT INTO clinlims.history (id, sys_user_id, reference_id,"
            + " reference_table, timestamp, activity, changes) VALUES (nextval('clinlims.history_seq'), ?, 2,"
            + " CAST(? AS numeric), ?, 'U', ?)";

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ObservationProvider observationProvider;

    @Autowired
    private javax.sql.DataSource dataSource;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/result-facade.xml");
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
    public void readObservation_neverNamesTheOrderingProviderAsPerformer() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/Observation/" + RESULT_WITH_PROVIDER_UUID));

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertFalse("a result that is not validated has no performer, whoever ordered it",
                jsonResponse.has("performer"));
        assertFalse(response.getContentAsString().contains(PROVIDER_FHIR_UUID));
    }

    @Test
    public void readObservation_validatedResult_namesTheValidator() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("UPDATE clinlims.system_user SET first_name = 'Vera', last_name = 'Validator' WHERE id = 2");
        String analysisTable = AnalysisServiceImpl.getTableReferenceId();
        jdbc.update(HISTORY_INSERT, 1, analysisTable, Timestamp.valueOf("2025-07-07 12:30:00"),
                "<statusId>1</statusId>".getBytes(StandardCharsets.UTF_8));
        jdbc.update(HISTORY_INSERT, 2, analysisTable, Timestamp.valueOf("2025-07-07 13:30:00"),
                "<statusId>15</statusId>".getBytes(StandardCharsets.UTF_8));
        jdbc.update(HISTORY_INSERT, 1, analysisTable, Timestamp.valueOf("2025-07-07 14:30:00"),
                "<revision>1</revision>".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = serve(
                buildFhirRequest("GET", "/Observation/" + RESULT_WITHOUT_PROVIDER_UUID));

        assertEquals(200, response.getStatus());
        JsonNode performer = objectMapper.readTree(response.getContentAsString()).get("performer");
        assertNotNull("a validated result names who validated it", performer);
        assertEquals(1, performer.size());
        assertEquals("Vera Validator", performer.get(0).get("display").asText());
        assertFalse(performer.get(0).has("reference"));
    }

    @Test
    public void readObservation_validatedResultWithoutAuditTrail_omitsPerformer() throws Exception {

        MockHttpServletResponse response = serve(
                buildFhirRequest("GET", "/Observation/" + RESULT_WITHOUT_PROVIDER_UUID));

        assertEquals(200, response.getStatus());
        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());
        assertFalse("with no record of who validated it, performer is omitted", jsonResponse.has("performer"));
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
    public void searchObservation_endpointExists_shouldNotReturn404() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/Observation");

        request.setQueryString("patient=" + PATIENT_FHIR_UUID);
        request.addParameter("patient", PATIENT_FHIR_UUID);

        MockHttpServletResponse response = serve(request);

        assertNotNull(response);
        assertTrue(response.getStatus() != 404);
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
