package org.openelisglobal.labelpreset.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.controller.rest.LabelPresetRestController;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Integration tests for
 * {@link org.openelisglobal.labelpreset.controller.rest.LabelPresetRestController}
 * (OGC-285 M3).
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

    @Autowired
    private LabelPresetRestController labelPresetRestController;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanTestData();
        executeDataSetWithStateManagement("testdata/system-user.xml");

        // The backend test classpath has no Jakarta EL implementation, so
        // Spring's OptionalValidatorFactoryBean silently degrades to a no-op and
        // @Valid @RequestBody validation never fires in the shared web context
        // (Tomcat supplies EL in production). Build an EL-free bean validator
        // (Hibernate Validator's ParameterMessageInterpolator) and wire it into
        // an ISOLATED standalone MockMvc, so these tests exercise the
        // controller's real @Valid -> BindingResult -> 422 + buildErrorBody
        // path end-to-end. Unwrap the security CGLIB proxy so @PreAuthorize does
        // not reject the request (auth ordering is covered elsewhere; this class
        // verifies validation). The unwrapped target keeps its real autowired
        // service + DAO + PostgreSQL, so the happy-path/DB cases below are
        // unaffected.
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        LabelPresetRestController target = (LabelPresetRestController) AopProxyUtils
                .getSingletonTarget(labelPresetRestController);
        mockMvc = MockMvcBuilders.standaloneSetup(target != null ? target : labelPresetRestController)
                .setValidator(validator).build();
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
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.CoreMatchers.notNullValue()))
                .andExpect(jsonPath("$.name").value(TEST_PREFIX + "created_ok"))
                .andExpect(jsonPath("$.heightMm").value(20)).andExpect(jsonPath("$.widthMm").value(40))
                .andExpect(jsonPath("$.barcodeType").value("CODE_128"))
                .andExpect(jsonPath("$.printsPerSample").value(true));
    }

    @Test
    public void getById_existingPreset_returns200() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "get_by_id");
        MvcResult postResult = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();
        LabelPreset created = JSON.readValue(postResult.getResponse().getContentAsString(), LabelPreset.class);

        mockMvc.perform(get(BASE_URL + "/" + created.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_PREFIX + "get_by_id"));
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
        assertNotNull("System presets (5 seeded) must be present for the deactivation-guard test", systemPreset);

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
        mockMvc.perform(post(BASE_URL + "/" + source.getId() + "/duplicate").contentType(MediaType.APPLICATION_JSON)
                .content(dupBody)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(TEST_PREFIX + "dup_copy"))
                .andExpect(jsonPath("$.isSystem").value(false)).andExpect(jsonPath("$.isActive").value(true));
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
        mockMvc.perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(updateForm))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TEST_PREFIX + "update_me"))
                .andExpect(jsonPath("$.heightMm").value(30));
    }

    @Test
    public void put_nonExistentPreset_returns404() throws Exception {
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "nonexistent_put");
        mockMvc.perform(put(BASE_URL + "/9999999").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(form))).andExpect(status().isNotFound());
    }

    // ── OGC-1227: the editor's payload, re-saves and partial updates ─────────

    @Test
    public void put_fieldsEchoedFromGet_withUnknownProperties_returns200() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "echo_fields");
        String body = "{\"name\":\"" + TEST_PREFIX + "echo_fields\",\"heightMm\":30,\"widthMm\":40,"
                + "\"barcodeType\":\"CODE_128\",\"printsPerOrder\":false,\"printsPerSample\":true,"
                + "\"defaultPerOrder\":0,\"maxPerOrder\":10,\"defaultPerSample\":1,\"maxPerSample\":5,"
                + "\"isActive\":true,\"isSystem\":false,\"isUniversal\":false,\"lastupdated\":1789153178046,"
                + "\"fields\":[{\"lastupdated\":1789153178046,\"id\":" + created.getFields().get(0).getId()
                + ",\"fieldKey\":\"LAB_NUMBER\",\"sourceType\":\"SYSTEM\",\"isRequired\":true,\"displayOrder\":1}]}";

        MvcResult result = mockMvc
                .perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();

        LabelPreset updated = JSON.readValue(result.getResponse().getContentAsString(), LabelPreset.class);
        assertEquals(Integer.valueOf(30), updated.getHeightMm());
        assertEquals(1, updated.getFields().size());
        assertEquals("LAB_NUMBER", updated.getFields().get(0).getFieldKey());
    }

    @Test
    public void put_secondSaveWithSameFields_returns200() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "resave");
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "resave");
        form.setFields(List.of(fieldEntry("LAB_NUMBER", true, 1)));

        form.setHeightMm(31);
        mockMvc.perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(form))).andExpect(status().isOk());
        form.setHeightMm(32);
        mockMvc.perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(form))).andExpect(status().isOk());

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.label_preset_field WHERE preset_id = ?",
                        Integer.class, created.getId()));
        assertEquals(Integer.valueOf(32), jdbc.queryForObject(
                "SELECT height_mm FROM clinlims.label_preset WHERE id = ?", Integer.class, created.getId()));
    }

    @Test
    public void put_withoutFieldsKey_keepsStoredFields() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "partial");
        String body = "{\"name\":\"" + TEST_PREFIX + "partial\",\"heightMm\":30,\"widthMm\":40,"
                + "\"barcodeType\":\"CODE_128\",\"printsPerOrder\":false,\"printsPerSample\":true,"
                + "\"defaultPerOrder\":0,\"maxPerOrder\":10,\"defaultPerSample\":1,\"maxPerSample\":5,\"isActive\":true}";

        mockMvc.perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        MvcResult getResult = mockMvc.perform(get(BASE_URL + "/" + created.getId())).andExpect(status().isOk())
                .andReturn();
        LabelPreset after = JSON.readValue(getResult.getResponse().getContentAsString(), LabelPreset.class);
        assertEquals(Integer.valueOf(30), after.getHeightMm());
        assertEquals(1, after.getFields().size());
        assertEquals("LAB_NUMBER", after.getFields().get(0).getFieldKey());
    }

    @Test
    public void put_duplicateFieldKey_returns422() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "dup_key");
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "dup_key");
        form.setFields(List.of(fieldEntry("LAB_NUMBER", true, 1), fieldEntry("LAB_NUMBER", false, 2)));

        MvcResult result = mockMvc
                .perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity()).andReturn();

        assertTrue("422 names the rule", result.getResponse().getContentAsString().contains("fields.unique"));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.label_preset_field WHERE preset_id = ?",
                        Integer.class, created.getId()));
    }

    @Test
    public void put_duplicateDisplayOrder_returns422() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "dup_order");
        LabelPresetForm form = buildValidForm(TEST_PREFIX + "dup_order");
        form.setFields(List.of(fieldEntry("LAB_NUMBER", true, 1), fieldEntry("PATIENT_NAME", false, 1)));

        mockMvc.perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(form))).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void put_nullFieldEntry_returns422AndKeepsStoredFields() throws Exception {
        LabelPreset created = createWithLabNumber(TEST_PREFIX + "null_entry");
        String body = "{\"name\":\"" + TEST_PREFIX + "null_entry\",\"heightMm\":30,\"widthMm\":40,"
                + "\"barcodeType\":\"CODE_128\",\"printsPerOrder\":false,\"printsPerSample\":true,"
                + "\"defaultPerOrder\":0,\"maxPerOrder\":10,\"defaultPerSample\":1,\"maxPerSample\":5,"
                + "\"isActive\":true,\"fields\":[null]}";

        MvcResult result = mockMvc
                .perform(put(BASE_URL + "/" + created.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity()).andReturn();

        assertTrue("422 names the rule", result.getResponse().getContentAsString().contains("field.required"));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.label_preset_field WHERE preset_id = ?",
                        Integer.class, created.getId()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelPreset createWithLabNumber(String name) throws Exception {
        LabelPresetForm form = buildValidForm(name);
        form.setFields(List.of(fieldEntry("LAB_NUMBER", true, 1)));
        MvcResult result = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();
        return JSON.readValue(result.getResponse().getContentAsString(), LabelPreset.class);
    }

    private LabelPresetForm.FieldEntry fieldEntry(String key, boolean required, int order) {
        LabelPresetForm.FieldEntry entry = new LabelPresetForm.FieldEntry();
        entry.setFieldKey(key);
        entry.setIsRequired(required);
        entry.setDisplayOrder(order);
        return entry;
    }

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

    // ── OGC-1218: content field keys come from the catalogue ────────────────

    @Test
    public void post_unknownFieldKey_returns422NamingTheRule() throws Exception {
        LabelPresetForm form = buildValidForm(null);
        LabelPresetForm.FieldEntry known = new LabelPresetForm.FieldEntry();
        known.setFieldKey("PATIENT_NAME");
        known.setIsRequired(false);
        known.setDisplayOrder(2);
        LabelPresetForm.FieldEntry unknown = new LabelPresetForm.FieldEntry();
        unknown.setFieldKey("FAVOURITE_COLOUR");
        unknown.setIsRequired(false);
        unknown.setDisplayOrder(3);
        form.setFields(List.of(known, unknown));

        MvcResult result = mockMvc
                .perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isUnprocessableEntity()).andReturn();
        assertTrue(result.getResponse().getContentAsString().contains("{error.labelpreset.field.key.unknown}"));
    }

    @Test
    public void post_cataloguedFieldKeys_areStoredWithLabNumberFirst() throws Exception {
        LabelPresetForm form = buildValidForm(null);
        form.setName(TEST_PREFIX + "catalogued_fields");
        LabelPresetForm.FieldEntry tests = new LabelPresetForm.FieldEntry();
        tests.setFieldKey("TESTS");
        tests.setIsRequired(true);
        tests.setDisplayOrder(4);
        LabelPresetForm.FieldEntry specimenType = new LabelPresetForm.FieldEntry();
        specimenType.setFieldKey("SPECIMEN_TYPE");
        specimenType.setIsRequired(false);
        specimenType.setDisplayOrder(2);
        form.setFields(List.of(tests, specimenType));

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(form)))
                .andExpect(status().isCreated());

        List<String> stored = jdbc.queryForList(
                "SELECT field_key FROM clinlims.label_preset_field WHERE preset_id = "
                        + "(SELECT id FROM clinlims.label_preset WHERE name = ?) ORDER BY display_order",
                String.class, TEST_PREFIX + "catalogued_fields");
        assertEquals(List.of("LAB_NUMBER", "SPECIMEN_TYPE", "TESTS"), stored);
    }
}
