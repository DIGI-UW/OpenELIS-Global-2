package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.fhir.providers.DiagnosticReportProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class DiagnosticReportFacadeTest extends BaseWebContextSensitiveTest {

    /** analysis id=1 in result-facade.xml. */
    private static final String ANALYSIS_1_FHIR_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    /** analysis id=2 in result-facade.xml. */
    private static final String ANALYSIS_2_FHIR_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Autowired
    private DiagnosticReportProvider diagnosticReportProvider;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/result-facade.xml");
        // result-facade.xml seeds result id=3/4; advance result_seq past them so
        // result inserts don't collide on result_pk under adverse test ordering.
        resyncSequence("clinlims.result_seq", "clinlims.result");

        servletContext = new MockServletContext();

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(diagnosticReportProvider));

        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void readDiagnosticReport_shouldReturnSuccess() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/DiagnosticReport/" + ANALYSIS_2_FHIR_UUID));

        assertEquals(200, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("DiagnosticReport", jsonResponse.get("resourceType").asText());
        assertEquals("final", jsonResponse.get("status").asText());
        assertNotNull(jsonResponse.get("result"));
    }

    @Test
    public void readDiagnosticReport_withNonExistentId_shouldReturn404() throws Exception {

        String nonExistentUuid = "00000000-0000-0000-0000-000000000000";

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/DiagnosticReport/" + nonExistentUuid));

        assertEquals(404, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void readDiagnosticReport_withInvalidUuid_shouldReturn400() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("GET", "/DiagnosticReport/not-a-uuid"));

        assertEquals(400, response.getStatus());

        JsonNode jsonResponse = objectMapper.readTree(response.getContentAsString());

        assertEquals("OperationOutcome", jsonResponse.get("resourceType").asText());
    }

    @Test
    public void deleteDiagnosticReport_shouldCancelUnderlyingAnalysis() throws Exception {

        Analysis before = analysisService.getAnalysisById("1");
        String canceledStatusId = statusService.getStatusID(AnalysisStatus.Canceled);
        assertNotEquals("fixture analysis must start out not cancelled", canceledStatusId, before.getStatusId());

        MockHttpServletResponse response = serve(
                buildFhirRequest("DELETE", "/DiagnosticReport/" + ANALYSIS_1_FHIR_UUID));

        assertEquals(204, response.getStatus());

        Analysis after = analysisService.getAnalysisById("1");
        assertEquals("delete must soft-cancel the analysis, not remove it", canceledStatusId, after.getStatusId());

        MockHttpServletResponse readAfter = serve(buildFhirRequest("GET", "/DiagnosticReport/" + ANALYSIS_1_FHIR_UUID));
        assertEquals("cancelled analysis remains readable", 200, readAfter.getStatus());
    }

    @Test
    public void deleteDiagnosticReport_withNonExistentId_shouldReturn404() throws Exception {

        MockHttpServletResponse response = serve(
                buildFhirRequest("DELETE", "/DiagnosticReport/00000000-0000-0000-0000-000000000000"));

        assertEquals(404, response.getStatus());
        assertEquals("OperationOutcome",
                objectMapper.readTree(response.getContentAsString()).get("resourceType").asText());
    }

    @Test
    public void deleteDiagnosticReport_withInvalidUuid_shouldReturn400() throws Exception {

        MockHttpServletResponse response = serve(buildFhirRequest("DELETE", "/DiagnosticReport/not-a-uuid"));

        assertEquals(400, response.getStatus());
        assertEquals("OperationOutcome",
                objectMapper.readTree(response.getContentAsString()).get("resourceType").asText());
    }

    @Test
    public void searchDiagnosticReport_endpointExists_shouldNotReturn404() throws Exception {

        MockHttpServletRequest request = buildFhirRequest("GET", "/DiagnosticReport");
        request.setQueryString("subject=Patient/550e8400-e29b-41d4-a716-446655440001&status=final");
        request.addParameter("subject", "Patient/550e8400-e29b-41d4-a716-446655440001");
        request.addParameter("status", "final");

        MockHttpServletResponse response = serve(request);

        assertNotNull(response);
        org.junit.Assert.assertTrue(response.getStatus() != 404);
    }

    private MockHttpServletResponse serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

}
