package org.openelisglobal.sampletypeterminology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.sampletypeterminology.service.SampleTypeTerminologyMappingService;
import org.openelisglobal.sampletypeterminology.valueholder.SampleTypeTerminologyMapping;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A LOINC code in a sample-types configuration file has to reach the editor.
 *
 * <p>
 * The Sample Type Editor's Terminology section reads
 * {@code sample_type_terminology_mapping}, so a {@code loinc} column that only
 * landed on the sample type row would be a code nobody could see or edit. The
 * import records it as LOINC / SAME_AS.
 *
 * <p>
 * Deliberately narrower than a full reconcile: an import knows about one code
 * and must leave alone both the other terminology systems an administrator
 * configured and — when the column is absent — any LOINC already recorded.
 */
public class SampleTypeConfiguredLoincTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE_TYPE = 96501L;
    private static final long LOCALIZATION = 96502L;

    @Autowired
    private SampleTypeTerminologyMappingService mappingService;

    // By interface, not by class: processConfiguration is @Transactional, so the
    // bean the context holds is a JDK proxy and cannot be assigned to the concrete
    // handler type.
    @Autowired
    @Qualifier("typeOfSampleConfigurationHandler")
    private DomainConfigurationHandler handler;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String sampleTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                LOCALIZATION, "sampleType name");
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                + " VALUES (?, ?, 'en', ?, NOW())", LOCALIZATION, LOCALIZATION, "ConfiguredLoincSpecimen");
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, 'H', true, 1, ?, NOW())",
                SAMPLE_TYPE, "ConfiguredLoincSpecimen", LOCALIZATION);
        sampleTypeId = String.valueOf(SAMPLE_TYPE);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void aConfiguredCodeBecomesASameAsMapping() {
        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        List<SampleTypeTerminologyMapping> mappings = mappingService.getActiveBySampleTypeId(sampleTypeId);

        assertEquals(1, mappings.size());
        assertEquals("LOINC", mappings.get(0).getSource());
        assertEquals("LP7057-5", mappings.get(0).getCode());
        assertEquals("SAME_AS", mappings.get(0).getRelationship());
    }

    @Test
    public void surroundingWhitespaceIsNotPartOfTheCode() {
        mappingService.syncConfiguredLoinc(sampleTypeId, "  LP7576-4  ", "1");

        assertEquals("LP7576-4", mappingService.getActiveBySampleTypeId(sampleTypeId).get(0).getCode());
    }

    @Test
    public void reimportingIsIdempotent() {
        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        assertEquals("a second import adds nothing", 1, mappingService.getActiveBySampleTypeId(sampleTypeId).size());
    }

    @Test
    public void changingTheConfiguredCodeRetiresTheOneItReplaced() {
        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7576-4", "1");

        List<SampleTypeTerminologyMapping> active = mappingService.getActiveBySampleTypeId(sampleTypeId);
        assertEquals(1, active.size());
        assertEquals("LP7576-4", active.get(0).getCode());
    }

    @Test
    public void anAbsentColumnSaysNothingRatherThanClearingTheCode() {
        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        // A file without the column, or with the cell empty: silence, not a request
        // to remove what is already configured.
        mappingService.syncConfiguredLoinc(sampleTypeId, "", "1");
        mappingService.syncConfiguredLoinc(sampleTypeId, null, "1");

        assertEquals("the existing mapping survives", 1, mappingService.getActiveBySampleTypeId(sampleTypeId).size());
    }

    @Test
    public void otherTerminologySystemsAreLeftAlone() {
        jdbc.update("INSERT INTO clinlims.sample_type_terminology_mapping"
                + " (id, sample_type_id, source, code, relationship, is_active, lastupdated, last_updated)"
                + " VALUES (gen_random_uuid()::varchar, ?, 'SNOMED', '119297000', 'BROADER_THAN', 'Y', NOW(), NOW())",
                SAMPLE_TYPE);

        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        assertTrue("the SNOMED mapping survives an import", mappingService.getActiveBySampleTypeId(sampleTypeId)
                .stream().anyMatch(m -> "SNOMED".equals(m.getSource())));
    }

    @Test
    public void aRelationshipChosenInTheEditorIsNotOverwritten() {
        jdbc.update("INSERT INTO clinlims.sample_type_terminology_mapping"
                + " (id, sample_type_id, source, code, relationship, is_active, lastupdated, last_updated)"
                + " VALUES (gen_random_uuid()::varchar, ?, 'LOINC', 'LP7057-5', 'NARROWER_THAN', 'Y', NOW(), NOW())",
                SAMPLE_TYPE);

        mappingService.syncConfiguredLoinc(sampleTypeId, "LP7057-5", "1");

        // The file names the code; what it means is the administrator's call.
        assertEquals("NARROWER_THAN", mappingService.getActiveBySampleTypeId(sampleTypeId).get(0).getRelationship());
    }

    // ── loading it from a configuration file ──────────────────────────────────

    /**
     * The abbreviation the import keys on, so the fixture can find its row again.
     */
    private static final String IMPORTED_ABBREV = "CLQ";

    @Test
    public void aLoincColumnInAConfigurationFileBecomesAMapping() throws Exception {
        String csv = "description,localAbbreviation,domain,isActive,sortOrder,loinc,localization:en\n"
                + "Configured Loinc Import," + IMPORTED_ABBREV + ",H,Y,900,LP7681-2,Configured Loinc Import\n";

        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "loinc-test.csv");

        String importedId = importedSampleTypeId();
        List<SampleTypeTerminologyMapping> mappings = mappingService.getActiveBySampleTypeId(importedId);
        assertEquals("the configured code is recorded once", 1, mappings.size());
        assertEquals("LOINC", mappings.get(0).getSource());
        assertEquals("LP7681-2", mappings.get(0).getCode());
        assertEquals("SAME_AS", mappings.get(0).getRelationship());
    }

    @Test
    public void aFileWithoutTheColumnStillImports() throws Exception {
        // Every sample-types file written before the column existed, including the
        // ones the distros ship. The column is optional, not newly required.
        String csv = "description,localAbbreviation,domain,isActive,sortOrder,localization:en\n"
                + "Configured Loinc Import," + IMPORTED_ABBREV + ",H,Y,900,Configured Loinc Import\n";

        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "no-loinc.csv");

        assertTrue("the sample type is created", importedSampleTypeId() != null);
        assertTrue("and carries no LOINC mapping",
                mappingService.getActiveBySampleTypeId(importedSampleTypeId()).isEmpty());
    }

    @Test
    public void theShippedExampleFileDeclaresTheColumn() throws Exception {
        // The example is the documentation for the format; a deployment copies it.
        Path example = Path.of("volume/configuration/backend/sample-types/example-sample-types.csv");
        String header = Files.readAllLines(example).get(0);

        assertTrue("example header declares loinc: " + header, header.contains("loinc"));
    }

    /**
     * Read straight from the table: the import normalizes the domain it stores, so
     * a service lookup by the domain the file declared is not a reliable way back
     * to the row it just wrote.
     */
    private String importedSampleTypeId() {
        List<String> ids = jdbc.queryForList("SELECT id::varchar FROM clinlims.type_of_sample WHERE local_abbrev = ?",
                String.class, IMPORTED_ABBREV);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void cleanup() {
        // The imported row's id is assigned by the sequence, so clear it by the
        // abbreviation the import keys on.
        jdbc.update("DELETE FROM clinlims.sample_type_terminology_mapping WHERE sample_type_id IN"
                + " (SELECT id FROM clinlims.type_of_sample WHERE local_abbrev = ?)", IMPORTED_ABBREV);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE local_abbrev = ?", IMPORTED_ABBREV);
        jdbc.update("DELETE FROM clinlims.sample_type_terminology_mapping WHERE sample_type_id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.localization_value WHERE id = ?", LOCALIZATION);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", LOCALIZATION);
    }
}
