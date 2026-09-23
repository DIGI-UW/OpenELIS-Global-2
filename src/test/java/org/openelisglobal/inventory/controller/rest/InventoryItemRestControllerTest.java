package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
 * Covers the REST paths for {@code inventory_item.code}: generation, explicit
 * codes, the duplicate 400 body, and the code surviving an update.
 */
public class InventoryItemRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "ITTEST-";

    // Names carry this token so their generated codes fall under one prefix this
    // suite owns.
    private static final String GENERATED_PREFIX = "ITT-4471";

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

    // Prefix-scoped, never TRUNCATE: other suites' fixtures share inventory_item.
    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_item WHERE code LIKE ? OR code LIKE ?", CODE_PREFIX + "%",
                GENERATED_PREFIX + "-%");
        jdbc.update("DELETE FROM clinlims.inventory_item_code_sequence WHERE prefix = ?", GENERATED_PREFIX);
        // Tag directory rows outlive the items that referenced them, so a retired
        // tag left behind would leak into the next run's suggestion list.
        jdbc.update("DELETE FROM clinlims.inventory_tag WHERE name LIKE ?", CODE_PREFIX + "%");
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
        MvcResult result = createItem(null, "Ittest Import Widget 4471");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(GENERATED_PREFIX + "-001", created.get("code").asText());
        assertNotNull("The surrogate id is still assigned by the sequence", created.get("id"));
    }

    @Test
    public void create_acceptsExplicitCode() throws Exception {
        MvcResult result = createItem(CODE_PREFIX + "explicit_code", CODE_PREFIX + "Explicit");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(CODE_PREFIX + "EXPLICIT-CODE", created.get("code").asText());
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

    /**
     * The whitelist in the update handler is hand-maintained, and a field missing
     * from it returns 200 while persisting nothing. That is exactly how editing a
     * lead time silently did nothing until it was found, so a tag edit is proved
     * over HTTP and read back rather than asserted on the response alone.
     */
    @Test
    public void update_persistsTagsAndReadsThemBack() throws Exception {
        MvcResult createResult = createItem(CODE_PREFIX + "TAGGED", CODE_PREFIX + "Tagged");
        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        HashMap<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", CODE_PREFIX + "Tagged");
        updateBody.put("units", "mL");
        updateBody.put("tags", List.of("Cartridge", "TB"));

        mockMvc.perform(put("/rest/inventory/items/" + id).session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody))).andExpect(status().isOk());

        MvcResult getResult = mockMvc.perform(get("/rest/inventory/items/" + id)).andExpect(status().isOk())
                .andReturn();
        JsonNode fetched = objectMapper.readTree(getResult.getResponse().getContentAsString());
        List<String> persisted = new ArrayList<>();
        fetched.get("tags").forEach(tag -> persisted.add(tag.asText()));
        Collections.sort(persisted);

        assertEquals(List.of("Cartridge", "TB"), persisted);
    }

    /** Create no longer has to be told a type; the editor stopped sending one. */
    @Test
    public void create_succeedsWithoutAnItemType() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", CODE_PREFIX + "No Type");
        body.put("units", "tests");
        body.put("tags", List.of("Consumable"));

        MvcResult result = mockMvc.perform(post("/rest/inventory/items").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andReturn();

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("REAGENT", created.get("itemType").asText());
        assertEquals("Consumable", created.get("tags").get(0).asText());
    }

    /**
     * Over HTTP, not through the service: the frontend reads this endpoint, and a
     * service-level test would have passed while the controller went on calling the
     * old method that knows nothing about retirement.
     */
    @Test
    public void tagsEndpointLeavesOutARetiredTag() throws Exception {
        String tag = CODE_PREFIX + "Retired";
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", CODE_PREFIX + "Retiring");
        body.put("units", "tests");
        body.put("tags", List.of(tag));
        mockMvc.perform(post("/rest/inventory/items").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))).andExpect(status().isCreated());
        assertTrue(suggestedTags().contains(tag));

        HashMap<String, Object> retire = new HashMap<>();
        retire.put("name", tag);
        mockMvc.perform(post("/rest/inventory/tags/deactivate").session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(retire)))
                .andExpect(status().isOk());

        assertFalse("a retired tag is no longer suggested", suggestedTags().contains(tag));
    }

    private List<String> suggestedTags() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/inventory/items/tags")).andExpect(status().isOk()).andReturn();
        List<String> tags = new ArrayList<>();
        objectMapper.readTree(result.getResponse().getContentAsString()).forEach(tag -> tags.add(tag.asText()));
        return tags;
    }

    @Test
    public void tagsEndpointListsWhatIsInUse() throws Exception {
        HashMap<String, Object> body = new HashMap<>();
        body.put("name", CODE_PREFIX + "Suggestible");
        body.put("units", "tests");
        body.put("tags", List.of(CODE_PREFIX + "Fridge"));
        mockMvc.perform(post("/rest/inventory/items").session(mockSession).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))).andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/rest/inventory/items/tags")).andExpect(status().isOk()).andReturn();

        List<String> tags = new ArrayList<>();
        objectMapper.readTree(result.getResponse().getContentAsString()).forEach(tag -> tags.add(tag.asText()));
        assertTrue(tags.contains(CODE_PREFIX + "Fridge"));
    }

    @Test
    public void update_leavesCodeUntouched() throws Exception {
        MvcResult createResult = createItem(CODE_PREFIX + "LOCKED", CODE_PREFIX + "Locked");
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = created.get("id").asText();
        String code = created.get("code").asText();

        HashMap<String, Object> updateBody = new HashMap<>();
        updateBody.put("code", CODE_PREFIX + "SHOULD-NOT-APPLY");
        updateBody.put("name", CODE_PREFIX + "Locked Renamed");
        updateBody.put("itemType", "REAGENT");
        updateBody.put("units", "mL");

        MvcResult updateResult = mockMvc.perform(put("/rest/inventory/items/" + id).session(mockSession)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk()).andReturn();

        JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals("The update handler does not copy code", code, updated.get("code").asText());
        assertEquals(CODE_PREFIX + "Locked Renamed", updated.get("name").asText());

        MvcResult getResult = mockMvc.perform(get("/rest/inventory/items/" + id)).andExpect(status().isOk())
                .andReturn();
        JsonNode fetched = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertEquals(code, fetched.get("code").asText());
    }

    @Test
    public void create_stripsPunctuation_whenGeneratingCodeFromName() throws Exception {
        MvcResult result = createItem(null, "Ittest Punctuation!!! 44.71% Test");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(GENERATED_PREFIX + "-001", created.get("code").asText());
    }
}
