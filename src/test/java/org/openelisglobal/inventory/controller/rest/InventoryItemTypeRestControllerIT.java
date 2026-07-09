package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-658 Part A — the admin-managed inventory item type lookup table that
 * replaces the hardcoded {@code InventoryEnums.ItemType} enum. Covers both the
 * admin CRUD endpoints ({@code /rest/inventory-item-types}) and the public
 * dropdown endpoint ({@code /rest/inventory/items/types}) that the Inventory
 * Catalog / Item Add form consumes.
 *
 * <p>
 * {@code inventory_item_type} carries real Liquibase-seeded production data
 * (the 5 original enum values), so this test never truncates it — it only
 * inserts/deletes rows scoped to a unique {@code ITTEST_} code prefix,
 * mirroring the raw-JDBC cleanup pattern used by
 * {@code TestReagentLinkRestControllerIntegrationTest}.
 */
public class InventoryItemTypeRestControllerIT extends BaseWebContextSensitiveTest {

    private static final String CODE_PREFIX = "ITTEST_";

    @Autowired
    private javax.sql.DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.inventory_item_type WHERE code LIKE ?", CODE_PREFIX + "%");
        jdbc.update(
                "DELETE FROM clinlims.localization_value WHERE localization_id IN "
                        + "(SELECT id FROM clinlims.localization WHERE description LIKE ?)",
                "inventory item type: " + CODE_PREFIX + "%");
        jdbc.update("DELETE FROM clinlims.localization WHERE description LIKE ?",
                "inventory item type: " + CODE_PREFIX + "%");
    }

    private MvcResult createType(String code, String name, String locale, int sortOrder) throws Exception {
        return createType(code, name, locale, sortOrder, null);
    }

    private MvcResult createType(String code, String name, String locale, int sortOrder, Boolean active)
            throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
            {
                put("code", code);
                put("name", name);
                put("locale", locale);
                put("sortOrder", sortOrder);
                put("active", active);
            }
        });
        return mockMvc.perform(post("/rest/inventory-item-types").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
    }

    @Test
    public void getAll_includesSeededTypesOrderedBySortOrder() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/inventory-item-types")).andExpect(status().isOk()).andReturn();

        JsonNode types = objectMapper.readTree(result.getResponse().getContentAsString());
        java.util.Map<String, Integer> sortOrderByCode = new java.util.HashMap<>();
        for (JsonNode type : types) {
            sortOrderByCode.put(type.get("code").asText(), type.get("sortOrder").asInt());
        }
        assertTrue("Seeded REAGENT type should be present", sortOrderByCode.containsKey("REAGENT"));
        assertTrue("Seeded SYPHILIS_KIT type should be present", sortOrderByCode.containsKey("SYPHILIS_KIT"));
        assertTrue("REAGENT should sort before RDT", sortOrderByCode.get("REAGENT") < sortOrderByCode.get("RDT"));
        assertTrue("RDT should sort before CARTRIDGE", sortOrderByCode.get("RDT") < sortOrderByCode.get("CARTRIDGE"));

        int previousSortOrder = Integer.MIN_VALUE;
        for (JsonNode type : types) {
            int sortOrder = type.get("sortOrder").asInt();
            assertTrue("Response should be ordered ascending by sortOrder", sortOrder >= previousSortOrder);
            previousSortOrder = sortOrder;
        }
    }

    @Test
    public void create_autoGeneratesCodeFromName_whenCodeBlank() throws Exception {
        MvcResult result = createType(null, CODE_PREFIX + "Import Widget", "en", 500);
        result.getResponse().setCharacterEncoding("UTF-8");

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("ITTEST_IMPORT_WIDGET", created.get("code").asText());
        assertEquals("ITTEST_Import Widget", created.get("name").asText());
        assertFalse("New types must not be marked seeded", created.get("seeded").asBoolean());
        assertTrue("New types default to active", created.get("active").asBoolean());
    }

    @Test
    public void create_respectsExplicitInactiveFlag() throws Exception {
        MvcResult result = createType(CODE_PREFIX + "INACTIVE", CODE_PREFIX + "Inactive On Create", "en", 507, false);

        assertEquals(201, result.getResponse().getStatus());
        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse("Explicit active=false must be honored on create", created.get("active").asBoolean());
    }

    @Test
    public void create_rejectsDuplicateCode() throws Exception {
        createType(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup", "en", 501);

        MvcResult result = createType(CODE_PREFIX + "DUP", CODE_PREFIX + "Dup Again", "en", 502);

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void create_rejectsBlankName() throws Exception {
        MvcResult result = createType(CODE_PREFIX + "BLANK", "   ", "en", 503);

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void update_changesNameForLocaleAndSortOrder() throws Exception {
        MvcResult createResult = createType(CODE_PREFIX + "UPD", CODE_PREFIX + "Original", "en", 504);
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
            {
                put("name", CODE_PREFIX + "Renamed");
                put("locale", "en");
                put("sortOrder", 505);
            }
        });
        MvcResult updateResult = mockMvc.perform(
                put("/rest/inventory-item-types/" + id).contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk()).andReturn();

        JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertEquals(CODE_PREFIX + "Renamed", updated.get("name").asText());
        assertEquals(505, updated.get("sortOrder").asInt());
        assertEquals(CODE_PREFIX + "UPD", updated.get("code").asText());
    }

    @Test
    public void update_returnsBadRequest_whenTypeMissing() throws Exception {
        String updateBody = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
            {
                put("name", "Does not matter");
                put("locale", "en");
                put("sortOrder", 1);
            }
        });

        MvcResult result = mockMvc.perform(
                put("/rest/inventory-item-types/999999999").contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void deactivate_hidesFromItemTypeDropdown_butKeepsInAdminList() throws Exception {
        MvcResult createResult = createType(CODE_PREFIX + "DEACT", CODE_PREFIX + "Deactivateable", "en", 506);
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.get("id").asLong();

        mockMvc.perform(put("/rest/inventory-item-types/" + id + "/deactivate")).andExpect(status().isOk());

        MvcResult adminListResult = mockMvc.perform(get("/rest/inventory-item-types")).andExpect(status().isOk())
                .andReturn();
        JsonNode adminList = objectMapper.readTree(adminListResult.getResponse().getContentAsString());
        boolean foundInactive = false;
        for (JsonNode type : adminList) {
            if (type.get("code").asText().equals(CODE_PREFIX + "DEACT")) {
                foundInactive = true;
                assertFalse("Deactivated type should be inactive", type.get("active").asBoolean());
            }
        }
        assertTrue("Deactivated type should still appear in the admin list", foundInactive);

        MvcResult dropdownResult = mockMvc.perform(get("/rest/inventory/items/types")).andExpect(status().isOk())
                .andReturn();
        JsonNode dropdown = objectMapper.readTree(dropdownResult.getResponse().getContentAsString());
        for (JsonNode option : dropdown) {
            assertFalse("Deactivated type must not appear in the active dropdown",
                    option.get("code").asText().equals(CODE_PREFIX + "DEACT"));
        }
    }

    @Test
    public void deactivate_returnsBadRequest_whenTypeMissing() throws Exception {
        MvcResult result = mockMvc.perform(put("/rest/inventory-item-types/999999999/deactivate")).andReturn();

        assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    public void itemTypesDropdown_returnsActiveSeededTypesWithResolvedLabels() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/inventory/items/types")).andExpect(status().isOk()).andReturn();

        JsonNode options = objectMapper.readTree(result.getResponse().getContentAsString());
        java.util.Map<String, String> labelByCode = new java.util.HashMap<>();
        for (JsonNode option : options) {
            labelByCode.put(option.get("code").asText(), option.get("label").asText());
        }
        assertEquals("Reagent", labelByCode.get("REAGENT"));
        assertEquals("RDT (Rapid Diagnostic Test)", labelByCode.get("RDT"));
        assertEquals("Analyzer Cartridge", labelByCode.get("CARTRIDGE"));
        assertEquals("HIV Test Kit", labelByCode.get("HIV_KIT"));
        assertEquals("Syphilis Test Kit", labelByCode.get("SYPHILIS_KIT"));
    }
}
