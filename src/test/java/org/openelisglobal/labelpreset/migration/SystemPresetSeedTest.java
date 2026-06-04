package org.openelisglobal.labelpreset.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Data-integrity test for the system-preset seed (changeset
 * {@code 030-seed-system-presets.xml}) against the v1 barcode configuration
 * carried in {@code clinlims.site_information} (FRS §2.7, data-model.md §2.5).
 *
 * <h2>Why this test re-runs the seed instead of reading the init-seeded
 * rows</h2>
 *
 * The seed changeset runs ONCE at Liquibase context initialization, long before
 * any JUnit fixture can load. In the test database the only
 * {@code site_information} rows present at that point come from
 * {@code postgre-db-init/OpenELIS-Global.sql} + {@code siteInfo.sql}, NEITHER
 * of which seeds a single {@code barcode.*} key. So the init run of {@code 030}
 * produced its 5 presets entirely from the canonical fallback constants (height
 * 25 / width 76 / default 1 / max 10) and never exercised the key-reading,
 * scope-mapping, or numeric-normalization branches.
 *
 * <p>
 * To genuinely exercise (and pin) that logic, each test:
 * <ol>
 * <li>DELETEs the init-seeded system presets (cascade also clears their seeded
 * {@code label_preset_field} rows) and clears any {@code barcode.*}
 * {@code site_information} keys — making the fixture the sole source of truth;
 * <li>loads {@code fixtures/v1-barcode-config.sql} with values that
 * INTENTIONALLY differ from the fallback constants;
 * <li>re-runs the <strong>real</strong> {@code <sql>} block extracted from
 * {@code 030-seed-system-presets.xml} at runtime (NOT a hardcoded copy — see
 * {@link #runRealSeedChangesetSql()}).
 * </ol>
 *
 * <h2>Inversion-worthiness</h2>
 *
 * Because the SQL under test is read from the actual changeset artifact, any
 * mutation to that artifact's COALESCE fallbacks, the
 * {@code (legacy_key = 'order')} scope expressions, or the
 * {@code value ~ '^[0-9]+$'} numeric guard makes these assertions go red. The
 * fixture values differ from the fallbacks, so a broken lookup that silently
 * returned the fallback would also be caught.
 *
 * <h2>State management</h2>
 *
 * The base class runs {@code @Transactional(NOT_SUPPORTED)} — there is no
 * rollback and the Testcontainer is shared statically across the suite. Every
 * test is therefore made fully self-contained in {@link #seedFromFixture()} (it
 * never assumes the pristine init rows survive) and
 * {@link #restoreCanonicalSeed()} leaves the canonical system presets in place
 * afterwards as hygiene for any later reader.
 */
public class SystemPresetSeedTest extends BaseWebContextSensitiveTest {

    private static final String SEED_CHANGESET = "liquibase/3.3.x.x/030-seed-system-presets.xml";
    /**
     * Field-seed changeset (031). The Liquibase init run executes 030 THEN 031, so
     * the pristine baseline has every system preset carrying its {@code LAB_NUMBER}
     * field row. {@link #clearSystemPresets()} cascade-deletes those field rows, so
     * {@code @After} must re-run 031 (not just 030) to truly restore the baseline
     * for sibling tests on this feature branch that read
     * {@code label_preset_field}.
     */
    static final String FIELD_SEED_CHANGESET = "liquibase/3.3.x.x/031-seed-system-preset-fields.xml";
    private static final String FIXTURE = "fixtures/v1-barcode-config.sql";

    /**
     * Canonical fallback constants from data-model.md §2.5 — used for the inversion
     * guard.
     */
    private static final int FALLBACK_HEIGHT = 25;
    private static final int FALLBACK_WIDTH = 76;

    @Autowired
    private DataSource dataSource;

    @Before
    public void seedFromFixture() throws Exception {
        clearSystemPresets();
        clearBarcodeSiteInformation();
        loadFixture(FIXTURE);
        runRealSeedChangesetSql();
    }

    @After
    public void restoreCanonicalSeed() throws Exception {
        // Leave the DB the way the rest of the suite expects: canonical system presets
        // (built from fallbacks, since no barcode.* keys remain), their LAB_NUMBER
        // field
        // rows (re-seeded via 031, since clearSystemPresets cascade-deleted them), and
        // no
        // fixture keys. The Testcontainer is shared + committed (NOT_SUPPORTED), so
        // this
        // restore protects sibling tests that read label_preset / label_preset_field.
        clearSystemPresets();
        clearBarcodeSiteInformation();
        runRealSeedChangesetSql();
        executeSeedSql(dataSource, FIELD_SEED_CHANGESET);
    }

    @Test
    public void seedReadsFixtureValuesForAllFiveSystemPresets() throws Exception {
        // Guard: the seed actually produced exactly the 5 system presets.
        assertEquals("seed should produce exactly 5 system presets", 5, countSystemPresets());

        // Order Label: per-order scope; quantities live in the per-order columns.
        assertPreset("Order Label", /* height */ 30, /* width */ 90, /* printsPerOrder */ true,
                /* printsPerSample */ false, /* defaultPerOrder */ 2, /* maxPerOrder */ 8, /* defaultPerSample */ 0,
                /* maxPerSample */ 10);

        // Specimen / Block / Slide / Freezer: per-sample scope; quantities live in the
        // per-sample columns.
        assertPreset("Specimen Label", 40, 80, false, true, 0, 10, 3, 7);
        assertPreset("Block Label", 35, 70, false, true, 0, 10, 4, 6);
        assertPreset("Slide Label", 45, 85, false, true, 0, 10, 5, 9);
        assertPreset("Freezer Label", 50, 60, false, true, 0, 10, 2, 5);

        // Inversion guard: prove the seed honored the fixture rather than falling back.
        // If the seed
        // had ignored the barcode.* keys (broken lookup), every height/width would
        // equal the fallback.
        assertFalse(
                "seed must read fixture dimensions, not fallbacks — "
                        + "Order Label height should not equal the canonical fallback " + FALLBACK_HEIGHT,
                isHeightEqual("Order Label", FALLBACK_HEIGHT));
        assertFalse(
                "seed must read fixture dimensions, not fallbacks — "
                        + "Specimen Label width should not equal the canonical fallback " + FALLBACK_WIDTH,
                isWidthEqual("Specimen Label", FALLBACK_WIDTH));
    }

    @Test
    public void seedMarksEverySystemPresetActiveCode128AndSystem() throws Exception {
        // FRS §2.7: every seeded preset is is_system=true, is_active=true,
        // barcode_type=CODE_128.
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM clinlims.label_preset "
                        + "WHERE is_system = true AND is_active = true AND barcode_type = 'CODE_128'");
                ResultSet rs = stmt.executeQuery()) {
            rs.next();
            assertEquals("all 5 system presets must be active CODE_128 system presets", 5, rs.getInt(1));
        }
    }

    // ----------------------------------------------------------------------------------------------
    // Assertions
    // ----------------------------------------------------------------------------------------------

    private void assertPreset(String name, int height, int width, boolean printsPerOrder, boolean printsPerSample,
            int defaultPerOrder, int maxPerOrder, int defaultPerSample, int maxPerSample) throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT height_mm, width_mm, prints_per_order, "
                        + "prints_per_sample, default_per_order, max_per_order, default_per_sample, max_per_sample, "
                        + "is_system FROM clinlims.label_preset WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue("system preset '" + name + "' must exist after seed", rs.next());
                assertEquals(name + ".height_mm", height, rs.getInt("height_mm"));
                assertEquals(name + ".width_mm", width, rs.getInt("width_mm"));
                assertEquals(name + ".prints_per_order", printsPerOrder, rs.getBoolean("prints_per_order"));
                assertEquals(name + ".prints_per_sample", printsPerSample, rs.getBoolean("prints_per_sample"));
                assertEquals(name + ".default_per_order", defaultPerOrder, rs.getInt("default_per_order"));
                assertEquals(name + ".max_per_order", maxPerOrder, rs.getInt("max_per_order"));
                assertEquals(name + ".default_per_sample", defaultPerSample, rs.getInt("default_per_sample"));
                assertEquals(name + ".max_per_sample", maxPerSample, rs.getInt("max_per_sample"));
                assertTrue(name + ".is_system", rs.getBoolean("is_system"));
                assertFalse("system preset '" + name + "' must be unique", rs.next());
            }
        }
    }

    private boolean isHeightEqual(String name, int height) throws Exception {
        return matchesInt("SELECT height_mm FROM clinlims.label_preset WHERE name = ?", name, height);
    }

    private boolean isWidthEqual(String name, int width) throws Exception {
        return matchesInt("SELECT width_mm FROM clinlims.label_preset WHERE name = ?", name, width);
    }

    private boolean matchesInt(String sql, String name, int expected) throws Exception {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == expected;
            }
        }
    }

    private int countSystemPresets() throws Exception {
        return countSystemPresets(dataSource);
    }

    // ----------------------------------------------------------------------------------------------
    // Seed orchestration — shared with SystemPresetSeedMalformedInputTest semantics
    // ----------------------------------------------------------------------------------------------

    /**
     * Re-runs the seed by reading the <strong>real</strong> changeset file at
     * runtime: parses {@code 030-seed-system-presets.xml}, extracts the text of its
     * single {@code <sql>} element (DOM text content auto-unescapes
     * {@code &lt;&gt;} back to {@code <>}), and executes it via JDBC. This keeps
     * the test inversion-worthy: the SQL exercised IS the production artifact, so a
     * mutation in the changeset is observable here. Liquibase's changelog tracking
     * is intentionally bypassed (the init run already recorded the changeset, so a
     * Liquibase re-run would no-op).
     */
    private void runRealSeedChangesetSql() throws Exception {
        executeSeedSql(dataSource, SEED_CHANGESET);
    }

    private void clearSystemPresets() throws Exception {
        clearSystemPresets(dataSource);
    }

    private void clearBarcodeSiteInformation() throws Exception {
        clearBarcodeSiteInformation(dataSource);
    }

    private void loadFixture(String fixture) throws Exception {
        loadFixture(dataSource, fixture);
    }

    // ----------------------------------------------------------------------------------------------
    // Static helpers (package-visible for reuse by
    // SystemPresetSeedMalformedInputTest)
    // ----------------------------------------------------------------------------------------------

    static void executeSeedSql(DataSource dataSource, String changesetPath) throws Exception {
        String seedSql = extractSeedSql(changesetPath);
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(seedSql);
        }
    }

    /**
     * Parses the changeset XML and returns the text of its (single) {@code <sql>}
     * child. Reads the production artifact so the executed SQL is never a copy.
     */
    static String extractSeedSql(String changesetPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        ClassPathResource resource = new ClassPathResource(changesetPath);
        try (InputStream in = resource.getInputStream()) {
            NodeList sqlNodes = builder.parse(in).getElementsByTagNameNS("*", "sql");
            // The seed changeset contains exactly one top-level <sql> insert block;
            // <rollback>
            // wraps its own <sql>, so filter to the element whose parent is a <changeSet>.
            for (int i = 0; i < sqlNodes.getLength(); i++) {
                Element sql = (Element) sqlNodes.item(i);
                String parentLocal = sql.getParentNode().getLocalName();
                if (parentLocal != null && parentLocal.equals("changeSet")) {
                    String text = sql.getTextContent();
                    if (text != null && text.toUpperCase().contains("INSERT INTO")) {
                        return text;
                    }
                }
            }
            throw new IllegalStateException("No top-level <sql> INSERT block found in changeset " + changesetPath);
        }
    }

    static void clearSystemPresets(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // FK label_preset_field -> label_preset is ON DELETE CASCADE, so seeded field
            // rows go too.
            stmt.execute("DELETE FROM clinlims.label_preset WHERE is_system = true");
        }
    }

    static void clearBarcodeSiteInformation(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM clinlims.site_information WHERE name LIKE 'barcode.order.%' "
                    + "OR name LIKE 'barcode.specimen.%' OR name LIKE 'barcode.block.%' "
                    + "OR name LIKE 'barcode.slide.%' OR name LIKE 'barcode.freezer.%'");
        }
    }

    static void loadFixture(DataSource dataSource, String fixture) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(fixture));
        }
    }

    static int countSystemPresets(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn
                        .prepareStatement("SELECT COUNT(*) FROM clinlims.label_preset WHERE is_system = true");
                ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
