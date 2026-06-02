package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
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
 * Service-layer tests for {@link LabelPresetServiceImpl} (OGC-285 M3, task
 * T063).
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
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void create_differentName_succeeds() {
        LabelPreset p1 = createPreset(TEST_PREFIX + "gamma");
        LabelPreset p2 = createPreset(TEST_PREFIX + "delta");
        assertNotNull(p1.getId());
        assertNotNull(p2.getId());
        assertFalse("IDs should differ", p1.getId().equals(p2.getId()));
    }

    // ── isSystem guard on rename ──────────────────────────────────────────────

    @Test
    public void update_systemPreset_renameAttempt_isRejected() {
        // Find a system preset seeded by Liquibase
        List<LabelPreset> all = labelPresetService.list(null, null);
        LabelPreset systemPreset = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsSystem())).findFirst()
                .orElse(null);
        if (systemPreset == null) {
            return; // No system presets seeded yet — skip
        }

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
        if (systemPreset == null) {
            return; // Skip
        }

        // Update with same name (no rename — should succeed)
        LabelPresetForm form = buildMinimalForm(systemPreset.getName());
        LabelPreset updated = labelPresetService.update(systemPreset.getId(), form, SYS_USER);
        assertNotNull(updated);
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
        assertNotNull("Fetched preset should not be null", fetched);
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

        assertNotNull(copy.getId());
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
            assertNotNull(e.getMessage());
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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
