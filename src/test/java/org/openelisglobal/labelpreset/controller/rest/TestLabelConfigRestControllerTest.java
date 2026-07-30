package org.openelisglobal.labelpreset.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.TestLabelConfigDAO;
import org.openelisglobal.labelpreset.dao.TestLabelPresetLinkDAO;
import org.openelisglobal.labelpreset.form.TestLabelConfigForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for {@link TestLabelConfigRestController} (OGC-285 M4).
 *
 * <p>
 * Uses {@link BaseWebContextSensitiveTest} with a real Spring context, real
 * services, real DAOs and real DB. No {@code @MockBean} of any service.
 * {@code BaseWebContextSensitiveTest.setDefaultTestAuthentication} grants
 * ROLE_ADMIN + ROLE_RESULTS, matching the
 * {@code @PreAuthorize("hasRole('ADMIN')")} on the controller.
 * </p>
 *
 * <p>
 * Test coverage (from tasks.md T102):
 * </p>
 * <ul>
 * <li>GET returns default (toggle=true, empty links) when no config
 * persisted.</li>
 * <li>GET returns existing config after PUT.</li>
 * <li>PUT happy path persists config + links; reload confirms round-trip.</li>
 * <li>PUT with order-only preset (prints_per_sample=false) returns 422.</li>
 * <li>PUT with duplicate presetId in links returns 422.</li>
 * </ul>
 */
public class TestLabelConfigRestControllerTest extends BaseWebContextSensitiveTest {

    private static final String PRESET_PER_SAMPLE_NAME = "T102-PerSample-Preset";
    private static final String PRESET_ORDER_ONLY_NAME = "T102-OrderOnly-Preset";

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkDAO testLabelPresetLinkDAO;

    @Autowired
    private TestLabelConfigDAO testLabelConfigDAO;

    @Autowired
    private TestService testService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;
    private LabelPreset perSamplePreset;
    private LabelPreset orderOnlyPreset;
    private org.openelisglobal.test.valueholder.Test bloodTest;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanTestData();

        bloodTest = testService.getTestById("1");
        assertNotNull("fixture Test id=1 must exist", bloodTest);

        perSamplePreset = saveLabelPreset(PRESET_PER_SAMPLE_NAME, false, true);
        orderOnlyPreset = saveLabelPreset(PRESET_ORDER_ONLY_NAME, true, false);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    /**
     * Remove test data in FK order. Identified by test-specific preset names so a
     * crashed prior run cannot collide with label_preset UNIQUE(name).
     */
    private void cleanTestData() {
        try {
            // Links referencing our test presets
            jdbcTemplate.update(
                    "DELETE FROM test_label_preset_link WHERE preset_id IN"
                            + " (SELECT id FROM label_preset WHERE name IN (?, ?))",
                    PRESET_PER_SAMPLE_NAME, PRESET_ORDER_ONLY_NAME);
            // TestLabelConfig for Test id=1 (bloodTest)
            jdbcTemplate.update("DELETE FROM test_label_config WHERE test_id = 1");
            // The presets themselves
            jdbcTemplate.update("DELETE FROM label_preset WHERE name IN (?, ?)", PRESET_PER_SAMPLE_NAME,
                    PRESET_ORDER_ONLY_NAME);
        } catch (Exception e) {
            // Ignore cleanup errors — next run will retry.
        }
    }

