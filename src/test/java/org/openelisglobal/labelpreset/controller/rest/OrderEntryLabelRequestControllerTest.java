package org.openelisglobal.labelpreset.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestPayload;
import org.openelisglobal.labelpreset.service.TestLabelPresetLinkService;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for {@link OrderEntryLabelRequestController} (OGC-285 M5).
 * POSTs the {@code contracts/openapi.yaml} §8.1 request shape and asserts the
 * response JSON matches the contract structure (snake_case keys, column + cell
 * shape, source tag).
 *
 * <p>
 * Real Spring stack: real controller + service + DAO + PostgreSQL via
 * {@link BaseWebContextSensitiveTest} and MockMvc — no @MockBean of
 * code-under-test.
 */
public class OrderEntryLabelRequestControllerTest extends BaseWebContextSensitiveTest {

    private static final String URL = "/api/orderEntry/labelRequest";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SPECIMEN_PRESET = "T133-CtrlSpecimen";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private TestService testService;

    private JdbcTemplate jdbc;
    private LabelPreset specimenPreset;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanTestData();

        specimenPreset = savePerSamplePreset(SPECIMEN_PRESET);
        TestLabelPresetLink link = new TestLabelPresetLink();
        link.setTest(testService.getTestById("1"));
        link.setPreset(specimenPreset);
        link.setDefaultQty(1);
        link.setMaxQty(5);
        link.setAllowOverride(true);
        link.setSysUserId(TEST_SYS_USER_ID);
        testLabelPresetLinkService.insert(link);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void post_returnsContractShape_withColumnsRowsAndSourceTags() throws Exception {
        OrderEntryLabelRequestPayload payload = new OrderEntryLabelRequestPayload();
        payload.setTestIds(List.of(1L));
        OrderEntryLabelRequestPayload.SampleRef sample = new OrderEntryLabelRequestPayload.SampleRef();
        sample.setSampleIdLocal("S1");
        sample.setSampleType("BLOOD_EDTA");
        payload.setSamples(List.of(sample));

        MvcResult result = mockMvc
                .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(payload)))
                .andExpect(status().isOk())
                // top-level contract keys (snake_case)
                .andExpect(jsonPath("$.order_columns").isArray()).andExpect(jsonPath("$.sample_columns").isArray())
                .andExpect(jsonPath("$.order_row.cells").isArray()).andExpect(jsonPath("$.sample_rows").isArray())
                .andExpect(jsonPath("$.sample_rows[0].sample_id_local").value("S1")).andReturn();

        // Parse the body and assert the contract shape in Java (robust to column
        // ordering and to coexisting seeded system presets).
        JsonNode root = JSON.readTree(result.getResponse().getContentAsString());

        JsonNode specimenColumn = findByPresetId(root.get("sample_columns"), specimenPreset.getId());
        assertNotNull("the linked custom specimen preset is a sample column", specimenColumn);
        assertEquals(SPECIMEN_PRESET, specimenColumn.get("name").asText());
        assertFalse("custom preset is not a system preset", specimenColumn.get("is_system").asBoolean());
        assertEquals(5, specimenColumn.get("max").asInt());

        JsonNode sampleCells = root.get("sample_rows").get(0).get("cells");
        JsonNode specimenCell = findByPresetId(sampleCells, specimenPreset.getId());
        assertNotNull("per-sample cell for the linked preset", specimenCell);
        assertEquals("test", specimenCell.get("source").asText());
        assertEquals(1, specimenCell.get("default").asInt());
        assertEquals(5, specimenCell.get("max").asInt());
        assertFalse(specimenCell.get("locked").asBoolean());
        assertEquals(1L, specimenCell.get("source_test_id").asLong());
        assertTrue("source_test_name present for a test-driven cell", specimenCell.hasNonNull("source_test_name"));
    }

    /** First array element whose {@code preset_id} matches, or null. */
    private JsonNode findByPresetId(JsonNode array, Integer presetId) {
        if (array == null) {
            return null;
        }
        for (JsonNode node : array) {
            if (node.hasNonNull("preset_id") && node.get("preset_id").asInt() == presetId) {
                return node;
            }
        }
        return null;
    }

    @Test
    public void post_emptyOrder_stillReturnsPerOrderColumns() throws Exception {
        OrderEntryLabelRequestPayload payload = new OrderEntryLabelRequestPayload();
        payload.setTestIds(List.of());
        payload.setSamples(List.of());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.order_columns").isArray())
                .andExpect(jsonPath("$.sample_rows").isArray());
    }

    private LabelPreset savePerSamplePreset(String name) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(25);
        preset.setWidthMm(50);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(false);
        preset.setPrintsPerSample(true);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(1);
        preset.setMaxPerSample(5);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        labelPresetDAO.insert(preset);
        return preset;
    }

    private void cleanTestData() {
        try {
            jdbc.update("DELETE FROM test_label_preset_link WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T133-%')");
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'T133-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
