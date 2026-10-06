package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service-layer tests for {@link LabelPresetServiceImpl} (OGC-285 M3).
 *
 * <p>
 * Real service + real DAO + real PostgreSQL (no @MockBean of code-under-test).
 * Covers: normalizeName collision detection (case, whitespace variants), system
 * preset protection (rename guard, deactivate guard), and CRUD lifecycle.
 *
 * <p>
 * Inversion worthiness: each test is written so that removing the corresponding
 * guard in {@link LabelPresetServiceImpl} turns it RED.
 */
public class LabelPresetServiceImplTest extends BaseWebContextSensitiveTest {

    private static final String TEST_PREFIX = "svc_test_";
    private static final String SYS_USER = TEST_SYS_USER_ID;

    @Autowired
    private LabelPresetService labelPresetService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanTestData();
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

    // ── normalizeName ─────────────────────────────────────────────────────────

    @Test
    public void normalizeName_trimsAndLowercases() {
        assertEquals("hello world", labelPresetService.normalizeName("  Hello World  "));
    }

    @Test
    public void normalizeName_nullReturnsEmpty() {
        assertEquals("", labelPresetService.normalizeName(null));
    }

    @Test
    public void normalizeName_alreadyNormalized_unchanged() {
        assertEquals("foo bar", labelPresetService.normalizeName("foo bar"));
    }

    // ── Collision detection ───────────────────────────────────────────────────

    @Test
    public void create_sameNameDifferentCase_isRejected() {
        createPreset(TEST_PREFIX + "alpha");
        try {
            createPreset("  " + TEST_PREFIX.toUpperCase() + "ALPHA  ");
            fail("Expected IllegalArgumentException for duplicate normalized name");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention the name", e.getMessage().contains(TEST_PREFIX + "alpha"));
        }
    }

    @Test
    public void create_sameNameWithWhitespaceVariant_isRejected() {
        createPreset(TEST_PREFIX + "beta");
        try {
            // LEADING/TRAILING whitespace variant of the same name — must collide.
            // (normalizeName trims outer whitespace; internal whitespace stays
            // significant, so the space must wrap the whole name, not sit inside it.)
            createPreset("  " + TEST_PREFIX + "beta  ");
            fail("Expected IllegalArgumentException for whitespace variant collision");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention the colliding name", e.getMessage().contains(TEST_PREFIX + "beta"));
        }
    }

    @Test
    public void create_differentName_succeeds() {
        LabelPreset p1 = createPreset(TEST_PREFIX + "gamma");
        LabelPreset p2 = createPreset(TEST_PREFIX + "delta");
        assertFalse("IDs should differ", p1.getId().equals(p2.getId()));
    }

    // ── isSystem guard on rename ──────────────────────────────────────────────

