package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-935 — the EQA programme configuration handler. A deployment's programme
 * registry is CSV under {@code configuration/backend/eqa-programs/}, loaded by
 * the configuration-initialization framework; these tests feed the handler
 * directly, including the shipped CPHL template, and assert the upsert
 * semantics row by row.
 */
public class EQAProgramConfigurationHandlerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String PROVIDER = "Central Public Health Laboratory (CPHL), Port Moresby";
    private static final String[] CPHL_PROGRAMMES = { "CPHL National HIV Serology EQA",
            "CPHL National HIV Viral Load EQA", "CPHL National EID EQA", "CPHL National HIV Recency EQA",
            "CPHL National COVID-19 Molecular EQA", "CPHL National TB Microscopy EQA" };

    @Autowired
    @Qualifier("EQAProgramConfigurationHandler")
    private DomainConfigurationHandler handler;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    @After
    public void cleanUp() {
        jdbc().update("DELETE FROM clinlims.eqa_program WHERE provider = ? OR name LIKE 'Handler Test%'", PROVIDER);
    }

    private void load(String csv, String fileName) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    @Test
    public void shippedCphlTemplateLoadsSixProgrammesAndRerunsWithoutDuplicating() throws Exception {
        Path template = Path.of("docs/eqa/cphl-eqa-programs.csv");
        try (InputStream first = Files.newInputStream(template)) {
            handler.processConfiguration(first, template.getFileName().toString());
        }

        List<Map<String, Object>> rows = jdbc()
                .queryForList("SELECT name, scheme_type, frequency, is_active, fhir_uuid, test_section_id"
                        + " FROM clinlims.eqa_program WHERE provider = ? ORDER BY id", PROVIDER);
        assertEquals(6, rows.size());
        for (int i = 0; i < CPHL_PROGRAMMES.length; i++) {
            assertEquals(CPHL_PROGRAMMES[i], rows.get(i).get("name"));
            assertEquals("REGIONAL_PT", rows.get(i).get("scheme_type"));
            assertEquals("Quarterly", rows.get(i).get("frequency"));
            assertEquals(Boolean.TRUE, rows.get(i).get("is_active"));
            assertNotNull("fhir_uuid must be generated", rows.get(i).get("fhir_uuid"));
        }
        assertEquals("each programme carries its own FHIR identity", Integer.valueOf(6),
                jdbc().queryForObject("SELECT count(DISTINCT fhir_uuid) FROM clinlims.eqa_program WHERE provider = ?",
                        Integer.class, PROVIDER));
        assertSectionMapping(rows.get(0), "Serology");
        assertSectionMapping(rows.get(1), "Molecular Biology");
        assertSectionMapping(rows.get(2), "Molecular Biology");
        assertSectionMapping(rows.get(3), "Serology");
        assertSectionMapping(rows.get(4), "Molecular Biology");
        assertSectionMapping(rows.get(5), "Microbiology");

        // The checksum layer normally prevents a second run of an unchanged file;
        // the handler itself must still upsert, not duplicate, when re-fed (edited
        // file, forced reload).
        try (InputStream second = Files.newInputStream(template)) {
            handler.processConfiguration(second, template.getFileName().toString());
        }
        assertEquals("re-run upserts instead of duplicating", Integer.valueOf(6), jdbc().queryForObject(
                "SELECT count(*) FROM clinlims.eqa_program WHERE provider = ?", Integer.class, PROVIDER));
    }

    @Test
    public void updateRowChangesNamedColumnsAndKeepsUnmentionedValues() throws Exception {
        load("name,description,provider,schemeType,frequency,testSection,active\n"
                + "Handler Test Scheme,First description,\"" + PROVIDER + "\",REGIONAL_PT,Quarterly,Serology,Y\n",
                "first.csv");
        Long id = jdbc().queryForObject("SELECT id FROM clinlims.eqa_program WHERE name = 'Handler Test Scheme'",
                Long.class);

        // Second file names the same programme but only corrects the frequency.
        load("name,frequency\nHandler Test Scheme,Biannual\n", "second.csv");

        Map<String, Object> row = jdbc()
                .queryForMap("SELECT id, description, provider, scheme_type, frequency FROM clinlims.eqa_program"
                        + " WHERE name = 'Handler Test Scheme'");
        assertEquals("same row updated, not replaced", id, Long.valueOf(((Number) row.get("id")).longValue()));
        assertEquals("Biannual", row.get("frequency"));
        assertEquals("unmentioned column survives the update", "First description", row.get("description"));
        assertEquals(PROVIDER, row.get("provider"));
        assertEquals("REGIONAL_PT", row.get("scheme_type"));
    }

    @Test
    public void unknownSectionAndBadSchemeTypeDegradeWithoutFailingTheFile() throws Exception {
        load("name,provider,schemeType,testSection\n"
                + "Handler Test No Section,Some Provider,REGIONAL_PT,No Such Section\n"
                + "Handler Test Bad Type,Some Provider,NOT_A_TYPE,Serology\n"
                + "Handler Test No Provider,,REGIONAL_PT,Serology\n" + "Handler Test Defaults,Some Provider,,\n",
                "degrade.csv");

        assertNull("unknown section resolves to NULL, row still lands",
                jdbc().queryForMap(
                        "SELECT test_section_id FROM clinlims.eqa_program" + " WHERE name = 'Handler Test No Section'")
                        .get("test_section_id"));
        assertEquals("a row with an invalid schemeType is skipped, not inserted half-formed", Integer.valueOf(0),
                jdbc().queryForObject("SELECT count(*) FROM clinlims.eqa_program WHERE name = 'Handler Test Bad Type'",
                        Integer.class));
        assertEquals("BR-004 still guards config rows: external scheme without provider is skipped", Integer.valueOf(0),
                jdbc().queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_program WHERE name = 'Handler Test No Provider'",
                        Integer.class));
        assertEquals("empty schemeType falls back to the entity default", "INTERNATIONAL_PT",
                jdbc().queryForMap(
                        "SELECT scheme_type FROM clinlims.eqa_program" + " WHERE name = 'Handler Test Defaults'")
                        .get("scheme_type"));
    }

    private void assertSectionMapping(Map<String, Object> row, String sectionName) {
        Number expected = jdbc().query(
                "SELECT id FROM clinlims.test_section WHERE name = ? AND is_active = 'Y' ORDER BY id LIMIT 1",
                rs -> rs.next() ? (Number) rs.getObject("id") : null, sectionName);
        Number actual = (Number) row.get("test_section_id");
        assertEquals("section mapping for " + row.get("name"), expected == null ? null : expected.longValue(),
                actual == null ? null : actual.longValue());
    }
}
