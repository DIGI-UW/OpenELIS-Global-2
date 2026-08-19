package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-658 Part C — {@code inventory_item} gains a human-readable {@code code}
 * column alongside its surrogate primary key. Covers auto-generation from the
 * name, explicit codes, duplicate rejection with a translatable 400, and the
 * code surviving an update untouched.
 *
 * <p>
 * Never truncates {@code inventory_item} (shared with other suites via
 * {@code inventory-test-data.xml} fixtures) — only inserts/deletes rows scoped
 * to a unique {@code ITTEST_} code prefix.
 */
public class InventoryItemRestControllerIT extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "ITTEST_";

    @Autowired
    private javax.sql.DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbc;
    private MockHttpSession mockSession;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbc = new JdbcTemplate(dataSource);
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ?", CODE_PREFIX + "%");
    }

    private MvcResult createItem(String code, String name) throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", name);
        body.put("itemType", "REAGENT");
        body.put("units", "mL");
        return mockMvc.perform(post("/rest/inventory/items").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andReturn();
    }

    @Test
    public void create_autoGeneratesCodeFromName_whenCodeBlank() throws Exception {
        MvcResult result = createItem(null, CODE_PREFIX + "Import Widget");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("ITTEST_IMPORT_WIDGET", created.get("code").asText());
        assertNotNull("The surrogate id is still assigned by the sequence", created.get("id"));
    }

    @Test
    public void create_acceptsExplicitCode() throws Exception {
        MvcResult result = createItem(CODE_PREFIX + "explicit_code", CODE_PREFIX + "Explicit");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(CODE_PREFIX + "EXPLICIT_CODE", created.get("code").asText());
    }

    @Test
    public void create_rejectsDuplicateCode() throws Exception {
        createItem(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup");

        MvcResult result = createItem(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup Again");

        assertEquals(400, result.getResponse().getStatus());
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("inventory.item.error.duplicateCode", body.get("errorCode").asText());
        assertEquals(CODE_PREFIX + "DUP", body.get("params").get("code").asText());
    }

    @Test
    public void update_leavesCodeUntouched() throws Exception {
        MvcResult createResult = createItem(CODE_PREFIX + "LOCKED", CODE_PREFIX + "Locked");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = created.get("id").asText();
        String code = created.get("code").asText();

        HashMap<String, Object> updateBody = new HashMap<>();
        updateBody.put("code", CODE_PREFIX + "SHOULD_NOT_APPLY");
        updateBody.put("name", CODE_PREFIX + "Locked Renamed");
        updateBody.put("itemType", "REAGENT");
        updateBody.put("units", "mL");

        MvcResult updateResult = mockMvc.perform(put("/rest/inventory/items/" + id).session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk()).andReturn();

        JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals("Derived lot numbers embed the code, so it stays put", code, updated.get("code").asText());
        assertEquals(CODE_PREFIX + "Locked Renamed", updated.get("name").asText());

        MvcResult getResult = mockMvc.perform(get("/rest/inventory/items/" + id)).andExpect(status().isOk())
                .andReturn();
        JsonNode fetched = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertEquals(code, fetched.get("code").asText());
    }

    @Test
    public void create_stripsPunctuation_whenGeneratingCodeFromName() throws Exception {
        MvcResult result = createItem(null, CODE_PREFIX + "Punctuation!!! Test");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("ITTEST_PUNCTUATION_TEST", created.get("code").asText());
    }
}
