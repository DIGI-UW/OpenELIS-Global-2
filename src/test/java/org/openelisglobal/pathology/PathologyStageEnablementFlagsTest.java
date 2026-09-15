package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties.OEProperties;
import org.openelisglobal.program.util.PathologyStages;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Before this, the FRS's per-deployment switches for the seven optional
 * pathology bench stages had no way to be stored: {@code site_information.name}
 * was VARCHAR(32), and the standard name for a switch, for example
 * {@code pathology.stage.DECALCIFICATION.enabled}, is 39 characters, so no such
 * row could exist under that name (FR-2.3, AC-7).
 *
 * <p>
 * The test database is built by the full Liquibase changelog before this class
 * runs, so changesets 035 (widen the column) and 036 (seed the seven rows) have
 * already executed and this class needs no DBUnit fixture of its own.
 */
public class PathologyStageEnablementFlagsTest extends BaseWebContextSensitiveTest {

    private static final String RESULT_CONFIGURATION_DOMAIN_ID = "SELECT id FROM clinlims.site_information_domain"
            + " WHERE name = 'resultConfiguration'";

    private static final int OPTIONAL_STAGES = 7;

    @Autowired
    private DefaultConfigurationProperties defaultConfigurationProperties;

    @Test
    public void siteInformationName_isWideEnoughForTheStandardNames() {
        Map<String, Object> column = jdbcTemplate.queryForMap("SELECT character_maximum_length, is_nullable"
                + " FROM information_schema.columns WHERE table_schema = 'clinlims'"
                + " AND table_name = 'site_information' AND column_name = 'name'");

        assertEquals(
                "changeset 035 widens site_information.name to 60 characters so the standard"
                        + " pathology.stage.<STATUS>.enabled names fit without shortening",
                Integer.valueOf(60), column.get("character_maximum_length"));
        assertEquals("widening the column must not relax the NOT NULL constraint it already carried", "NO",
                column.get("is_nullable"));
    }

    @Test
    public void everyOptionalStage_hasAFlagRowThatShipsOn() {
        // domain_id is a numeric(10,0) column, so the driver hands back a
        // java.math.BigDecimal; comparing longValue() keeps the assertion honest about
        // which domain the row belongs to without depending on the JDBC wrapper type.
        long resultConfigurationDomainId = ((Number) jdbcTemplate.queryForObject(RESULT_CONFIGURATION_DOMAIN_ID,
                Number.class)).longValue();

        optionalStageSwitches().forEach((status, property) -> {
            List<Map<String, Object>> rows = jdbcTemplate
                    .queryForList("SELECT value, value_type, domain_id, instruction_key FROM clinlims.site_information"
                            + " WHERE name = ?", property.getDBName());

            assertEquals("exactly one row seeds the switch for " + status, 1, rows.size());
            Map<String, Object> row = rows.get(0);
            assertEquals(status + "'s switch ships on so a fresh deployment behaves exactly like the bench"
                    + " already did before the switch existed", "true", row.get("value"));
            assertEquals(status + "'s switch is a boolean, not free text", "boolean", row.get("value_type"));
            assertEquals(status + "'s switch belongs on the Result Entry Configuration admin page",
                    resultConfigurationDomainId, ((Number) row.get("domain_id")).longValue());
            assertEquals(status + "'s switch shares the one instruction text every stage switch uses",
                    "instructions.pathology.stage.enabled", row.get("instruction_key"));
        });
    }

    @Test
    public void noMandatoryStage_hasAFlagRow() {
        for (PathologyStatus status : PathologyStages.ordered()) {
            if (PathologyStages.isMandatory(status)) {
                String mandatoryName = "pathology.stage." + status.name() + ".enabled";
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.site_information WHERE name = ?", Integer.class, mandatoryName);

                assertEquals(status + " is mandatory, so nothing may disable it and it must carry no switch",
                        Integer.valueOf(0), count);
            }
        }

        Integer allStageFlags = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.site_information WHERE name LIKE 'pathology.stage.%.enabled'",
                Integer.class);

        assertEquals("exactly the seven optional stages have a switch, one row each", Integer.valueOf(OPTIONAL_STAGES),
                allStageFlags);
    }

    @Test
    public void everyFlag_readsAsEnabledThroughConfigurationProperties() {
        optionalStageSwitches().forEach((status, property) -> assertEquals(
                status + "'s seeded row must be readable through the configuration layer the rest of the"
                        + " application uses",
                "true", ConfigurationProperties.getInstance().getPropertyValue(property)));
    }

    @Test
    public void theDbName_roundTripsToItsConstant() {
        optionalStageSwitches().forEach((status, property) -> assertEquals(
                "the stored name must resolve back to the same constant, or the value loaded from the"
                        + " database would never reach this property",
                property, Property.fromDBName(property.getDBName())));
    }

    /**
     * The value a deployment falls back to when its own row is missing or deleted,
     * so a stage can never be switched off simply by the row's absence.
     */
    @Test
    public void aDeploymentWithoutTheRow_stillVisitsTheStage() {
        OEProperties hardcoded = (OEProperties) ReflectionTestUtils.invokeMethod(defaultConfigurationProperties,
                "loadHardcodedProperties");

        optionalStageSwitches().forEach((status, property) -> assertEquals(
                status + "'s hardcoded default must be enabled, since a deployment whose row is missing or"
                        + " deleted must still visit the stage",
                "true", hardcoded.getPropertyHolder(property).getValue()));
    }

    // helpers

    /**
     * The optional stages paired with their switches, in bench order. The size is
     * asserted here so that a mapping answering empty for every stage could not
     * turn the loops above into tests that check nothing.
     */
    private static Map<PathologyStatus, Property> optionalStageSwitches() {
        Map<PathologyStatus, Property> switches = new LinkedHashMap<>();
        for (PathologyStatus status : PathologyStages.ordered()) {
            PathologyStages.enablementProperty(status).ifPresent(property -> switches.put(status, property));
        }
        assertEquals("seven optional stages carry a switch, so every loop over them checks seven", OPTIONAL_STAGES,
                switches.size());
        return switches;
    }
}
