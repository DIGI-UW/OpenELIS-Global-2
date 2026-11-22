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
import org.openelisglobal.fhir.providers.PatientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * {@code Patient?_revinclude=...} pulls the orders, specimens, results and
 * reports linked to a patient through sample_human (result-facade.xml).
 */
public class PatientSearchRevIncludeFacadeTest extends BaseWebContextSensitiveTest {

    /** patient 1: sample 1, sample item 601, analysis 1, result 3. */
    private static final String PATIENT_1_UUID = "550e8400-e29b-41d4-a716-446655440001";
    /** patient 2: sample 2, sample item 602, analysis 2 (final), result 4. */
    private static final String PATIENT_2_UUID = "550e8400-e29b-41d4-a716-446655440004";
    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String ANALYSIS_2_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b02";
    private static final String SAMPLE_ITEM_601_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String RESULT_3_UUID = "550e8400-e29b-41d4-a716-446655440003";

    @Autowired
    private PatientProvider patientProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");
        resyncSequence("clinlims.result_seq", "clinlims.result");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_withoutRevInclude_returnsPatientsOnly() throws Exception {
        JsonNode bundle = search(PATIENT_1_UUID, null);

        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of("Patient"), resourceTypes(bundle));
    }

    @Test
    public void revIncludeServiceRequestPatient_addsTheOrders() throws Exception {
        JsonNode bundle = search(PATIENT_1_UUID, "ServiceRequest:patient");

        assertEquals("total counts matches only, not included resources", 1, bundle.get("total").asInt());
        assertTrue(idsOf(bundle, "Patient").contains(PATIENT_1_UUID));
        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));
        assertEquals("include", searchModeOf(bundle, "ServiceRequest"));
    }

    @Test
    public void revIncludeServiceRequestSubject_isAcceptedAsAlias() throws Exception {
        JsonNode bundle = search(PATIENT_1_UUID, "ServiceRequest:subject");

        assertEquals(List.of(ANALYSIS_1_UUID), idsOf(bundle, "ServiceRequest"));
    }

    @Test
    public void revIncludeSpecimenPatient_addsTheSampleItems() throws Exception {
        JsonNode bundle = search(PATIENT_1_UUID, "Specimen:patient");

        assertEquals(List.of(SAMPLE_ITEM_601_UUID), idsOf(bundle, "Specimen"));
    }

    @Test
    public void revIncludeObservationPatient_addsTheResults() throws Exception {
        JsonNode bundle = search(PATIENT_1_UUID, "Observation:patient");

        assertEquals(List.of(RESULT_3_UUID), idsOf(bundle, "Observation"));
    }

    @Test
    public void revIncludeDiagnosticReportPatient_addsReports() throws Exception {
        JsonNode bundle = search(PATIENT_2_UUID, "DiagnosticReport:patient");

        assertEquals(List.of(ANALYSIS_2_UUID), idsOf(bundle, "DiagnosticReport"));
        assertEquals("final", bundle.get("entry").get(1).get("resource").get("status").asText());
    }

    @Test
    public void unsupportedRevInclude_isRejected() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient");
        request.setQueryString("_id=" + PATIENT_1_UUID + "&_revinclude=Encounter:patient");
        request.addParameter("_id", PATIENT_1_UUID);
        request.addParameter("_revinclude", "Encounter:patient");

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);

        assertEquals(400, response.getStatus());
    }

    private JsonNode search(String patientUuid, String revInclude) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient");
        String query = "_id=" + patientUuid;
        request.addParameter("_id", patientUuid);
        if (revInclude != null) {
            query += "&_revinclude=" + revInclude;
            request.addParameter("_revinclude", revInclude);
        }
        request.setQueryString(query);

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private static List<String> resourceTypes(JsonNode bundle) {
        List<String> types = new ArrayList<>();
        for (JsonNode entry : bundle.get("entry")) {
            types.add(entry.get("resource").get("resourceType").asText());
        }
        return types;
    }

    private static List<String> idsOf(JsonNode bundle, String resourceType) {
        List<String> ids = new ArrayList<>();
        for (JsonNode entry : bundle.get("entry")) {
            JsonNode resource = entry.get("resource");
            if (resourceType.equals(resource.get("resourceType").asText())) {
                ids.add(resource.get("id").asText());
            }
        }
        return ids;
    }

    private static String searchModeOf(JsonNode bundle, String resourceType) {
        for (JsonNode entry : bundle.get("entry")) {
            if (resourceType.equals(entry.get("resource").get("resourceType").asText())) {
                return entry.get("search").get("mode").asText();
            }
        }
        return null;
    }
}
