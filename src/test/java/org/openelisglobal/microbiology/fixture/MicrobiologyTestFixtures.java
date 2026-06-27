package org.openelisglobal.microbiology.fixture;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test-only rows for microbiology integration tests; do not move this data into
 * Liquibase migrations.
 */
public class MicrobiologyTestFixtures {

    public static final long CATALOG_TEST_ID = 78201L;
    public static final String ORGANISM_ID = "ogc782-org";
    public static final String ANTIBIOTIC_ID = "ogc782-abx";
    public static final String STANDARD_ID = "ogc782-std";
    public static final String RULE_ID = "ogc782-rule";
    public static final String PANEL_ID = "ogc782-panel";
    public static final String SETUP_ID = "ogc782-setup";
    public static final String TB_SETUP_ID = "ogc782-tb-setup";
    public static final String DEFAULT_USER_ID = "1";

    private final JdbcTemplate jdbc;

    public MicrobiologyTestFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String firstMethodId() {
        return jdbc.queryForObject("SELECT id FROM clinlims.method ORDER BY id LIMIT 1", String.class);
    }

    public String firstSampleItemId() {
        return jdbc.queryForObject("SELECT id FROM clinlims.sample_item ORDER BY id LIMIT 1", String.class);
    }

    public String insertSampleWithSampleItem(String accessionNumber) {
        String sampleId = String.valueOf(jdbc.queryForObject("SELECT nextval('clinlims.sample_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.sample"
                        + " (id, accession_number, entered_date, received_date, lastupdated, sys_user_id)"
                        + " VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(sampleId), accessionNumber);
        String sampleItemId = String
                .valueOf(jdbc.queryForObject("SELECT nextval('clinlims.sample_item_seq')", Long.class));
        jdbc.update(
                "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                        + " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Long.valueOf(sampleItemId), Long.valueOf(sampleId), 1, 1);
        return sampleItemId;
    }

    public void insertCatalogTest() {
        jdbc.update("INSERT INTO clinlims.test"
                + " (id, name, description, is_active, guid, domain, antimicrobial_resistance, orderable,"
                + " culture_workflow_type, lastupdated)"
                + " VALUES (?, 'MicroCatalogIT', 'MicroCatalogIT desc', 'Y', ?, 'CLINICAL', true, true, null, NOW())",
                CATALOG_TEST_ID, UUID.randomUUID().toString());
    }

    public void deleteCatalogTest() {
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", CATALOG_TEST_ID);
    }

    public void insertReferenceData(String methodId) {
        jdbc.update("INSERT INTO clinlims.micro_organism"
                + " (id, display_name, whonet_code, organism_group, is_active, lastupdated)"
                + " VALUES (?, 'Escherichia coli', 'eco', 'Enterobacterales', 'Y', NOW())", ORGANISM_ID);
        jdbc.update("INSERT INTO clinlims.micro_antibiotic"
                + " (id, display_name, whonet_code, antibiotic_class, is_active, lastupdated)"
                + " VALUES (?, 'Ampicillin', 'AMP', 'Penicillins', 'Y', NOW())", ANTIBIOTIC_ID);
        jdbc.update(
                "INSERT INTO clinlims.micro_ast_panel"
                        + " (id, name, workflow_type, organism_group, is_active, lastupdated)"
                        + " VALUES (?, 'Enterobacterales panel', 'BACTERIOLOGY', 'Enterobacterales', 'Y', NOW())",
                PANEL_ID);
        jdbc.update(
                "INSERT INTO clinlims.micro_breakpoint_standard"
                        + " (id, authority, version, is_active, lastupdated) VALUES (?, 'CLSI', '2026', 'Y', NOW())",
                STANDARD_ID);
        jdbc.update(
                "INSERT INTO clinlims.micro_breakpoint_rule"
                        + " (id, standard_id, organism_id, organism_group, antibiotic_id, method, breakpoint_type,"
                        + " susceptible_value, resistant_value, is_active, lastupdated)"
                        + " VALUES (?, ?, ?, 'Enterobacterales', ?, 'MIC', 'MIC', 8.0000, 32.0000, 'Y', NOW())",
                RULE_ID, STANDARD_ID, ORGANISM_ID, ANTIBIOTIC_ID);
        jdbc.update("INSERT INTO clinlims.micro_culture_setup"
                + " (id, method_id, name, workflow_type, media_defaults, incubation_defaults, atmosphere_defaults,"
                + " is_active, lastupdated) VALUES (?, ?, 'Urine culture', 'BACTERIOLOGY', 'Blood agar',"
                + " '18-24h', 'Ambient', 'Y', NOW())", SETUP_ID, Long.valueOf(methodId));
    }

    public void insertTbCultureSetup(String methodId) {
        jdbc.update("INSERT INTO clinlims.micro_culture_setup"
                + " (id, method_id, name, workflow_type, media_defaults, incubation_defaults, atmosphere_defaults,"
                + " is_active, lastupdated) VALUES (?, ?, 'TB culture', 'MYCOBACTERIOLOGY_TB', 'MGIT',"
                + " 'up to 42 days', 'Ambient', 'Y', NOW())", TB_SETUP_ID, Long.valueOf(methodId));
    }

    public void deleteReferenceData() {
        jdbc.update("DELETE FROM clinlims.micro_culture_setup WHERE id = ?", TB_SETUP_ID);
        jdbc.update("DELETE FROM clinlims.micro_culture_setup WHERE id = ?", SETUP_ID);
        jdbc.update("DELETE FROM clinlims.micro_breakpoint_rule WHERE id = ?", RULE_ID);
        jdbc.update("DELETE FROM clinlims.micro_breakpoint_standard WHERE id = ?", STANDARD_ID);
        jdbc.update("DELETE FROM clinlims.micro_ast_panel WHERE id = ?", PANEL_ID);
        jdbc.update("DELETE FROM clinlims.micro_antibiotic WHERE id = ?", ANTIBIOTIC_ID);
        jdbc.update("DELETE FROM clinlims.micro_organism WHERE id = ?", ORGANISM_ID);
    }

    public void deleteCaseDataForSampleItem(String sampleItemId) {
        jdbc.update("DELETE FROM clinlims.micro_isolate WHERE case_id IN"
                + " (SELECT id FROM clinlims.micro_case WHERE sample_item_id = ?)", Long.valueOf(sampleItemId));
        jdbc.update("DELETE FROM clinlims.micro_case_activity WHERE case_id IN"
                + " (SELECT id FROM clinlims.micro_case WHERE sample_item_id = ?)", Long.valueOf(sampleItemId));
        jdbc.update("DELETE FROM clinlims.micro_case WHERE sample_item_id = ?", Long.valueOf(sampleItemId));
    }

    public void deleteSampleItemAndSample(String sampleItemId) {
        String sampleId = jdbc.queryForObject("SELECT samp_id FROM clinlims.sample_item WHERE id = ?", String.class,
                Long.valueOf(sampleItemId));
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", Long.valueOf(sampleItemId));
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", Long.valueOf(sampleId));
    }
}
