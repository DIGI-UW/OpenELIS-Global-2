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
 * Changeset {@code 3.6.x.x/009-seed-system-preset-fields-from-legacy-keys.xml}
 * (OGC-1218) fills the block 031 deferred: each system preset receives the
 * content fields the retired Barcode Configuration screen printed for it, read
 * from the legacy {@code site_information} element keys. A key that is absent
 * (fresh install) or {@code true} yields a row, an explicit {@code false} does
 * not, and a preset an administrator has already edited is left alone.
 *
 * <p>
 * Each test starts from the canonical 030/031/032 state (LAB_NUMBER only), runs
 * the real INSERT block of 009 and reads the rows back. The base class commits,
 * so {@code @After} restores the boot state (009 applied, no element keys) for
 * the tests that follow.
 */
public class SystemPresetFieldSeedTest extends BaseWebContextSensitiveTest {

    static final String FIELD_DEFAULTS_CHANGESET = "liquibase/3.6.x.x/009-seed-system-preset-fields-from-legacy-keys.xml";
    private static final String ELEMENT_KEY_PATTERN = "^(order|specimen|slide|block|freezer)Label";

    @Autowired
    private DataSource dataSource;

    @Before
    public void startFromLabNumberOnly() throws Exception {
        deleteElementKeys();
        SystemPresetSeedTest.restoreLabNumberOnlySeed(dataSource);
        assertEquals("the pre-upgrade seed carries LAB_NUMBER alone", List.of("LAB_NUMBER"),
                fieldKeys("Specimen Label"));
    }

    @After
    public void restoreBootState() throws Exception {
        deleteElementKeys();
        SystemPresetSeedTest.restoreCanonicalSeed(dataSource);
    }

    @Test
    public void aFreshInstallGetsTheShippedDefaultsPerPreset() throws Exception {
        runFieldDefaults();

        assertEquals(List.of("LAB_NUMBER", "PATIENT_NAME", "PATIENT_DOB", "PATIENT_ID", "SITE_ID"),
                fieldKeys("Order Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_NAME", "PATIENT_DOB", "PATIENT_ID", "PATIENT_SEX",
                "COLLECTION_DATETIME", "COLLECTED_BY", "TESTS"), fieldKeys("Specimen Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "SLIDE_ID", "STAIN_TYPE", "BLOCK_ID", "CASE_NUMBER"),
                fieldKeys("Slide Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "BLOCK_ID", "SPECIMEN_TYPE", "CASE_NUMBER"),
                fieldKeys("Block Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "STORAGE_LOCATION", "SPECIMEN_TYPE", "COLLECTION_DATETIME",
                "EXPIRY_DATE"), fieldKeys("Freezer Label"));
        assertEquals("positions are contiguous and only Lab Number is required",
                List.of("1:true", "2:false", "3:false", "4:false", "5:false"), positions("Order Label"));
    }

    @Test
    public void anUpgradedSiteLandsOnExactlyWhatItPrinted() throws Exception {
        insertElementKey("specimenLabelTests", "false");
        insertElementKey("orderLabelSiteId", "FALSE");
        insertElementKey("slideLabelStainType", " TRUE ");

        runFieldDefaults();

        assertEquals(List.of("LAB_NUMBER", "PATIENT_NAME", "PATIENT_DOB", "PATIENT_ID", "PATIENT_SEX",
                "COLLECTION_DATETIME", "COLLECTED_BY"), fieldKeys("Specimen Label"));
        assertEquals(List.of("1:true", "2:false", "3:false", "4:false", "5:false", "6:false", "7:false"),
                positions("Specimen Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_NAME", "PATIENT_DOB", "PATIENT_ID"), fieldKeys("Order Label"));
        assertEquals("true in any spelling keeps the field",
                List.of("LAB_NUMBER", "PATIENT_ID", "SLIDE_ID", "STAIN_TYPE", "BLOCK_ID", "CASE_NUMBER"),
                fieldKeys("Slide Label"));
    }

    @Test
    public void aPresetAnAdministratorAlreadyEditedIsLeftAlone() throws Exception {
        execute("INSERT INTO clinlims.label_preset_field (id, preset_id, field_key, source_type, is_required,"
                + " display_order, last_updated) SELECT nextval('clinlims.label_preset_field_seq'), id, 'CASE_NUMBER',"
                + " 'SYSTEM', false, 2, CURRENT_TIMESTAMP FROM clinlims.label_preset WHERE is_system = true"
                + " AND name = 'Slide Label'");

        runFieldDefaults();

        assertEquals(List.of("LAB_NUMBER", "CASE_NUMBER"), fieldKeys("Slide Label"));
        assertEquals(List.of("LAB_NUMBER", "PATIENT_ID", "BLOCK_ID", "SPECIMEN_TYPE", "CASE_NUMBER"),
                fieldKeys("Block Label"));
    }

    @Test
    public void runningTwiceChangesNothing() throws Exception {
        runFieldDefaults();
        List<String> first = allRows();

        runFieldDefaults();

        assertEquals(first, allRows());
    }

    private void runFieldDefaults() throws Exception {
        SystemPresetSeedTest.executeSeedSql(dataSource, FIELD_DEFAULTS_CHANGESET);
    }

    private List<String> fieldKeys(String presetName) throws Exception {
        return column(
                "SELECT f.field_key FROM clinlims.label_preset_field f JOIN clinlims.label_preset p"
                        + " ON p.id = f.preset_id WHERE p.is_system = true AND p.name = ? ORDER BY f.display_order",
                presetName);
    }

    private List<String> positions(String presetName) throws Exception {
        return column("SELECT f.display_order || ':' || f.is_required FROM clinlims.label_preset_field f"
                + " JOIN clinlims.label_preset p ON p.id = f.preset_id WHERE p.is_system = true AND p.name = ?"
                + " ORDER BY f.display_order", presetName);
    }

    private List<String> allRows() throws Exception {
        return column("SELECT p.name || '|' || f.field_key || '|' || f.display_order || '|' || f.is_required"
                + " FROM clinlims.label_preset_field f JOIN clinlims.label_preset p ON p.id = f.preset_id"
                + " WHERE p.is_system = true AND p.name <> ? ORDER BY p.name, f.display_order", "");
    }

    private List<String> column(String sql, String parameter) throws Exception {
        List<String> values = new ArrayList<>();
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parameter);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    values.add(rs.getString(1));
                }
            }
        }
        return values;
    }

    private void insertElementKey(String name, String value) throws Exception {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO clinlims.site_information" + " (id, name, lastupdated, value, value_type) VALUES"
                                + " (nextval('clinlims.site_information_seq'), ?, CURRENT_TIMESTAMP, ?, 'text')")) {
            stmt.setString(1, name);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    private void deleteElementKeys() throws Exception {
        execute("DELETE FROM clinlims.site_information WHERE name ~ '" + ELEMENT_KEY_PATTERN + "'");
    }

    private void execute(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
