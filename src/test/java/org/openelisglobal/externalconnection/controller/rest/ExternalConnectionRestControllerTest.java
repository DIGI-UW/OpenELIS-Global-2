package org.openelisglobal.externalconnection.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class ExternalConnectionRestControllerTest extends BaseWebContextSensitiveTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/external-connection.xml");
    }

    @Test
    public void showExternalConnection() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/ExternalConnection")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        String formJson = urlResult.getResponse().getContentAsString();
        int status = urlResult.getResponse().getStatus();
        assertEquals(200, status);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> response = objectMapper.readValue(formJson, new TypeReference<Map<String, Object>>() {
        });

        assertEquals("POST", response.get("formMethod"));
        assertEquals("Home", response.get("cancelAction"));
        assertEquals(false, response.get("submitOnCancel"));
        assertEquals("POST", response.get("cancelMethod"));

        @SuppressWarnings("unchecked")
        List<String> authTypes = (List<String>) response.get("authenticationTypes");
        assertEquals(3, authTypes.size());
        assertTrue(authTypes.contains("CERTIFICATE"));
        assertTrue(authTypes.contains("BASIC"));
        assertTrue(authTypes.contains("NONE"));

        @SuppressWarnings("unchecked")
        List<String> programmedConnections = (List<String>) response.get("programmedConnections");
        assertEquals(4, programmedConnections.size());
        assertTrue(programmedConnections.contains("SMPP_SERVER"));
        assertTrue(programmedConnections.contains("BMP_SMS_SERVER"));
        assertTrue(programmedConnections.contains("INFO_HIGHWAY"));
        assertTrue(programmedConnections.contains("SMTP_SERVER"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void showExternalConnection_shouldReturnExternalConnectionGivenId() throws Exception {
        MvcResult urlResult = super.mockMvc
                .perform(get("/rest/ExternalConnection").accept(MediaType.APPLICATION_JSON_VALUE).param("ID", "1")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String formJson = urlResult.getResponse().getContentAsString();
        int status = urlResult.getResponse().getStatus();
        assertTrue(200 == status);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> connectionInfo = objectMapper.readValue(formJson, new TypeReference<Map<String, Object>>() {
        });

        Map<String, Object> externalConnection = (Map<String, Object>) connectionInfo.get("externalConnection");
        Map<String, Object> descriptionLocalization = (Map<String, Object>) externalConnection
                .get("descriptionLocalization");

        assertEquals("Test Description 1", descriptionLocalization.get("description"));
        assertEquals("SMTP_SERVER", externalConnection.get("programmedConnection"));
        assertEquals("BASIC", externalConnection.get("activeAuthenticationType"));

        List<Map<String, Object>> contacts = (List<Map<String, Object>>) connectionInfo
                .get("externalConnectionContacts");
        assertEquals(1, contacts.size());

        Map<String, Object> contactInfo = contacts.get(0);

        assertEquals("Doe", ((Map<String, Object>) contactInfo.get("person")).get("lastName"));
    }

}
