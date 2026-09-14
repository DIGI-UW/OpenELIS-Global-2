package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.DeviceProvider;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.fhir.providers.PractitionerProvider;
import org.openelisglobal.fhir.providers.ServiceRequestProvider;
import org.openelisglobal.fhir.providers.SpecimenProvider;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * Search behaviour that regressed in ways a caller notices: a resource type
 * that could not be searched at all, reverse includes that silently dropped
 * what they were asked for, and ids that did not survive being read back.
 *
 * <p>
 * Each test here pins one of those. The bundle assertions look at
 * {@code search.mode}, because a reverse include failing looks exactly like a
 * successful search until you check whether anything was actually included.
 */
public class FhirSearchRegressionTest extends BaseWebContextSensitiveTest {

    /** result-facade.xml seeds these with statuses that are NOT Finalized. */
    private static final String ANALYSIS_1_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";

    private static final String SERUM_ITEM_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";

    @Autowired
    private ServiceRequestProvider serviceRequestProvider;

    @Autowired
    private ObservationProvider observationProvider;

    @Autowired
    private SpecimenProvider specimenProvider;

    @Autowired
    private PractitionerProvider practitionerProvider;

    @Autowired
    private DeviceProvider deviceProvider;

    @Autowired
    private PersonService personService;

    @Autowired
    private ProviderService providerService;

    private RestfulServer fhirServlet;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.<IResourceProvider>asList(serviceRequestProvider, observationProvider,
                specimenProvider, practitionerProvider, deviceProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        objectMapper = new ObjectMapper();
    }

    /**
     * The reverse include used to emit a report only for a Finalized analysis,
     * while a direct DiagnosticReport search returned it whatever the status. A
     * reverse include has to return what the equivalent direct search returns.
     */
    @Test
    public void serviceRequest_revIncludesDiagnosticReport_whateverTheAnalysisStatus() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        JsonNode bundle = search("/ServiceRequest", "_id", ANALYSIS_1_UUID, "_revinclude", "DiagnosticReport:based-on");

