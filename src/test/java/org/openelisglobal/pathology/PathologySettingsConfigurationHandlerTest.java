package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologySettingsConfigurationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * The shipped pathology settings file creates the scan verification setting on
 * for a laboratory that has none, and never overwrites a choice an
 * administrator made.
 */
public class PathologySettingsConfigurationHandlerTest extends BaseWebContextSensitiveTest {

    private static final String SETTING = "pathology.microtomy.scanVerificationRequired";

    @Autowired
    private PathologySettingsConfigurationHandler handler;

    @Before
    public void removeSetting() {
        // The suite runs its classes alphabetically, and the barcode fixtures truncate
        // site_information_domain on their way through, so the domain the setting
        // belongs to is put back here when a sibling has removed it.
        jdbcTemplate.update("INSERT INTO clinlims.site_information_domain (id, name, description) "
                + "SELECT nextval('clinlims.site_information_domain_seq'), 'resultConfiguration', "
                + "'site information which effects the handling of results' "
                + "WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information_domain WHERE name = 'resultConfiguration')");
        jdbcTemplate.update("DELETE FROM clinlims.site_information WHERE name = ?", SETTING);
    }

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM clinlims.site_information WHERE name = ?", SETTING);
    }

    @Test
    public void load_withNoStoredSetting_createsItOnAsABooleanInResultConfiguration() throws Exception {
        load();

        assertEquals("a missing setting is created on", "true", storedValue());
        assertEquals("the setting is a boolean", "boolean", jdbcTemplate.queryForObject(
                "SELECT value_type FROM clinlims.site_information WHERE name = ?", String.class, SETTING));
        assertEquals("the setting sits in Result Entry Configuration", "resultConfiguration",
                jdbcTemplate.queryForObject(
                        "SELECT d.name FROM clinlims.site_information s "
                                + "JOIN clinlims.site_information_domain d ON d.id = s.domain_id WHERE s.name = ?",
                        String.class, SETTING));
    }

    @Test
    public void load_afterAnAdministratorTurnedItOff_keepsItOff() throws Exception {
        load();
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = 'false' WHERE name = ?", SETTING);

        load();

        assertEquals("a later load never overwrites the administrator's choice", "false", storedValue());
    }

    private void load() throws Exception {
        try (InputStream csv = new ClassPathResource("configuration/pathology-settings/default.csv").getInputStream()) {
            handler.processConfiguration(csv, "default.csv");
        }
    }

    private String storedValue() {
        return jdbcTemplate.queryForObject("SELECT value FROM clinlims.site_information WHERE name = ?", String.class,
                SETTING);
    }
}
