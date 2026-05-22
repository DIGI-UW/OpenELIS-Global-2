package org.openelisglobal.externalconnection.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class ExternalConnectionMenuRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/external-connection.xml");
    }

    @Test
    public void showExternalConnectionMenu() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/ExternalConnectionMenu")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String formJson = urlResult.getResponse().getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Object> formMap = objectMapper.readValue(formJson, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("ExternalConnectionMenuForm", formMap.get("formName"));
        assertEquals("Home", formMap.get("cancelAction"));
        assertEquals("POST", formMap.get("cancelMethod"));

    }

    @Test
    public void deactivateExternalConnection_shouldSetExternalConnectionActivefalse() throws Exception {
        MvcResult urlResult = super.mockMvc
                .perform(post("/rest/DeactivateExternalConnection").accept(MediaType.APPLICATION_JSON_VALUE)
                        .param("ID", "1").contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        int status = urlResult.getResponse().getStatus();
        assertTrue("Status should be 200", 200 == status);
        String content = urlResult.getResponse().getContentAsString();
        assertEquals("External connection(s) deactivated successfully.", content);
        ExternalConnection connection = externalConnectionService.get(1);
        assertFalse(connection.getActive());

    }

}