    @Test
    public void update_systemPreset_renameAttempt_isRejected() {
        // Find a system preset seeded by Liquibase
        List<LabelPreset> all = labelPresetService.list(null, null);
        LabelPreset systemPreset = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst()
                .orElse(null);
        assertNotNull("System presets (5 seeded by Liquibase) must be present for the rename-guard test", systemPreset);

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "renamed_system");
        try {
            labelPresetService.update(systemPreset.getId(), form, SYS_USER);
            fail("Expected IllegalArgumentException when renaming a system preset");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention system preset renaming restriction",
                    e.getMessage().contains("System presets cannot be renamed"));
        }
    }

    @Test
    public void update_systemPreset_sameName_succeeds() {
        List<LabelPreset> all = labelPresetService.list(null, null);
        LabelPreset systemPreset = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst()
                .orElse(null);
        assertNotNull("System presets (5 seeded by Liquibase) must be present for the same-name-update test",
                systemPreset);

        // Update with same name (no rename — should succeed). Mirror the preset's
        // existing scope/dimensions/quantities so the update is NON-DESTRUCTIVE: a
        // minimal form would flip prints_per_order/prints_per_sample and corrupt the
        // shared, Liquibase-seeded preset for sibling tests in the reuse-fork JVM
        // (e.g. Order Label would stop being per-order, breaking the aggregation
        // tests). reuseForks=true + a static Testcontainer means this row is shared.
        LabelPresetForm form = buildMinimalForm(systemPreset.getName());
        form.setPrintsPerOrder(systemPreset.getPrintsPerOrder());
        form.setPrintsPerSample(systemPreset.getPrintsPerSample());
        form.setHeightMm(systemPreset.getHeightMm());
        form.setWidthMm(systemPreset.getWidthMm());
        form.setDefaultPerOrder(systemPreset.getDefaultPerOrder());
        form.setMaxPerOrder(systemPreset.getMaxPerOrder());
        form.setDefaultPerSample(systemPreset.getDefaultPerSample());
        form.setMaxPerSample(systemPreset.getMaxPerSample());
        LabelPreset updated = labelPresetService.update(systemPreset.getId(), form, SYS_USER);
        assertEquals(systemPreset.getId(), updated.getId());
    }

    // ── isSystem guard on deactivate ──────────────────────────────────────────

    @Test
    public void toggleActive_systemPreset_deactivate_isRejected() {
        List<LabelPreset> all = labelPresetService.list(null, null);
        LabelPreset systemPreset = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst()
                .orElse(null);
        if (systemPreset == null) {
            return; // Skip
        }

        try {
            labelPresetService.toggleActive(systemPreset.getId(), false, SYS_USER);
            fail("Expected IllegalStateException when deactivating a system preset");
        } catch (IllegalStateException e) {
            assertTrue("Error should mention system preset cannot be deactivated",
                    e.getMessage().contains("System presets cannot be deactivated"));
        }
    }

    // ── CRUD lifecycle ────────────────────────────────────────────────────────

    @Test
    public void createAndGet_roundtrip() {
        LabelPreset created = createPreset(TEST_PREFIX + "roundtrip");
        assertNotNull("Created preset should have an id", created.getId());

        LabelPreset fetched = labelPresetService.get(created.getId());
        assertEquals(TEST_PREFIX + "roundtrip", fetched.getName());
        assertEquals(Integer.valueOf(20), fetched.getHeightMm());
        assertEquals(Integer.valueOf(40), fetched.getWidthMm());
        assertEquals(BarcodeType.CODE_128, fetched.getBarcodeType());
        assertTrue("Should be active by default", Boolean.TRUE.equals(fetched.getIsActive()));
        assertFalse("Should not be system", Boolean.TRUE.equals(fetched.getIsSystem()));
    }

    @Test
    public void get_nonExistentId_returnsNull() {
        LabelPreset result = labelPresetService.get(Integer.MAX_VALUE);
        assertNull("Non-existent id should return null", result);
    }

    @Test
    public void toggleActive_deactivateThenReactivate() {
        LabelPreset preset = createPreset(TEST_PREFIX + "toggle_active");
        assertTrue(Boolean.TRUE.equals(preset.getIsActive()));

        // Deactivate
        LabelPreset deactivated = labelPresetService.toggleActive(preset.getId(), false, SYS_USER);
        assertFalse(Boolean.TRUE.equals(deactivated.getIsActive()));

        // Reactivate
        LabelPreset reactivated = labelPresetService.toggleActive(preset.getId(), true, SYS_USER);
        assertTrue(Boolean.TRUE.equals(reactivated.getIsActive()));
    }

    @Test
    public void duplicate_createsNonSystemActiveCopy() {
        LabelPreset original = createPreset(TEST_PREFIX + "orig_dup");
        LabelPreset copy = labelPresetService.duplicate(original.getId(), TEST_PREFIX + "copy_dup", SYS_USER);

        assertFalse("Copy id should differ from original", copy.getId().equals(original.getId()));
        assertEquals(TEST_PREFIX + "copy_dup", copy.getName());
        assertFalse("Copy should not be system", Boolean.TRUE.equals(copy.getIsSystem()));
        assertTrue("Copy should be active", Boolean.TRUE.equals(copy.getIsActive()));
    }

    @Test
    public void duplicate_namingCollision_isRejected() {
        LabelPreset original = createPreset(TEST_PREFIX + "orig_dupcolide");
        createPreset(TEST_PREFIX + "existing_copy");
        try {
            labelPresetService.duplicate(original.getId(), TEST_PREFIX + "existing_copy", SYS_USER);
            fail("Expected IllegalArgumentException for name collision in duplicate");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention the colliding name",
                    e.getMessage().contains(TEST_PREFIX + "existing_copy"));
        }
    }

    @Test
    public void list_activeOnly_filtersInactivePresets() {
        LabelPreset active = createPreset(TEST_PREFIX + "list_active");
        LabelPreset inactive = createPreset(TEST_PREFIX + "list_inactive");
        labelPresetService.toggleActive(inactive.getId(), false, SYS_USER);

        List<LabelPreset> activeList = labelPresetService.list(true, null);
        assertTrue("Active preset should appear", activeList.stream().anyMatch(p -> p.getId().equals(active.getId())));
        assertFalse("Inactive preset should not appear",
                activeList.stream().anyMatch(p -> p.getId().equals(inactive.getId())));
    }

    // ── Content fields on save (OGC-1227) ────────────────────────────────────

    @Test
    public void update_sameFieldsTwice_succeedsAndKeepsFieldRowId() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_idem", entry("LAB_NUMBER", true, 1));
        Integer fieldId = preset.getFields().get(0).getId();

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_idem");
        form.setFields(List.of(entry("LAB_NUMBER", true, 1)));
        form.setHeightMm(31);
        labelPresetService.update(preset.getId(), form, SYS_USER);
        form.setHeightMm(32);
        LabelPreset updated = labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals(Integer.valueOf(32), updated.getHeightMm());
        assertEquals(List.of("LAB_NUMBER"), storedFieldKeys(preset.getId()));
        assertEquals("The retained field keeps its row", fieldId, updated.getFields().get(0).getId());
    }

    @Test
    public void update_withoutFields_leavesStoredFieldsUntouched() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_null", entry("LAB_NUMBER", true, 1));

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_null");
        form.setFields(null);
        form.setHeightMm(30);
        LabelPreset updated = labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals(Integer.valueOf(30), updated.getHeightMm());
        assertEquals(List.of("LAB_NUMBER"), storedFieldKeys(preset.getId()));
    }

    @Test
    public void update_withEmptyFields_clearsTheSelectableFieldsButKeepsLabNumber() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_clear", entry("LAB_NUMBER", true, 1),
                entry("PATIENT_NAME", false, 2));

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_clear");
        form.setFields(List.of());
        labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals("Lab Number is on every preset (FR-008)", List.of("LAB_NUMBER"), storedFieldKeys(preset.getId()));
    }

    @Test
    public void update_swappedDisplayOrders_isApplied() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_swap", entry("LAB_NUMBER", true, 1),
                entry("PATIENT_NAME", false, 2), entry("PATIENT_ID", false, 3));

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_swap");
        form.setFields(
                List.of(entry("LAB_NUMBER", true, 1), entry("PATIENT_NAME", false, 3), entry("PATIENT_ID", false, 2)));
        labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "PATIENT_NAME"), storedFieldKeys(preset.getId()));
    }

    // ── Lab Number first, always (OGC-1218) ─────────────────────────────────

    @Test
    public void create_withoutFields_getsLabNumberRequiredAtPositionOne() {
        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_fresh");
        form.setFields(null);
        LabelPreset created = labelPresetService.create(form, SYS_USER);

        assertEquals(List.of("LAB_NUMBER"), storedFieldKeys(created.getId()));
        assertEquals(List.of("1:true"), storedFieldPositions(created.getId()));
    }

    @Test
    public void fields_labNumberIsForcedFirstRequiredAndTheRestRenumbered() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_forced", entry("PATIENT_NAME", false, 5),
                entry("LAB_NUMBER", false, 9), entry("TESTS", true, 2));

        assertEquals(List.of("LAB_NUMBER", "TESTS", "PATIENT_NAME"), storedFieldKeys(preset.getId()));
        assertEquals(List.of("1:true", "2:true", "3:false"), storedFieldPositions(preset.getId()));

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_forced");
        form.setFields(List.of(entry("PATIENT_ID", false, 1)));
        labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals("a request without Lab Number still keeps it first", List.of("LAB_NUMBER", "PATIENT_ID"),
                storedFieldKeys(preset.getId()));
        assertEquals(List.of("1:true", "2:false"), storedFieldPositions(preset.getId()));
    }

    @Test
    public void update_addsAndRemovesFields_keepingRetainedRowIds() {
        LabelPreset preset = createPresetWithFields(TEST_PREFIX + "fields_diff", entry("LAB_NUMBER", true, 1),
                entry("PATIENT_NAME", false, 2));
        Integer labNumberId = fieldId(preset, "LAB_NUMBER");

        LabelPresetForm form = buildMinimalForm(TEST_PREFIX + "fields_diff");
        form.setFields(
                List.of(entry("LAB_NUMBER", true, 1), entry("PATIENT_ID", false, 2), entry("PATIENT_DOB", false, 3)));
        LabelPreset updated = labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "PATIENT_DOB"), storedFieldKeys(preset.getId()));
        assertEquals(labNumberId, fieldId(updated, "LAB_NUMBER"));
    }

    @Test
    public void update_systemPreset_lowerCasedName_keepsStoredName() {
        LabelPreset systemPreset = labelPresetService.list(null, null).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst().orElse(null);
        assertNotNull("System presets (5 seeded by Liquibase) must be present", systemPreset);
        String storedName = systemPreset.getName();
        try {
            LabelPresetForm form = mirrorForm(systemPreset);
            form.setName(storedName.toLowerCase(Locale.ROOT));
            LabelPreset updated = labelPresetService.update(systemPreset.getId(), form, SYS_USER);

            assertEquals(storedName, updated.getName());
            assertEquals(storedName, jdbc.queryForObject("SELECT name FROM clinlims.label_preset WHERE id = ?",
                    String.class, systemPreset.getId()));
        } finally {
            jdbc.update("UPDATE clinlims.label_preset SET name = ? WHERE id = ?", storedName, systemPreset.getId());
        }
    }

    @Test
    public void update_userPreset_storesNameAsTyped() {
        LabelPreset preset = createPreset(TEST_PREFIX + "typed name");

        LabelPresetForm form = buildMinimalForm("  " + TEST_PREFIX + "Typed Name  ");
        LabelPreset updated = labelPresetService.update(preset.getId(), form, SYS_USER);

        assertEquals(TEST_PREFIX + "Typed Name", updated.getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LabelPresetForm.FieldEntry entry(String key, boolean required, int order) {
        LabelPresetForm.FieldEntry entry = new LabelPresetForm.FieldEntry();
        entry.setFieldKey(key);
        entry.setIsRequired(required);
        entry.setDisplayOrder(order);
        return entry;
    }

    private LabelPreset createPresetWithFields(String name, LabelPresetForm.FieldEntry... entries) {
        LabelPresetForm form = buildMinimalForm(name);
        form.setFields(List.of(entries));
        return labelPresetService.create(form, SYS_USER);
    }

    private List<String> storedFieldPositions(Integer presetId) {
        return jdbc.queryForList("SELECT display_order || ':' || is_required FROM clinlims.label_preset_field"
                + " WHERE preset_id = ? ORDER BY display_order", String.class, presetId);
    }

    private List<String> storedFieldKeys(Integer presetId) {
        return jdbc.queryForList(
                "SELECT field_key FROM clinlims.label_preset_field WHERE preset_id = ? ORDER BY display_order",
                String.class, presetId);
    }

    private Integer fieldId(LabelPreset preset, String key) {
        return preset.getFields().stream().filter(f -> key.equals(f.getFieldKey())).findFirst()
                .orElseThrow(() -> new AssertionError("field " + key + " missing")).getId();
    }

    private LabelPresetForm mirrorForm(LabelPreset preset) {
        LabelPresetForm form = buildMinimalForm(preset.getName());
        form.setPrintsPerOrder(preset.getPrintsPerOrder());
        form.setPrintsPerSample(preset.getPrintsPerSample());
        form.setHeightMm(preset.getHeightMm());
        form.setWidthMm(preset.getWidthMm());
        form.setBarcodeType(preset.getBarcodeType());
        form.setDefaultPerOrder(preset.getDefaultPerOrder());
        form.setMaxPerOrder(preset.getMaxPerOrder());
        form.setDefaultPerSample(preset.getDefaultPerSample());
        form.setMaxPerSample(preset.getMaxPerSample());
        form.setIsActive(preset.getIsActive());
        form.setFields(preset.getFields().stream()
                .map(f -> entry(f.getFieldKey(), Boolean.TRUE.equals(f.getIsRequired()), f.getDisplayOrder()))
                .collect(java.util.stream.Collectors.toList()));
        return form;
    }

    private LabelPreset createPreset(String name) {
        LabelPresetForm form = buildMinimalForm(name);
        return labelPresetService.create(form, SYS_USER);
    }

    private LabelPresetForm buildMinimalForm(String name) {
        LabelPresetForm form = new LabelPresetForm();
        form.setName(name);
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
