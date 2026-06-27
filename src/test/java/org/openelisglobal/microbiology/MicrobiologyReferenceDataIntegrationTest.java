package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class MicrobiologyReferenceDataIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ORGANISM_ID = "ogc782-org";
    private static final String ANTIBIOTIC_ID = "ogc782-abx";
    private static final String STANDARD_ID = "ogc782-std";
    private static final String RULE_ID = "ogc782-rule";
    private static final String PANEL_ID = "ogc782-panel";
    private static final String SETUP_ID = "ogc782-setup";

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    private JdbcTemplate jdbc;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        methodId = jdbc.queryForObject("SELECT id FROM clinlims.method ORDER BY id LIMIT 1", String.class);
        cleanup();
        seedReferenceData();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void activeReferenceLookupsReturnOnlyTheRequestedWorkflow() {
        assertEquals("Escherichia coli", referenceService.getActiveOrganisms().get(0).getDisplayName());
        assertEquals("Ampicillin", referenceService.getActiveAntibiotics().get(0).getDisplayName());
        assertEquals(PANEL_ID, referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).get(0).getId());
        assertEquals(0, referenceService.getActiveAstPanels(MicroWorkflowType.MYCOLOGY).size());
        assertEquals(SETUP_ID,
                referenceService.getActiveCultureSetupForMethod(methodId, MicroWorkflowType.BACTERIOLOGY).getId());
    }

    @Test
    public void breakpointLookupReturnsBestRuleAndNullWhenMissing() {
        MicroBreakpointRule rule = breakpointService.findBreakpointRule(STANDARD_ID, ORGANISM_ID, "Enterobacterales",
                ANTIBIOTIC_ID, "MIC", null, "MIC");

        assertEquals(RULE_ID, rule.getId());
        assertEquals(new BigDecimal("8.0000"), rule.getSusceptibleValue());
        assertNull(breakpointService.findBreakpointRule(STANDARD_ID, ORGANISM_ID, "Enterobacterales", "missing", "MIC",
                null, "MIC"));
    }

    private void seedReferenceData() {
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

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.micro_culture_setup WHERE id = ?", SETUP_ID);
        jdbc.update("DELETE FROM clinlims.micro_breakpoint_rule WHERE id = ?", RULE_ID);
        jdbc.update("DELETE FROM clinlims.micro_breakpoint_standard WHERE id = ?", STANDARD_ID);
        jdbc.update("DELETE FROM clinlims.micro_ast_panel WHERE id = ?", PANEL_ID);
        jdbc.update("DELETE FROM clinlims.micro_antibiotic WHERE id = ?", ANTIBIOTIC_ID);
        jdbc.update("DELETE FROM clinlims.micro_organism WHERE id = ?", ORGANISM_ID);
    }
}
