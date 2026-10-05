package org.openelisglobal.labelpreset.migration;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remediation test for changeset
 * {@code 3.6.x.x/008-reseed-system-presets-from-legacy-keys.xml} (OGC-1219).
 *
 * <p>
 * Sites upgraded with the first release of changeset 030 got their five system
 * presets from the fallbacks (25 x 76 mm, default 1, max 10) while the real
 * configuration stayed in {@code site_information}.
 * {@link #seedTheWayTheBrokenReleaseDid()} reproduces that state exactly: the
 * legacy keys are stashed so the seed reads nothing, then the site's real keys
 * are put in place afterwards. Each test then runs the real {@code UPDATE}
 * block of 008 (extracted from the changeset artifact) and reads the rows back.
 *
 * <p>
 * Self-contained per {@link SystemPresetSeedTest}'s contract: the base class
 * commits (NOT_SUPPORTED) and the container is shared, so {@code @After}
 * restores the pristine init state, legacy keys included.
 */
public class SystemPresetReseedTest extends BaseWebContextSensitiveTest {

    static final String RESEED_CHANGESET = "liquibase/3.6.x.x/008-reseed-system-presets-from-legacy-keys.xml";
    private static final String FIXTURE = "fixtures/v1-barcode-config.sql";

    @Autowired
    private DataSource dataSource;

    @Before
    public void seedTheWayTheBrokenReleaseDid() throws Exception {
        SystemPresetSeedTest.clearSystemPresets(dataSource);
        SystemPresetSeedTest.stashLegacyLabelKeys(dataSource);
        SystemPresetSeedTest.executeSeedSql(dataSource, SystemPresetSeedTest.SEED_CHANGESET);
        SystemPresetSeedTest.executeSeedSql(dataSource, SystemPresetSeedTest.FIELD_SEED_CHANGESET);
        SystemPresetSeedTest.executeUpdateSql(dataSource, SystemPresetSeedTest.UNIVERSAL_BACKFILL_CHANGESET);
        SystemPresetSeedTest.loadFixture(dataSource, FIXTURE);
        assertEquals("the broken release left every system preset on the fallback set", 5, countAtFallbackSet());
    }

    @After
    public void restoreCanonicalSeed() throws Exception {
        SystemPresetSeedTest.restoreCanonicalSeed(dataSource);
    }

    @Test
    public void reseedRepairsEveryPresetStillOnTheFallbackSet() throws Exception {
        runReseed();

        SystemPresetSeedTest.assertPreset(dataSource, "Order Label", 30, 90, true, false, 2, 8, 0, 10);
        SystemPresetSeedTest.assertPreset(dataSource, "Specimen Label", 40, 80, false, true, 0, 10, 3, 7);
        SystemPresetSeedTest.assertPreset(dataSource, "Block Label", 35, 70, false, true, 0, 10, 4, 6);
        SystemPresetSeedTest.assertPreset(dataSource, "Slide Label", 45, 85, false, true, 0, 10, 5, 9);
        SystemPresetSeedTest.assertPreset(dataSource, "Freezer Label", 50, 60, false, true, 0, 10, 2, 5);
        assertEquals(0, countAtFallbackSet());
    }

    @Test
    public void reseedLeavesAnAdministratorEditedPresetAlone() throws Exception {
        execute("UPDATE clinlims.label_preset SET height_mm = 30 WHERE is_system = true AND name = 'Slide Label'");

        runReseed();

        SystemPresetSeedTest.assertPreset(dataSource, "Slide Label", 30, 76, false, true, 0, 10, 1, 10);
        SystemPresetSeedTest.assertPreset(dataSource, "Order Label", 30, 90, true, false, 2, 8, 0, 10);
        SystemPresetSeedTest.assertPreset(dataSource, "Freezer Label", 50, 60, false, true, 0, 10, 2, 5);
    }

    @Test
    public void reseedKeepsTheCurrentValueForMissingOrMalformedKeys() throws Exception {
        execute("DELETE FROM clinlims.site_information WHERE name = 'heightSlideLabels'");
        execute("UPDATE clinlims.site_information SET value = 'garbage' WHERE name = 'numDefaultSlideLabels'");

        runReseed();

        SystemPresetSeedTest.assertPreset(dataSource, "Slide Label", 25, 85, false, true, 0, 10, 1, 9);
    }

    @Test
    public void reseedRaisesMaxToTheDefaultWhenLegacyMaxIsBelowIt() throws Exception {
        execute("UPDATE clinlims.site_information SET value = '1' WHERE name = 'numMaxOrderLabels'");

        runReseed();

        SystemPresetSeedTest.assertPreset(dataSource, "Order Label", 30, 90, true, false, 2, 2, 0, 10);
    }

    @Test
    public void reseedIsIdempotent() throws Exception {
        runReseed();
        List<String> first = snapshot();

        runReseed();

        assertEquals(first, snapshot());
    }

    @Test
    public void reseedWithoutLegacyKeysChangesNothing() throws Exception {
        execute("DELETE FROM clinlims.site_information WHERE name ~ '" + SystemPresetSeedTest.LEGACY_KEY_PATTERN + "'");

        runReseed();

        assertEquals(5, countAtFallbackSet());
    }

    private void runReseed() throws Exception {
        SystemPresetSeedTest.executeUpdateSql(dataSource, RESEED_CHANGESET);
    }

    private int countAtFallbackSet() throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM clinlims.label_preset"
                        + " WHERE is_system = true AND height_mm = 25 AND width_mm = 76"
                        + " AND ((prints_per_order AND default_per_order = 1 AND max_per_order = 10)"
                        + "   OR (NOT prints_per_order AND default_per_sample = 1 AND max_per_sample = 10))");
                ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<String> snapshot() throws Exception {
        List<String> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT name, height_mm, width_mm, default_per_order,"
                        + " max_per_order, default_per_sample, max_per_sample FROM clinlims.label_preset"
                        + " WHERE is_system = true ORDER BY name");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rows.add(rs.getString(1) + "|" + rs.getInt(2) + "|" + rs.getInt(3) + "|" + rs.getInt(4) + "|"
                        + rs.getInt(5) + "|" + rs.getInt(6) + "|" + rs.getInt(7));
            }
        }
        return rows;
    }

    private void execute(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
