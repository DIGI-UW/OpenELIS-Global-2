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
import org.openelisglobal.provider.valueholder.Provider;
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

        Provider provider1 = new Provider();
        provider1.setPerson(personService.get("3"));
        provider1.setId("2");
        provider1.setFhirUuid(FH_UUID2);

        String providerJson = new ObjectMapper().writeValueAsString(provider1);

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