        assertEquals(List.of(ANALYSIS_1_UUID), matchedIds(bundle, "ServiceRequest"));
        assertFalse("the DiagnosticReport based on this ServiceRequest was not reverse-included",
                includedIds(bundle, "DiagnosticReport").isEmpty());
    }

    @Test
    public void observation_revIncludesDiagnosticReport_whateverTheAnalysisStatus() throws Exception {
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        JsonNode bundle = search("/Observation", "_revinclude", "DiagnosticReport:result");

        assertFalse("no DiagnosticReport was reverse-included from Observation",
                includedIds(bundle, "DiagnosticReport").isEmpty());
    }

    /**
     * accessionIdentifier is published as {@code accession[-sortOrder]}, so a
     * client searching with the value it was just given has to get a hit.
     */
    @Test
    public void specimen_accessionAcceptsTheValueTheResourcePublishes() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-specimen.xml");

        JsonNode published = search("/Specimen", "_id", SERUM_ITEM_UUID);
        String accession = published.get("entry").get(0).get("resource").get("accessionIdentifier").get("value")
                .asText();
        assertTrue("expected the item suffix in " + accession, accession.contains("-"));

        assertEquals(List.of(SERUM_ITEM_UUID), matchedIds(search("/Specimen", "accession", accession), "Specimen"));
        assertFalse("the bare accession must still match every item of the sample",
                matchedIds(search("/Specimen", "accession", accession.substring(0, accession.lastIndexOf('-'))),
                        "Specimen").isEmpty());
    }

    /**
     * A provider with no FHIR uuid used to yield a Practitioner with no id, and
     * HAPI rejects a whole bundle over one id-less resource - so a single such row
     * made every Practitioner search fail.
     */
    @Test
    public void practitioner_searchSurvivesAProviderWithoutAFhirUuid() throws Exception {
        resyncSequence("person_seq", "person");
        resyncSequence("provider_seq", "provider");

        Person person = new Person();
        person.setFirstName("Nouuid");
        person.setLastName("Practitioner");
        person.setSysUserId("1");
        personService.save(person);

        Provider provider = new Provider();
        provider.setPerson(person);
        provider.setFhirUuid(null);
        provider.setSysUserId("1");
        providerService.insert(provider);

        JsonNode bundle = search("/Practitioner");

        assertEquals("Bundle", bundle.get("resourceType").asText());
        assertFalse("the uuid-less provider should still come back with an id",
                matchedIds(bundle, "Practitioner").isEmpty());
        assertFalse("a resource in a bundle must carry an id", matchedIds(bundle, "Practitioner").contains(""));
    }

    /**
     * The Device id was minted per request on a read-only path, so the same
     * analyzer answered with a different id every time and none of them resolved.
     */
    @Test
    public void device_idIsStableAcrossRequests() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-device.xml");
        resyncSequence("clinlims.analyzer_seq", "clinlims.analyzer");

        List<String> first = matchedIds(search("/Device"), "Device");
        List<String> second = matchedIds(search("/Device"), "Device");

        assertFalse("no Device to compare", first.isEmpty());
        assertEquals("the same analyzer must keep the same id between requests", first, second);
    }

    /**
     * Walking a search one page at a time has to visit each resource once.
     *
     * <p>
     * HAPI advertises the next page as {@code _offset}, and in that mode it hands
     * the provider the whole range and returns whatever comes back. Reading that
     * range literally made every page after the first serve the entire result set,
     * so a client following the links saw the same rows over and over.
     */
    @Test
    public void search_pagesThroughEveryResultExactlyOnce() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-specimen.xml");

        List<String> everything = matchedIds(search("/Specimen"), "Specimen");
        assertTrue("need at least two specimens to page through", everything.size() >= 2);

        List<String> walked = new ArrayList<>();
        for (int offset = 0; offset < everything.size(); offset++) {
            List<String> page = matchedIds(search("/Specimen", "_count", "1", "_offset", String.valueOf(offset)),
                    "Specimen");
            assertEquals("_count=1 must return one match at offset " + offset, 1, page.size());
            walked.add(page.get(0));
        }

        assertEquals("paging must not repeat a resource", everything.size(), Set.copyOf(walked).size());
        assertEquals("paging must reach every resource the unpaged search returns", Set.copyOf(everything),
                Set.copyOf(walked));
    }

    /**
     * {@code ContactPoint.use} is optional in FHIR, and a phone that omitted it
     * used to be dropped on the way in - so the resource the facade had just
     * accepted could not be found by the very parameters that search phone numbers.
     */
    @Test
    public void practitioner_phoneWithoutAUseIsStoredAndSearchable() throws Exception {
        resyncSequence("person_seq", "person");
        resyncSequence("provider_seq", "provider");

        String phone = "0700111222";
        String id = createPractitioner("{\"resourceType\":\"Practitioner\",\"name\":[{\"family\":\"Nousetest\","
                + "\"given\":[\"Phone\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"" + phone + "\"}]}");

        assertEquals(List.of(id), matchedIds(search("/Practitioner", "phone", phone), "Practitioner"));
        assertEquals(List.of(id), matchedIds(search("/Practitioner", "telecom", phone), "Practitioner"));
    }

    /**
     * address-city, address-state, address-postalcode and address-country are
     * declared search parameters reading the person's address columns, but nothing
     * on the write path populated them, so all four could only ever return nothing.
     */
    @Test
    public void practitioner_addressIsStoredAndSearchableByEveryAddressParam() throws Exception {
        resyncSequence("person_seq", "person");
        resyncSequence("provider_seq", "provider");

        String id = createPractitioner("{\"resourceType\":\"Practitioner\",\"name\":[{\"family\":\"Addrtest\","
                + "\"given\":[\"Live\"]}],\"address\":[{\"city\":\"Testcity\",\"state\":\"Teststate\","
                + "\"postalCode\":\"90210\",\"country\":\"Testcountry\"}]}");

        assertEquals(List.of(id), matchedIds(search("/Practitioner", "address-city", "Testcity"), "Practitioner"));
        assertEquals(List.of(id), matchedIds(search("/Practitioner", "address-state", "Teststate"), "Practitioner"));
        assertEquals(List.of(id), matchedIds(search("/Practitioner", "address-postalcode", "90210"), "Practitioner"));
        assertEquals(List.of(id),
                matchedIds(search("/Practitioner", "address-country", "Testcountry"), "Practitioner"));
        JsonNode address = read("/Practitioner/" + id).path("address").get(0);
        assertEquals("the stored address must come back on a read", "Testcity", address.path("city").asText());
        assertEquals("PERSON.ZIP_CODE is a fixed width column, so the padding must not reach the client", "90210",
                address.path("postalCode").asText());
    }

    // ==================== helpers ====================

    private String createPractitioner(String json) throws Exception {
        MockHttpServletResponse response = post("/Practitioner", json);
        assertEquals(response.getContentAsString(), 201, response.getStatus());
        return objectMapper.readTree(response.getContentAsString()).path("id").asText();
    }

    private MockHttpServletResponse post(String path, String json) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("POST", path);
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        return response;
    }

    private JsonNode read(String path) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private JsonNode search(String path, String... params) throws Exception {
        MockHttpServletRequest request = buildFhirRequest("GET", path);
        StringBuilder query = new StringBuilder();
        for (int i = 0; i + 1 < params.length; i += 2) {
            request.addParameter(params[i], params[i + 1]);
            query.append(query.length() == 0 ? "" : "&").append(params[i]).append("=").append(params[i + 1]);
        }
        request.setQueryString(query.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(response.getContentAsString(), 200, response.getStatus());
        return objectMapper.readTree(response.getContentAsString());
    }

    private List<String> matchedIds(JsonNode bundle, String resourceType) {
        return idsWithMode(bundle, resourceType, "match");
    }

    private List<String> includedIds(JsonNode bundle, String resourceType) {
        return idsWithMode(bundle, resourceType, "include");
    }

    private List<String> idsWithMode(JsonNode bundle, String resourceType, String mode) {
        List<String> ids = new ArrayList<>();
        JsonNode entries = bundle.get("entry");
        if (entries == null) {
            return ids;
        }
        for (JsonNode entry : entries) {
            JsonNode resource = entry.get("resource");
            if (resource == null || !resourceType.equals(resource.path("resourceType").asText())) {
                continue;
            }
            String entryMode = entry.path("search").path("mode").asText("match");
            if (mode.equals(entryMode)) {
                ids.add(resource.path("id").asText());
            }
        }
        return ids;
    }
}
