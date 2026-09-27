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
 * Patient search answered from the OpenELIS database (facade-patient.xml).
 */
public class PatientSearchFacadeTest extends BaseWebContextSensitiveTest {

    private static final String JOHN_DOE_UUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04635";
    private static final String JAMES_MULIZI_UUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04636";
    private static final String FAITH_KUKKI_UUID = "b479ab79-5f53-4d1f-bc9b-10f19ce04637";

    @Autowired
    private PatientProvider patientProvider;

    private RestfulServer fhirServlet;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-patient.xml");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);

        objectMapper = new ObjectMapper();
    }

    @Test
    public void search_withoutParameters_returnsEveryPatient() throws Exception {
        JsonNode bundle = search();

        assertEquals("searchset", bundle.get("type").asText());
        assertEquals(3, bundle.get("total").asInt());
        assertTrue(patientIds(bundle).containsAll(List.of(JOHN_DOE_UUID, JAMES_MULIZI_UUID, FAITH_KUKKI_UUID)));
    }

    @Test
    public void search_byId_returnsOnlyThatPatient() throws Exception {
        JsonNode bundle = search("_id", JAMES_MULIZI_UUID);

        assertEquals(1, bundle.get("total").asInt());
        assertEquals(List.of(JAMES_MULIZI_UUID), patientIds(bundle));
    }

    @Test
    public void search_byUnknownId_returnsEmptySearchset() throws Exception {
        JsonNode bundle = search("_id", "00000000-0000-0000-0000-000000000000");

        assertEquals(0, bundle.get("total").asInt());
    }

    @Test
    public void search_byNonUuidId_returnsEmptyRatherThanError() throws Exception {
        JsonNode bundle = search("_id", "not-a-uuid");

        assertEquals(0, bundle.get("total").asInt());
    }

    @Test
    public void search_byBareIdentifier_matchesNationalId() throws Exception {
        JsonNode bundle = search("identifier", "1234");

        assertEquals("patients 1 and 3 share national id 1234", 2, bundle.get("total").asInt());
        assertTrue(patientIds(bundle).containsAll(List.of(JOHN_DOE_UUID, FAITH_KUKKI_UUID)));
    }

    @Test
    public void search_byBareIdentifier_matchesPatientIdentityRow() throws Exception {
        JsonNode bundle = search("identifier", "334-422-A");

        assertEquals(List.of(JOHN_DOE_UUID), patientIds(bundle));
    }

    @Test
    public void search_byNationalIdSystem_matchesOnlyNationalId() throws Exception {
        JsonNode bundle = search("identifier", "http://openelis-global.org/pat_nationalId|56789");

        assertEquals(List.of(JAMES_MULIZI_UUID), patientIds(bundle));
    }

    @Test
    public void search_byUuidSystem_matchesFhirUuid() throws Exception {
        JsonNode bundle = search("identifier", "http://openelis-global.org/pat_uuid|" + FAITH_KUKKI_UUID);

        assertEquals(List.of(FAITH_KUKKI_UUID), patientIds(bundle));
    }

    @Test
    public void search_byUnknownIdentifierSystem_returnsNothing() throws Exception {
        JsonNode bundle = search("identifier", "http://example.org/other|1234");

        assertEquals(0, bundle.get("total").asInt());
    }

    @Test
    public void search_byName_matchesGivenOrFamily() throws Exception {
        assertEquals(List.of(JOHN_DOE_UUID), patientIds(search("name", "Doe")));
        assertEquals(List.of(JAMES_MULIZI_UUID), patientIds(search("name", "james")));
    }

    @Test
    public void search_byGivenAndFamily_areAndCombined() throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient");
        request.setQueryString("given=Faith&family=Kukki");
        request.addParameter("given", "Faith");
        request.addParameter("family", "Kukki");

        assertEquals(List.of(FAITH_KUKKI_UUID), patientIds(serve(request)));

        request = buildFhirRequest("GET", "/Patient");
        request.setQueryString("given=Faith&family=Doe");
        request.addParameter("given", "Faith");
        request.addParameter("family", "Doe");

        assertEquals(0, serve(request).get("total").asInt());
    }

    @Test
    public void search_byGender_mapsFhirCodesToOpenElisColumn() throws Exception {
        assertEquals(List.of(JAMES_MULIZI_UUID), patientIds(search("gender", "female")));
        assertEquals(2, search("gender", "male").get("total").asInt());
        assertEquals(0, search("gender", "unknown").get("total").asInt());
    }

    @Test
    public void search_byBirthDateRange_usesPrefixes() throws Exception {
        assertEquals(List.of(JAMES_MULIZI_UUID), patientIds(search("birthdate", "ge1995-01-01")));
        assertEquals(2, search("birthdate", "lt1995-01-01").get("total").asInt());
        assertEquals(2, search("birthdate", "1992-12-12").get("total").asInt());
    }

    @Test
    public void search_returnsFullyTransformedPatients() throws Exception {
        JsonNode bundle = search("_id", JOHN_DOE_UUID);
        JsonNode patient = bundle.get("entry").get(0).get("resource");

        assertEquals("Patient", patient.get("resourceType").asText());
        assertEquals("Doe", patient.get("name").get(0).get("family").asText());
        assertEquals("male", patient.get("gender").asText());
    }

    private JsonNode search() throws Exception {
        return serve(buildFhirRequest("GET", "/Patient"));
    }

    private JsonNode search(String param, String value) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", "/Patient");
        request.setQueryString(param + "=" + value);
        request.addParameter(param, value);
        return serve(request);
    }

    private JsonNode serve(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private static List<String> patientIds(JsonNode bundle) {
        List<String> ids = new ArrayList<>();
        JsonNode entries = bundle.get("entry");
        if (entries == null) {
            return ids;
        }
        for (JsonNode entry : entries) {
            JsonNode resource = entry.get("resource");
            if ("Patient".equals(resource.get("resourceType").asText())) {
                ids.add(resource.get("id").asText());
            }
        }
        return ids;
    }

    @Test
    public void search_byLastUpdated_andExposesMetaLastUpdated() throws Exception {
        assertEquals(3, search("_lastUpdated", "ge2023-11-01").get("total").asInt());
        assertEquals(3, search("_lastUpdated", "le2023-11-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "lt2023-11-01").get("total").asInt());
        assertEquals(0, search("_lastUpdated", "gt2023-11-02").get("total").asInt());
        JsonNode resource = search("_id", JOHN_DOE_UUID).get("entry").get(0).get("resource");
        assertTrue(resource.get("meta").get("lastUpdated").asText().startsWith("2023-11-01"));
    }
}
