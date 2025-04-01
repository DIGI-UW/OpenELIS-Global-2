package org.openelisglobal.provider.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class ProviderMenuRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/provider.xml");
    }

    @Test
    public void getUrl() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/ProviderMenu").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String formJson = urlResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> formMap = objectMapper.readValue(formJson, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("providerMenuForm", formMap.get("formName"));
        assertEquals("Home", formMap.get("cancelAction"));
        assertEquals("POST", formMap.get("cancelMethod"));

    }
}