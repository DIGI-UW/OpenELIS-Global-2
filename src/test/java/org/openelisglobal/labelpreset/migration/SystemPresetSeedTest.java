package org.openelisglobal.labelpreset.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

    static final String SEED_CHANGESET = "liquibase/3.3.x.x/030-seed-system-presets.xml";
    /**
     * Field-seed changeset (031). The Liquibase init run executes 030 THEN 031, so
     * every preset initially receives its required {@code LAB_NUMBER} field.
     * {@link #clearSystemPresets()} cascade-deletes those rows, so cleanup must
     * restore 031 before the later content-field migration.
     */
    static final String FIELD_SEED_CHANGESET = "liquibase/3.3.x.x/031-seed-system-preset-fields.xml";
    /**
     * Universality backfill changeset (032). The seed SQL in 030 inserts every
     * system preset with {@code is_universal} at its column default of false; 032
     * is what marks Specimen Label universal. Re-running 030 alone therefore
     * restores the presets in a state the pristine baseline never had, and every
     * later reader of the seeded Specimen Label sees a non-universal preset.
     */
    static final String UNIVERSAL_BACKFILL_CHANGESET = "liquibase/3.3.x.x/032-label-preset-is-universal.xml";
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
        stashLegacyLabelKeys(dataSource);
        loadFixture(FIXTURE);
        runRealSeedChangesetSql();
        runRealSeedGuardSql();
    }

    @After
    public void restoreCanonicalSeed() throws Exception {
        restoreCanonicalSeed(dataSource);
    }

    @Test
    public void restoreCanonicalSeedRestoresTheDefaultOrderLabelFields() throws Exception {
        restoreCanonicalSeed(dataSource);

        List<String> fields = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT f.field_key FROM clinlims.label_preset_field f"
                        + " JOIN clinlims.label_preset p ON p.id = f.preset_id"
                        + " WHERE p.is_system = true AND p.name = 'Order Label' ORDER BY f.display_order")) {
            while (rs.next()) {
                fields.add(rs.getString(1));
            }
        }
        assertEquals(List.of("LAB_NUMBER", "PATIENT_NAME", "PATIENT_DOB", "PATIENT_ID", "SITE_ID"), fields);
    }

    /**
     * AC-22a (FRS v2.6): on a database carrying v1 barcode configuration no seeded
     * preset may end up with the fallback dimensions, because the keys were read.
     */
    @Test
    public void noSeededPresetCarriesTheFallbackDimensionsWhenV1ConfigurationExists() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT name FROM clinlims.label_preset"
                        + " WHERE is_system = true AND height_mm = ? AND width_mm = ?")) {
            stmt.setInt(1, FALLBACK_HEIGHT);
            stmt.setInt(2, FALLBACK_WIDTH);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> onFallback = new ArrayList<>();
                while (rs.next()) {
                    onFallback.add(rs.getString(1));
                }
                assertTrue("system presets seeded from the fallback dimensions although the site configured its own: "
                        + onFallback, onFallback.isEmpty());
            }
        }
    }

    /**
     * MG-6 (FRS v2.6): the seed fails loudly when v1 configuration exists and a
     * preset did not take it. Reproduces OGC-1219 by hand (a preset put back on the
     * fallbacks while its keys are present) and expects the guard to raise.
     */
    @Test
    public void seedGuardRaisesWhenAPresetIgnoredItsLegacyKeys() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE clinlims.label_preset SET height_mm = " + FALLBACK_HEIGHT + ", width_mm = "
                    + FALLBACK_WIDTH + " WHERE name = 'Slide Label'");
        }
        try {
            runRealSeedGuardSql();
            fail("the MG-6 guard must raise when v1 configuration exists and a preset was seeded from fallbacks");
        } catch (java.sql.SQLException e) {
            assertTrue("guard names the ticket: " + e.getMessage(), e.getMessage().contains("OGC-1219"));
            assertTrue("guard names the ignored key: " + e.getMessage(),
                    e.getMessage().contains("heightSlideLabels=45"));
        }
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
        assertPreset("Block Label", 36, 70, false, true, 0, 10, 4, 6);
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

    /**
     * The restore this class runs after every test is what sibling classes read for
     * the rest of the suite, so it must reproduce the whole init run and not just
     * the insert half. Re-running 030 alone leaves Specimen Label at the column
     * default of false, and the aggregation tests that assert the universal column
     * then fail depending only on surefire ordering.
     */
    @Test
    public void restoreCanonicalSeed_leavesSpecimenLabelUniversal() throws Exception {
        clearSystemPresets();

        restoreCanonicalSeed(dataSource);

        assertEquals("restore must re-apply the universality backfill, not only the preset insert", Boolean.TRUE,
                isSpecimenLabelUniversal());
        assertEquals("restore must leave exactly one seeded Specimen Label", 1, countSeededSpecimenLabels());
    }

    private Boolean isSpecimenLabelUniversal() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT is_universal FROM clinlims.label_preset"
                        + " WHERE is_system = true AND name = 'Specimen Label'");
                ResultSet rs = stmt.executeQuery()) {
            assertTrue("the seeded Specimen Label preset must exist after a restore", rs.next());
            return rs.getBoolean(1);
        }
    }

    private int countSeededSpecimenLabels() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM clinlims.label_preset"
                        + " WHERE is_system = true AND name = 'Specimen Label'");
                ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
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
        assertPreset(dataSource, name, height, width, printsPerOrder, printsPerSample, defaultPerOrder, maxPerOrder,
                defaultPerSample, maxPerSample);
    }

    static void assertPreset(DataSource dataSource, String name, int height, int width, boolean printsPerOrder,
            boolean printsPerSample, int defaultPerOrder, int maxPerOrder, int defaultPerSample, int maxPerSample)
            throws Exception {
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

    private void runRealSeedGuardSql() throws Exception {
        executeGuardSql(dataSource, SEED_CHANGESET);
    }

    private void clearSystemPresets() throws Exception {
        clearSystemPresets(dataSource);
    }

    private void loadFixture(String fixture) throws Exception {
        loadFixture(dataSource, fixture);
    }

    // ----------------------------------------------------------------------------------------------
    // Static helpers (package-visible for reuse by
    // SystemPresetSeedMalformedInputTest)
    // ----------------------------------------------------------------------------------------------

    static void executeSeedSql(DataSource dataSource, String changesetPath) throws Exception {
        executeChangesetSql(dataSource, changesetPath, "INSERT INTO");
    }

    /**
     * Runs the MG-6 guard block of the seed changeset, the {@code DO} block that
     * raises when v1 configuration exists and a seeded preset did not take it.
     */
    static void executeGuardSql(DataSource dataSource, String changesetPath) throws Exception {
        executeChangesetSql(dataSource, changesetPath, "RAISE EXCEPTION");
    }

    /**
     * Runs the {@code UPDATE} block of a changeset, such as 032's backfill or 008's
     * re-seed.
     */
    static void executeUpdateSql(DataSource dataSource, String changesetPath) throws Exception {
        executeChangesetSql(dataSource, changesetPath, "UPDATE CLINLIMS.LABEL_PRESET");
    }

    private static void executeChangesetSql(DataSource dataSource, String changesetPath, String statementKeyword)
            throws Exception {
        String sql = extractChangesetSql(changesetPath, statementKeyword);
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    static String extractSeedSql(String changesetPath) throws Exception {
        return extractChangesetSql(changesetPath, "INSERT INTO");
    }

    /**
     * Parses the changeset XML and returns the text of the top-level {@code <sql>}
     * block containing {@code statementKeyword}. Reads the production artifact so
     * the executed SQL is never a copy. Only {@code <sql>} elements whose parent is
     * a {@code <changeSet>} are considered, so a {@code <rollback>} carrying the
     * inverse statement never matches.
     */
    static String extractChangesetSql(String changesetPath, String statementKeyword) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        ClassPathResource resource = new ClassPathResource(changesetPath);
        try (InputStream in = resource.getInputStream()) {
            NodeList sqlNodes = builder.parse(in).getElementsByTagNameNS("*", "sql");
            // <rollback> wraps its own <sql> and carries the inverse statement, so
            // filter to the elements whose parent is a <changeSet> before matching on
            // the keyword.
            for (int i = 0; i < sqlNodes.getLength(); i++) {
                Element sql = (Element) sqlNodes.item(i);
                String parentLocal = sql.getParentNode().getLocalName();
                if (parentLocal != null && parentLocal.equals("changeSet")) {
                    String text = sql.getTextContent();
                    if (text != null && text.toUpperCase().contains(statementKeyword)) {
                        return text;
                    }
                }
            }
            throw new IllegalStateException(
                    "No top-level <sql> " + statementKeyword + " block found in changeset " + changesetPath);
        }
    }

    /**
     * Rebuilds the complete boot state after a test cleared the system presets,
     * including the default content fields added by the later 009 changeset.
     *
     * <p>
     * The Testcontainer is shared and committed ({@code NOT_SUPPORTED}), so an
     * incomplete restore is not local damage — it is the state every later test
     * class in the run reads. Add the matching call here whenever a new changeset
     * conditions the seeded presets.
     */
    static void restoreCanonicalSeed(DataSource dataSource) throws Exception {
        restoreLabNumberOnlySeed(dataSource);
        executeSeedSql(dataSource, SystemPresetFieldSeedTest.FIELD_DEFAULTS_CHANGESET);
    }

    /** The earlier state that the content-field migration upgrades. */
    static void restoreLabNumberOnlySeed(DataSource dataSource) throws Exception {
        restoreLegacyLabelKeys(dataSource);
        clearSystemPresets(dataSource);
        executeSeedSql(dataSource, SEED_CHANGESET);
        executeSeedSql(dataSource, FIELD_SEED_CHANGESET);
        executeUpdateSql(dataSource, UNIVERSAL_BACKFILL_CHANGESET);
    }

    static void clearSystemPresets(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // FK label_preset_field -> label_preset is ON DELETE CASCADE, so seeded field
            // rows go too.
            stmt.execute("DELETE FROM clinlims.label_preset WHERE is_system = true");
        }
    }

    /**
     * The real per-type label keys the seed reads (ConfigurationProperties.Property
     * names).
     */
    static final String LEGACY_KEY_PATTERN = "^(height|width|numDefault|numMax)(Order|Specimen|Block|Slide|Freezer)Labels$";

    private static final String LEGACY_KEY_BACKUP_TABLE = "label_key_backup_seed_test";

    /**
     * Moves the database's own legacy label keys into a backup table so a fixture
     * can be the sole source of truth. The 2.5.x.x changelog seeds
     * {@code numDefaultOrderLabels} and {@code numDefaultSpecimenLabels} on every
     * database, so these rows exist in the shared test container and must come back
     * afterwards ({@link #restoreLegacyLabelKeys}). An existing backup (a previous
     * test that died before restoring) is kept, never overwritten.
     */
    static void stashLegacyLabelKeys(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            if (!backupTableExists(conn)) {
                stmt.execute("CREATE TABLE clinlims." + LEGACY_KEY_BACKUP_TABLE
                        + " AS SELECT * FROM clinlims.site_information WHERE name ~ '" + LEGACY_KEY_PATTERN + "'");
            }
            stmt.execute("DELETE FROM clinlims.site_information WHERE name ~ '" + LEGACY_KEY_PATTERN + "'");
        }
    }

    /** Puts the stashed legacy label keys back and drops the fixture's rows. */
    static void restoreLegacyLabelKeys(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            if (!backupTableExists(conn)) {
                return;
            }
            stmt.execute("DELETE FROM clinlims.site_information WHERE name ~ '" + LEGACY_KEY_PATTERN + "'");
            stmt.execute("INSERT INTO clinlims.site_information SELECT * FROM clinlims." + LEGACY_KEY_BACKUP_TABLE);
            stmt.execute("DROP TABLE clinlims." + LEGACY_KEY_BACKUP_TABLE);
        }
    }

    private static boolean backupTableExists(Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
            stmt.setString(1, "clinlims." + LEGACY_KEY_BACKUP_TABLE);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
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
