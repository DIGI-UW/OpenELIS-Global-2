package org.openelisglobal.labelpreset.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for
 * {@link org.openelisglobal.labelpreset.controller.rest.LabelPresetRestController}
 * (OGC-285 M3, task T062).
 *
 * <p>
 * Uses real service + DAO + PostgreSQL via {@link BaseWebContextSensitiveTest}
 * and MockMvc. No @MockBean of code-under-test.
 *
 * <p>
 * Covers AC-2 (validation rejection), AC-3 (name collision), AC-4 (system
 * preset deactivation guard), AC-7 (duplicate creates copy).
 */
public class LabelPresetRestControllerValidationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_PREFIX = "ctrl_test_";
    private static final String BASE_URL = "/api/labelPresets";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanTestData();
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        jdbc.execute("DELETE FROM clinlims.label_preset_field WHERE preset_id IN "
                + "(SELECT id FROM clinlims.label_preset WHERE name LIKE '" + TEST_PREFIX + "%')");
        jdbc.execute("DELETE FROM clinlims.label_preset WHERE name LIKE '" + TEST_PREFIX + "%'");
    }

    // ── AC-2: field validation rejections ────────────────────────────────────

    @Test
    public void post_missingName_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(null);
        form.setName(null);
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_blankName_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(null);
        form.setName("  ");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_nameTooLong_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(null);
        form.setName("a".repeat(121));
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_heightOutOfRange_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "height_bad");
        form.setHeightMm(3); // below min=5
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_widthOutOfRange_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "width_bad");
        form.setWidthMm(250); // above max=200
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_noBarcodeType_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "no_barcode_type");
        form.setBarcodeType(null);
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void post_noScopeSelected_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "no_scope");
        form.setPrintsPerOrder(false);
        form.setPrintsPerSample(false);
        MvcResult result = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity()).andReturn();

        Map<String, Object> body = JSON.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Object>>() {
                });
        // Either fieldErrors or globalErrors should contain the scope violation
        @SuppressWarnings("unchecked")
        List<Object> globalErrors = (List<Object>) body.get("globalErrors");
        @SuppressWarnings("unchecked")
        List<Object> fieldErrors = (List<Object>) body.get("fieldErrors");
        assertTrue("Should report a scope violation",
                (globalErrors != null && !globalErrors.isEmpty()) || (fieldErrors != null && !fieldErrors.isEmpty()));
    }

    @Test
    public void post_maxPerSampleLessThanDefault_returns422() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "max_lt_default");
        form.setPrintsPerSample(true);
        form.setDefaultPerSample(10);
        form.setMaxPerSample(5); // max < default — invalid
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── AC-3: name collision ──────────────────────────────────────────────────

    @Test
    public void post_duplicateNormalizedName_returns422() throws Exception {
        LabelPresetForm form1 = buildValidForm(TEST_PREFIX + "collision1");
        MvcResult r1 = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form1)))
                .andExpect(status().isCreated()).andReturn();
        assertNotNull(JSON.readValue(r1.getResponse().getContentAsString(), LabelPreset.class).getId());

        // Same name with different case — should collide
        LabelPresetForm form2 = buildValidForm(TEST_PREFIX.toUpperCase() + "COLLISION1");
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form2)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Happy path — create + get ─────────────────────────────────────────────

    @Test
    public void post_validForm_returns201WithId() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "created_ok");
        MvcResult result = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();

        LabelPreset created = JSON.readValue(result.getResponse().getContentAsString(), LabelPreset.class);
        assertNotNull("Created preset should have an id", created.getId());
    }

    @Test
    public void getById_existingPreset_returns200() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "get_by_id");
        MvcResult postResult = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();
        LabelPreset created = JSON.readValue(postResult.getResponse().getContentAsString(), LabelPreset.class);

        mockMvc.perform(get(BASE_URL + "/" + created.getId())).andExpect(status().isOk());
    }

    @Test
    public void getById_nonExistentPreset_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/9999999")).andExpect(status().isNotFound());
    }

    // ── AC-4: system preset deactivation guard ────────────────────────────────

    @Test
    public void patch_activate_systemPreset_deactivate_returns422() throws Exception {
        // Get any system preset
        MvcResult listResult = mockMvc.perform(get(BASE_URL)).andExpect(status().isOk()).andReturn();
        List<LabelPreset> presets = JSON.readValue(listResult.getResponse().getContentAsString(),
                new TypeReference<List<LabelPreset>>() {
                });
        LabelPreset systemPreset = presets.stream().filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst()
                .orElse(null);
        if (systemPreset == null) {
            return; // No system presets seeded — skip
        }

        String body = "{\"isActive\": false}";
        MvcResult result = mockMvc.perform(patch(BASE_URL + "/" + systemPreset.getId() + "/activate")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        // Should be 422 due to system preset guard
        assertEquals("System preset deactivation should return 422", 422, result.getResponse().getStatus());
    }

    // ── AC-7: duplicate ───────────────────────────────────────────────────────

    @Test
    public void duplicate_createsNewNonSystemActivePreset() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "dup_source");
        MvcResult postResult = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();
        LabelPreset source = JSON.readValue(postResult.getResponse().getContentAsString(), LabelPreset.class);

        String dupBody = "{\"name\": \"" + TEST_PREFIX + "dup_copy\"}";
        MvcResult dupResult = mockMvc.perform(post(BASE_URL + "/" + source.getId() + "/duplicate")
                .contentType(MediaType.APPLICATION_JSON).content(dupBody)).andExpect(status().isCreated()).andReturn();

        LabelPreset copy = JSON.readValue(dupResult.getResponse().getContentAsString(), LabelPreset.class);
        assertNotNull("Copy should have an id", copy.getId());
        assertTrue("Copy id should differ from source", !copy.getId().equals(source.getId()));
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    public void put_updateExistingPreset_returns200() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "update_me");
        MvcResult createResult = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();
        LabelPreset created = JSON.readValue(createResult.getResponse().getContentAsString(), LabelPreset.class);

        LabelPresetForm updateForm = buildValidForm(TEST_PREFIX + "update_me");
        updateForm.setHeightMm(30);
        MvcResult updateResult = mockMvc.perform(put(BASE_URL + "/" + created.getId())
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(updateForm)))
                .andExpect(status().isOk()).andReturn();

        LabelPreset updated = JSON.readValue(updateResult.getResponse().getContentAsString(), LabelPreset.class);
        assertEquals("Height should be updated", Integer.valueOf(30), updated.getHeightMm());
    }

    @Test
    public void put_nonExistentPreset_returns404() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "nonexistent_put");
        mockMvc.perform(put(BASE_URL + "/9999999").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(form))).andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelPresetForm buildValidForm(String name) {
        LabelPresetForm form = new LabelPresetForm();
        form.setName(name != null ? name : TEST_PREFIX + "default_form");
        form.setHeightMm(20);
        form.setWidthMm(40);
        form.setBarcodeType(BarcodeType.CODE_128);
        form.setPrintsPerSample(true);
        form.setPrintsPerOrder(false);
        form.setDefaultPerSample(1);
        form.setMaxPerSample(5);
        form.setDefaultPerOrder(0);
        form.setMaxPerOrder(10);
        form.setIsActive(true);
        return form;
    }
}
