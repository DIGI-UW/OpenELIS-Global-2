package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class MicrobiologyReferenceDataIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    private JdbcTemplate jdbc;
    private MicrobiologyTestFixtures fixtures;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        fixtures = new MicrobiologyTestFixtures(jdbc);
        methodId = fixtures.firstMethodId();
        cleanup();
        fixtures.insertReferenceData(methodId);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void activeReferenceLookupsReturnOnlyTheRequestedWorkflow() {
        assertEquals("Escherichia coli", referenceService.getActiveOrganisms().get(0).getDisplayName());
        assertEquals("Ampicillin", referenceService.getActiveAntibiotics().get(0).getDisplayName());
        assertEquals(MicrobiologyTestFixtures.PANEL_ID,
                referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).get(0).getId());
        assertEquals(0, referenceService.getActiveAstPanels(MicroWorkflowType.MYCOLOGY).size());
        assertEquals(MicrobiologyTestFixtures.SETUP_ID,
                referenceService.getActiveCultureSetupForMethod(methodId, MicroWorkflowType.BACTERIOLOGY).getId());
    }

    @Test
    public void breakpointLookupReturnsBestRuleAndNullWhenMissing() {
        MicroBreakpointRule rule = breakpointService.findBreakpointRule(MicrobiologyTestFixtures.STANDARD_ID,
                MicrobiologyTestFixtures.ORGANISM_ID, "Enterobacterales", MicrobiologyTestFixtures.ANTIBIOTIC_ID, "MIC",
                null, "MIC");

        assertEquals(MicrobiologyTestFixtures.RULE_ID, rule.getId());
        assertEquals(new BigDecimal("8.0000"), rule.getSusceptibleValue());
        assertNull(breakpointService.findBreakpointRule(MicrobiologyTestFixtures.STANDARD_ID,
                MicrobiologyTestFixtures.ORGANISM_ID, "Enterobacterales", "missing", "MIC", null, "MIC"));
    }

    private void cleanup() {
        if (fixtures != null) {
            fixtures.deleteReferenceData();
        }
    }
}
