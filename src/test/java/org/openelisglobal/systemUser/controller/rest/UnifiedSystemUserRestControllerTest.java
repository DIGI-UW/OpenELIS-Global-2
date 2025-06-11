package org.openelisglobal.systemUser.controller.rest;

import static org.junit.Assert.assertEquals;
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

public class UnifiedSystemUserRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/system-user.xml");

    }

    @Test
    public void getUsersWithRole_shouldReturnSystemUsersWithRoles() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/users").accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        String results = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> users = objectMapper.readValue(results,
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(4, users.size());

        Map<String, Object> user1 = users.get(1);
        assertEquals("3", user1.get("id"));
        assertEquals("Doe,John", user1.get("value"));

        Map<String, Object> user2 = users.get(2);
        assertEquals("4", user2.get("id"));
        assertEquals("Smith,Alice", user2.get("value"));

        Map<String, Object> user3 = users.get(3);
        assertEquals("5", user3.get("id"));
        assertEquals("White,Bob", user3.get("value"));
    }

    @Test
    public void getUsersWithRole_shouldReturnSystemUsersGivenThereRole() throws Exception {
        MvcResult urlResult = super.mockMvc.perform(get("/rest/users/adminRole")
                .accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn();
        String results = urlResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> users = objectMapper.readValue(results,
                new TypeReference<List<Map<String, Object>>>() {
                });
        assertEquals(3, users.size());

        Map<String, Object> user1 = users.get(1);
        assertEquals("3", user1.get("id"));
        assertEquals("Doe,John", user1.get("value"));

        Map<String, Object> user2 = users.get(2);
        assertEquals("4", user2.get("id"));
        assertEquals("Smith,Alice", user2.get("value"));
    }

}