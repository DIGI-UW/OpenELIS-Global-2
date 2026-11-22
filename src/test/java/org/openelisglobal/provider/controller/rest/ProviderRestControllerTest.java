package org.openelisglobal.provider.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.person.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class ProviderRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PersonService personService;

    private static final UUID FH_UUID2 = UUID.fromString("f1cdeff8-8d5b-4023-bd7c-932b4b98b6d2");
    private static final String PERSON1_FIRSTNAME = "Henry";
    private static final String PERSON1_MIDDLENAME = "Mutton";
    private static final String PERSON1_LASTNAME = "Stanley";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/provider.xml");
    }

    private static final int SAME_NAME_PROVIDERS = 30;

    private void insertSameNameProviders() {
        for (int i = 0; i < SAME_NAME_PROVIDERS; i++) {
            long personId = 9500 + i;
            jdbcTemplate.update("INSERT INTO clinlims.person (id, first_name, last_name, lastupdated)"
                    + " VALUES (?, 'Jim', 'Jamtest', now())", personId);
            jdbcTemplate.update("INSERT INTO clinlims.provider (id, person_id, fhir_uuid, active, lastupdated)"
                    + " VALUES (?, ?, ?, true, now())", personId, personId, UUID.randomUUID());
        }
    }

    private Map<String, Object> search(String query) throws Exception {
        MvcResult result = super.mockMvc.perform(get("/rest/provider/search" + query)).andReturn();
        assertEquals(200, result.getResponse().getStatus());
        return new ObjectMapper().readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {
                });
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Object> personIds(Map<String, Object> response) {
        java.util.List<Object> ids = new java.util.ArrayList<>();
        for (Map<String, Object> provider : (java.util.List<Map<String, Object>>) response.get("providers")) {
            ids.add(provider.get("personId"));
        }
        return ids;
    }

    /**
     * OGC-431 review: the search ignored the page size it was asked for and always
     * returned the configured default plus one, while the start row was computed
     * from the requested size, so a larger page was truncated and later pages
     * skipped rows.
     */
    @Test
    public void searchProviders_honoursTheRequestedPageSize() throws Exception {
        insertSameNameProviders();

        Map<String, Object> all = search("?search=Jamtest&pageSize=100");

        assertEquals(SAME_NAME_PROVIDERS, all.get("totalCount"));
        assertEquals(SAME_NAME_PROVIDERS, personIds(all).size());
        assertEquals(SAME_NAME_PROVIDERS, new java.util.HashSet<>(personIds(all)).size());
        assertEquals(100, all.get("pageSize"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchProviders_reportsWhetherEachProviderIsActive() throws Exception {
        Map<String, Object> response = search("?search=Doe");
        Map<String, Object> doe = ((java.util.List<Map<String, Object>>) response.get("providers")).get(0);
        assertEquals(Boolean.TRUE, doe.get("isActive"));

        response = search("?search=Mulizi");
        Map<String, Object> mulizi = ((java.util.List<Map<String, Object>>) response.get("providers")).get(0);
        assertEquals(Boolean.FALSE, mulizi.get("isActive"));
    }

    @Test
    public void searchProviders_capsThePageSize() throws Exception {
        assertEquals(ProviderRestController.MAX_SEARCH_PAGE_SIZE,
                search("?search=Jamtest&pageSize=100000").get("pageSize"));
        assertEquals(1, search("?search=Jamtest&pageSize=0").get("pageSize"));
    }

    @Test
    public void searchProviders_pagesWithoutGapsOrOverlaps() throws Exception {
        insertSameNameProviders();

        java.util.Set<Object> seen = new java.util.HashSet<>();
        for (int page = 1; page <= 3; page++) {
            java.util.List<Object> ids = personIds(search("?search=Jamtest&pageSize=10&page=" + page));
            assertEquals("page " + page + " is full", 10, ids.size());
            seen.addAll(ids);
        }
        assertEquals("three pages of ten cover every record once", SAME_NAME_PROVIDERS, seen.size());
    }

    @Test
    public void getProvider_shouldReturnProviderGivenId() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/Provider/raw/2").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String providerJson = urlResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> providerMap = objectMapper.readValue(providerJson,
                new TypeReference<Map<String, Object>>() {
                });

        assertEquals("2", providerMap.get("id"));
        assertEquals("1234567891", providerMap.get("npi"));
        assertEquals("EXT123466", providerMap.get("externalId"));
        assertEquals("S", providerMap.get("providerType"));
        assertEquals(false, providerMap.get("active"));
        assertEquals(true, providerMap.get("desynchronized"));
    }

    @Test
    public void getPerson_shouldReturnPersonInfoByID() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/Provider/Person/2")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String personJson = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> personInfo = objectMapper.readValue(personJson, new TypeReference<Map<String, Object>>() {
        });

        assertEquals("James", personInfo.get("firstName"));
        assertEquals("Mark", personInfo.get("middleName"));
        assertEquals("Orion", personInfo.get("city"));

    }

    @Test
    public void insertOrUpdateProviderByFhirUuid_shouldUpdateProviderGivenTheFhirUUID() throws Exception {

        // The UI-shaped payload (ProviderMenu.tsx handleUpdateProvider): a
        // whitelist of bindable fields. Serializing the Hibernate entity emits
        // derived getters the strict production mapper rejects with 400.
        String providerJson = "{\"id\":\"2\",\"fhirUuid\":\"" + FH_UUID2 + "\",\"person\":{\"id\":\"3\","
                + "\"firstName\":\"" + PERSON1_FIRSTNAME + "\",\"lastName\":\"" + PERSON1_LASTNAME + "\","
                + "\"middleName\":\"" + PERSON1_MIDDLENAME + "\"},\"active\":true}";

        MvcResult urlResult = super.mockMvc.perform(post("/rest/Provider/FhirUuid")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
                .param("fhirUuid", FH_UUID2.toString()).content(providerJson)).andReturn();

        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        String providerString = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> providerUpdated = objectMapper.readValue(providerString,
                new TypeReference<Map<String, Object>>() {
                });
        @SuppressWarnings("unchecked")
        Map<String, Object> personMap = (Map<String, Object>) providerUpdated.get("person");
        assertEquals(PERSON1_FIRSTNAME, personMap.get("firstName"));
        assertEquals(PERSON1_LASTNAME, personMap.get("lastName"));
        assertEquals(PERSON1_MIDDLENAME, personMap.get("middleName"));

    }

}
