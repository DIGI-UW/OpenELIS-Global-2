package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
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
 * OGC-658 Part C — {@code inventory_item}'s primary key is now a
 * server-generated string code rather than an auto-increment integer. Covers
 * C2/C3 (auto-generate from name), C4 (locked once saved), C8
 * (duplicate/malformed code rejected with a clean 400).
 *
 * <p>
 * Never truncates {@code inventory_item} (shared with other suites via
 * {@code inventory-test-data.xml} fixtures) — only inserts/deletes rows scoped
 * to a unique {@code ITTEST_} code prefix, mirroring
 * {@code InventoryItemTypeRestControllerIT}.
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
        body.put("id", code);
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
        assertEquals("ITTEST_IMPORT_WIDGET", created.get("id").asText());
    }

    @Test
    public void create_acceptsExplicitCode() throws Exception {
        MvcResult result = createItem(CODE_PREFIX + "explicit_code", CODE_PREFIX + "Explicit");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(CODE_PREFIX + "EXPLICIT_CODE", created.get("id").asText());
    }

    @Test
    public void create_rejectsDuplicateCode() throws Exception {
        createItem(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup");

        MvcResult result = createItem(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup Again");

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void update_doesNotChangeCode_evenWhenDifferentIdSent() throws Exception {
        MvcResult createResult = createItem(CODE_PREFIX + "LOCKED", CODE_PREFIX + "Locked");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String code = created.get("id").asText();

        HashMap<String, Object> updateBody = new HashMap<>();
        updateBody.put("id", CODE_PREFIX + "SHOULD_NOT_APPLY");
        updateBody.put("name", CODE_PREFIX + "Locked Renamed");
        updateBody.put("itemType", "REAGENT");
        updateBody.put("units", "mL");

        MvcResult updateResult = mockMvc
                .perform(put("/rest/inventory/items/" + code).session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk()).andReturn();

        JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals("Code must stay locked once saved", code, updated.get("id").asText());
        assertEquals(CODE_PREFIX + "Locked Renamed", updated.get("name").asText());

        MvcResult getResult = mockMvc.perform(get("/rest/inventory/items/" + code)).andExpect(status().isOk())
                .andReturn();
        JsonNode fetched = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertEquals(code, fetched.get("id").asText());
    }

    @Test
    public void create_stripsPunctuation_whenGeneratingCodeFromName() throws Exception {
        MvcResult result = createItem(null, CODE_PREFIX + "Punctuation!!! Test");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("ITTEST_PUNCTUATION_TEST", created.get("id").asText());
    }
}