    private LabelPreset saveLabelPreset(String name, boolean printsPerOrder, boolean printsPerSample) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(30);
        preset.setWidthMm(60);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(printsPerOrder);
        preset.setPrintsPerSample(printsPerSample);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(1);
        preset.setMaxPerSample(5);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        Integer id = labelPresetDAO.insert(preset);
        assertNotNull("LabelPreset insert should return id", id);
        return preset;
    }

    private TestLabelConfigForm formWithPerSamplePreset() {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
        entry.setPresetId(perSamplePreset.getId());
        entry.setDefaultQty(1);
        entry.setMaxQty(5);
        entry.setAllowOverride(true);
        links.add(entry);
        form.setLinks(links);
        return form;
    }

    // -----------------------------------------------------------------------
    // T102a: GET returns default when no config persisted
    // -----------------------------------------------------------------------

    @Test
    public void getLabelConfig_returnsDefaultWhenNotPersisted() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/api/tests/1/labelConfig").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(result.getResponse().getContentAsString(), Map.class);

        assertEquals("default allowOrderEntryOverride should be true", Boolean.TRUE,
                body.get("allowOrderEntryOverride"));
        assertNotNull("links key must be present", body.get("links"));
        @SuppressWarnings("unchecked")
        List<?> links = (List<?>) body.get("links");
        assertTrue("links must be empty for unpersisted test", links.isEmpty());
    }

    // -----------------------------------------------------------------------
    // T102b: PUT happy path + GET round-trip
    // -----------------------------------------------------------------------

    @Test
    public void putLabelConfig_happyPath_persistsAndReloads() throws Exception {
        TestLabelConfigForm form = formWithPerSamplePreset();
        String json = new ObjectMapper().writeValueAsString(form);

        MvcResult putResult = mockMvc
                .perform(put("/rest/api/tests/1/labelConfig").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> putBody = mapper.readValue(putResult.getResponse().getContentAsString(), Map.class);
        assertEquals("PUT response toggle should match form", Boolean.TRUE, putBody.get("allowOrderEntryOverride"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> putLinks = (List<Map<String, Object>>) putBody.get("links");
        assertEquals("PUT should return 1 link", 1, putLinks.size());
        assertEquals("link presetId should round-trip", perSamplePreset.getId(), putLinks.get(0).get("presetId"));

        // Verify via GET that the state is durable
        MvcResult getResult = mockMvc.perform(get("/rest/api/tests/1/labelConfig").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        @SuppressWarnings("unchecked")
        Map<String, Object> getBody = mapper.readValue(getResult.getResponse().getContentAsString(), Map.class);
        assertEquals("GET should reflect persisted toggle", Boolean.TRUE, getBody.get("allowOrderEntryOverride"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> getLinks = (List<Map<String, Object>>) getBody.get("links");
        assertEquals("GET should return 1 persisted link", 1, getLinks.size());
    }

    // -----------------------------------------------------------------------
    // T102c: PUT with order-only preset -> 422
    // This is the key inversion-test target per T120: if assertPerSamplePreset
    // is gutted, this test must fail.
    // -----------------------------------------------------------------------

    @Test
    public void putLabelConfig_orderOnlyPreset_returns422() throws Exception {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();
        TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
        entry.setPresetId(orderOnlyPreset.getId());
        entry.setDefaultQty(1);
        entry.setMaxQty(5);
        entry.setAllowOverride(true);
        links.add(entry);
        form.setLinks(links);

        mockMvc.perform(put("/rest/api/tests/1/labelConfig").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isUnprocessableEntity());
    }

    // -----------------------------------------------------------------------
    // T102d: PUT with duplicate presetId -> 422 (AC-11)
    // -----------------------------------------------------------------------

    @Test
    public void putLabelConfig_duplicatePresetId_returns422() throws Exception {
        TestLabelConfigForm form = new TestLabelConfigForm();
        form.setAllowOrderEntryOverride(true);
        List<TestLabelConfigForm.LinkEntry> links = new ArrayList<>();

        // Two entries with the same presetId
        for (int i = 0; i < 2; i++) {
            TestLabelConfigForm.LinkEntry entry = new TestLabelConfigForm.LinkEntry();
            entry.setPresetId(perSamplePreset.getId());
            entry.setDefaultQty(1);
            entry.setMaxQty(5);
            entry.setAllowOverride(true);
            links.add(entry);
        }
        form.setLinks(links);

        mockMvc.perform(put("/rest/api/tests/1/labelConfig").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isUnprocessableEntity());
    }

    // -----------------------------------------------------------------------
    // T102e: PUT with master toggle off -> GET reflects false (AC-12)
    // -----------------------------------------------------------------------

    @Test
    public void putLabelConfig_masterToggleOff_persistedAndReturned() throws Exception {
        TestLabelConfigForm form = formWithPerSamplePreset();
        form.setAllowOrderEntryOverride(false); // AC-12: master toggle off

        mockMvc.perform(put("/rest/api/tests/1/labelConfig").contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(form))).andExpect(status().isOk());

        MvcResult getResult = mockMvc.perform(get("/rest/api/tests/1/labelConfig").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(getResult.getResponse().getContentAsString(), Map.class);
        assertFalse("Master toggle should be false after PUT with false",
                (Boolean) body.get("allowOrderEntryOverride"));
    }
}
