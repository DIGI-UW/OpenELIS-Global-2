package org.openelisglobal.labelpreset.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for {@link LabelPresetDAO} (OGC-285 M2, task T027).
 *
 * <p>
 * Real DAO bean + real PostgreSQL via {@link BaseWebContextSensitiveTest} — NO
 * Mockito of any kind (Constitution V.6 anti-mocking discipline). Every
 * assertion is on observable persisted/read state, never on a mock return.
 *
 * <p>
 * Inversion-worthiness: the {@code label_preset} entity, DAO, and schema all
 * landed before this test, so we never observed a natural RED phase. Each test
 * is therefore written so that a plausible mutation of the implementation would
 * turn it red:
 *
 * <ul>
 * <li>insert+get persists <em>distinct non-default</em> values on every mapped
 * column, so a column-name swap in the entity mapping (e.g. {@code maxPerOrder}
 * &harr; {@code maxPerSample}) is caught instead of passing silently on shared
 * defaults.</li>
 * <li>a raw JDBC read of {@code barcode_type} asserts the literal {@code "QR"}
 * — the inversion check for {@code @Enumerated(STRING)}; flipping to
 * {@code ORDINAL} would persist {@code "1"}.</li>
 * <li>{@link LabelPresetDAO#listActive()} is checked for presence of an active
 * row, absence of an inactive row, AND that every returned row is active — so a
 * filter that drops the {@code isActive == true} predicate is caught.</li>
 * <li>{@link LabelPresetDAO#listByBarcodeType(BarcodeType)} uses
 * {@code QR}/{@code DATAMATRIX} (never {@code CODE_128}, which the 5 system
 * seeds all use) and asserts every returned row matches the requested
 * symbology.</li>
 * </ul>
 *
 * <p>
 * Scope note: {@code save}/{@code deactivate} (T042) and {@code is_system}
 * hard-delete protection (T064) are not yet implemented on the DAO — they are
 * M3 service-layer concerns — so they are intentionally not covered here.
 *
 * <p>
 * Lifecycle mirrors {@code SiteBrandingDAOTest} / {@code StorageDeviceDAOTest}:
 * {@code super.setUp()} → {@code new JdbcTemplate(dataSource)} →
 * {@code cleanTestData()}. Cleanup is by name prefix (NOT wholesale) because
 * {@code label_preset} has FK dependents and 5 seeded system rows that must
 * survive. The {@code name} column is UNIQUE, so a leftover row from a crashed
 * run would break the insert — hence cleanup runs in both {@code @Before} and
 * {@code @After}.
 */
public class LabelPresetDAOImplTest extends BaseWebContextSensitiveTest {

    /**
     * Distinguishes this test's rows from the 5 Liquibase-seeded system presets.
     */
    private static final String NAME_PREFIX = "T027_";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    private void cleanTestData() {
        try {
            // By-name only: must not touch the 5 seeded system presets or any FK
            // dependents. name is UNIQUE, so leftover rows from a crashed run
            // would break the insert.
            jdbcTemplate.execute("DELETE FROM label_preset WHERE name LIKE '" + NAME_PREFIX + "%'");
        } catch (Exception e) {
            // Ignore cleanup errors (table may be empty / first run)
        }
    }

    /**
     * Build a preset with distinct, non-default, CHECK-constraint-respecting
     * values.
     */
    private LabelPreset newPreset(String name, BarcodeType barcodeType, boolean active) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        // CHECK: height/width in [5,200]
        preset.setHeightMm(30);
        preset.setWidthMm(50);
        preset.setBarcodeType(barcodeType);
        // CHECK: at least one of prints_per_order / prints_per_sample is true.
        // Use DISTINCT booleans (false/true) so a swap of the two @Column
        // mappings (prints_per_order <-> prints_per_sample) is caught instead of
        // passing silently on a matched pair.
        preset.setPrintsPerOrder(false);
        preset.setPrintsPerSample(true);
        // CHECK: max >= default; pick distinct values per column so a mapping
        // swap (e.g. order<->sample) is caught
        preset.setDefaultPerOrder(2);
        preset.setMaxPerOrder(5);
        preset.setDefaultPerSample(3);
        preset.setMaxPerSample(7);
        preset.setIsSystem(false);
        preset.setIsActive(active);
        return preset;
    }

    /**
     * insert + get round-trip: every mapped column persists and reads back with its
     * distinct value. A column-name swap in the entity mapping turns this red.
     */
    @Test
    public void testInsertAndGet_PersistsAllColumns() {
        LabelPreset preset = newPreset(NAME_PREFIX + "RoundTrip", BarcodeType.QR, true);

        Integer id = labelPresetDAO.insert(preset);

        assertNotNull("insert should return a generated id", id);

        LabelPreset retrieved = labelPresetDAO.get(id)
                .orElseThrow(() -> new AssertionError("Inserted preset should be retrievable by id"));
        assertEquals("name should round-trip", NAME_PREFIX + "RoundTrip", retrieved.getName());
        assertEquals("heightMm should round-trip", Integer.valueOf(30), retrieved.getHeightMm());
        assertEquals("widthMm should round-trip", Integer.valueOf(50), retrieved.getWidthMm());
        assertEquals("barcodeType should round-trip", BarcodeType.QR, retrieved.getBarcodeType());
        assertEquals("printsPerOrder should round-trip", Boolean.FALSE, retrieved.getPrintsPerOrder());
        assertEquals("printsPerSample should round-trip", Boolean.TRUE, retrieved.getPrintsPerSample());
        assertEquals("defaultPerOrder should round-trip", Integer.valueOf(2), retrieved.getDefaultPerOrder());
        assertEquals("maxPerOrder should round-trip", Integer.valueOf(5), retrieved.getMaxPerOrder());
        assertEquals("defaultPerSample should round-trip", Integer.valueOf(3), retrieved.getDefaultPerSample());
        assertEquals("maxPerSample should round-trip", Integer.valueOf(7), retrieved.getMaxPerSample());
        assertEquals("isSystem should round-trip", Boolean.FALSE, retrieved.getIsSystem());
        assertEquals("isActive should round-trip", Boolean.TRUE, retrieved.getIsActive());
    }

    /**
     * {@code @Enumerated(EnumType.STRING)} inversion check: the persisted column
     * holds the enum NAME, not its ordinal. Read raw via JDBC so the assertion is
     * independent of the entity mapping under test.
     */
    @Test
    public void testInsert_PersistsBarcodeTypeAsString() {
        LabelPreset preset = newPreset(NAME_PREFIX + "EnumString", BarcodeType.QR, true);

        Integer id = labelPresetDAO.insert(preset);

        String rawBarcodeType = jdbcTemplate.queryForObject("SELECT barcode_type FROM label_preset WHERE id = ?",
                String.class, id);
        assertEquals("barcode_type must be persisted as the enum name (STRING), not its ordinal", "QR", rawBarcodeType);
    }

    /**
     * getAll (= listAll) returns the inserted row alongside any pre-existing rows.
     * Tolerant of the 5 seeded system presets via anyMatch on a unique marker.
     */
    @Test
    public void testGetAll_ContainsInsertedPreset() {
        LabelPreset preset = newPreset(NAME_PREFIX + "ListAll", BarcodeType.QR, true);
        labelPresetDAO.insert(preset);

        List<LabelPreset> all = labelPresetDAO.getAll();

        assertTrue("getAll should contain the inserted preset",
                all.stream().anyMatch(p -> (NAME_PREFIX + "ListAll").equals(p.getName())));
    }

    /**
     * listActive returns active rows and excludes inactive ones. The
     * all-elements-active assertion catches a mutation that drops the
     * {@code isActive == true} filter.
     */
    @Test
    public void testListActive_ReturnsOnlyActivePresets() {
        labelPresetDAO.insert(newPreset(NAME_PREFIX + "ActiveYes", BarcodeType.QR, true));
        labelPresetDAO.insert(newPreset(NAME_PREFIX + "ActiveNo", BarcodeType.QR, false));

        List<LabelPreset> active = labelPresetDAO.listActive();

        assertTrue("listActive should include the active preset",
                active.stream().anyMatch(p -> (NAME_PREFIX + "ActiveYes").equals(p.getName())));
        assertFalse("listActive must exclude the inactive preset",
                active.stream().anyMatch(p -> (NAME_PREFIX + "ActiveNo").equals(p.getName())));
        assertTrue("every preset returned by listActive must be active",
                active.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsActive())));
    }

    /**
     * listByBarcodeType filters by symbology. Uses QR/DATAMATRIX (never CODE_128,
     * which every system seed uses) so the result set is driven purely by this
     * test's rows. The all-elements-match assertion catches a broken filter.
     */
    @Test
    public void testListByBarcodeType_FiltersBySymbology() {
        labelPresetDAO.insert(newPreset(NAME_PREFIX + "Qr", BarcodeType.QR, true));
        labelPresetDAO.insert(newPreset(NAME_PREFIX + "DataMatrix", BarcodeType.DATAMATRIX, true));

        List<LabelPreset> qrPresets = labelPresetDAO.listByBarcodeType(BarcodeType.QR);

        assertTrue("listByBarcodeType(QR) should include the QR preset",
                qrPresets.stream().anyMatch(p -> (NAME_PREFIX + "Qr").equals(p.getName())));
        assertFalse("listByBarcodeType(QR) must exclude the DATAMATRIX preset",
                qrPresets.stream().anyMatch(p -> (NAME_PREFIX + "DataMatrix").equals(p.getName())));
        assertTrue("every preset returned by listByBarcodeType(QR) must be QR",
                qrPresets.stream().allMatch(p -> BarcodeType.QR.equals(p.getBarcodeType())));
    }
}
