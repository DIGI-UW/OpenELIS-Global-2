package org.openelisglobal.labelpreset.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Malformed-input test for the system-preset seed (changeset
 * {@code 030-seed-system-presets.xml}). Verifies the seed coerces a non-numeric
 * {@code site_information.barcode.*} value to {@code NULL} via its
 * {@code value ~ '^[0-9]+$'} guard and falls back to the canonical default
 * ({@code default_per_sample = 1}) WITHOUT throwing on the
 * {@code value::INTEGER} cast (FRS §2.7, data-model.md §2.5).
 *
 * <h2>Why the seed is re-run against a fixture (not read post-init)</h2>
 *
 * See {@link SystemPresetSeedTest} for the full rationale. In short: the
 * init-time run of {@code 030} saw ZERO {@code barcode.*} keys, so the
 * {@code 'garbage'} value never existed when the seed ran. Asserting
 * {@code default_per_sample == 1} against the init rows would pass vacuously
 * and would NOT detect removal of the regex guard. The malformed value must be
 * present <em>when the seed runs</em>, so this test loads
 * {@code fixtures/v1-barcode-config-malformed.sql} and re-runs the
 * <strong>real</strong> changeset {@code <sql>} (via
 * {@link SystemPresetSeedTest#executeSeedSql}).
 *
 * <h2>Inversion-worthiness</h2>
 *
 * The malformed fixture pairs {@code barcode.specimen.default = 'garbage'} with
 * <em>valid, non-fallback</em> specimen height/width/max (42 / 88 / 6). The
 * test asserts BOTH that those numeric keys were read (proving the seed did not
 * skip the whole specimen row) AND that {@code default_per_sample} fell back to
 * 1 (proving the bad value was normalized). Delete the regex guard from the
 * changeset and the {@code value::INTEGER} cast throws on {@code 'garbage'} —
 * the seed run fails and this test goes red.
 *
 * <h2>State management</h2>
 *
 * Self-contained per {@link SystemPresetSeedTest}'s contract (base class runs
 * {@code @Transactional(NOT_SUPPORTED)}: no rollback, shared static container).
 * {@code @Before} rebuilds the system presets from the malformed fixture;
 * {@code @After} restores the canonical fallback presets so later readers see a
 * clean baseline.
 */
public class SystemPresetSeedMalformedInputTest extends BaseWebContextSensitiveTest {

    private static final String SEED_CHANGESET = "liquibase/3.3.x.x/030-seed-system-presets.xml";
    private static final String MALFORMED_FIXTURE = "fixtures/v1-barcode-config-malformed.sql";

    @Autowired
    private DataSource dataSource;

    @Before
    public void seedFromMalformedFixture() throws Exception {
        SystemPresetSeedTest.clearSystemPresets(dataSource);
        SystemPresetSeedTest.clearBarcodeSiteInformation(dataSource);
        SystemPresetSeedTest.loadFixture(dataSource, MALFORMED_FIXTURE);
        // If the seed did not normalize 'garbage' -> NULL, this call would throw on
        // value::INTEGER.
        SystemPresetSeedTest.executeSeedSql(dataSource, SEED_CHANGESET);
    }

    @After
    public void restoreCanonicalSeed() throws Exception {
        // Restore the pristine baseline for sibling tests: canonical presets + their
        // LAB_NUMBER field rows (031 re-run, since clearSystemPresets cascade-deletes
        // them) + no fixture keys. Shared committed Testcontainer (NOT_SUPPORTED).
        SystemPresetSeedTest.clearSystemPresets(dataSource);
        SystemPresetSeedTest.clearBarcodeSiteInformation(dataSource);
        SystemPresetSeedTest.executeSeedSql(dataSource, SEED_CHANGESET);
        SystemPresetSeedTest.executeSeedSql(dataSource, SystemPresetSeedTest.FIELD_SEED_CHANGESET);
    }

    @Test
    public void malformedDefaultSeedsCanonicalFallbackWithoutError() throws Exception {
        // The @Before seed run completed without throwing (else this test would have
        // errored in setup),
        // and produced the full set of system presets.
        assertEquals("seed must still produce all 5 system presets despite the malformed value", 5,
                SystemPresetSeedTest.countSystemPresets(dataSource));

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT height_mm, width_mm, "
                        + "default_per_sample, max_per_sample FROM clinlims.label_preset WHERE name = 'Specimen Label'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue("Specimen Label preset must exist after seed", rs.next());

                // Proves the specimen row WAS read (valid non-fallback dimensions/max survive),
                // so the assertion below isolates malformed-handling from total-absence
                // fallback.
                assertEquals("Specimen Label.height_mm (valid numeric key must be read)", 42, rs.getInt("height_mm"));
                assertEquals("Specimen Label.width_mm (valid numeric key must be read)", 88, rs.getInt("width_mm"));
                assertEquals("Specimen Label.max_per_sample (valid numeric key must be read)", 6,
                        rs.getInt("max_per_sample"));

                // The canonical fallback: 'garbage' normalized to NULL -> COALESCE(..., 1).
                assertEquals("malformed barcode.specimen.default ('garbage') must fall back to default_per_sample = 1",
                        1, rs.getInt("default_per_sample"));
            }
        }
    }
}
