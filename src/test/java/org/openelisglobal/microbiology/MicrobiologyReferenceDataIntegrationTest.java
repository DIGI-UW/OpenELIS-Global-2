package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MicrobiologyReferenceDataIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicrobiologyReferenceService referenceService;

    @Autowired
    private MicroBreakpointService breakpointService;

    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        referenceData = fixtures.createReferenceData(fixtures.firstMethodId());
    }

    @Test
    public void activeReferenceLookupsReturnOnlyTheRequestedWorkflow() {
        assertEquals(referenceData.organism().getDisplayName(),
                referenceService.getActiveOrganisms().stream()
                        .filter(organism -> organism.getId().equals(referenceData.organism().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.antibiotic().getDisplayName(),
                referenceService.getActiveAntibiotics().stream()
                        .filter(antibiotic -> antibiotic.getId().equals(referenceData.antibiotic().getId())).findFirst()
                        .orElseThrow().getDisplayName());
        assertEquals(referenceData.panel().getId(),
                referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                        .filter(panel -> panel.getId().equals(referenceData.panel().getId())).findFirst().orElseThrow()
                        .getId());
        assertEquals(0, referenceService.getActiveAstPanels(MicroWorkflowType.MYCOLOGY).size());
        assertEquals(referenceData.cultureSetup().getId(),
                referenceService.getActiveCultureSetupForMethod(referenceData.cultureSetup().getMethodId(),
                        MicroWorkflowType.BACTERIOLOGY).getId());
    }

    @Test
    public void breakpointLookupReturnsBestRuleAndNullWhenMissing() {
        MicroBreakpointRule rule = breakpointService.findBreakpointRule(referenceData.standard().getId(),
                referenceData.organism().getId(), "Enterobacterales", referenceData.antibiotic().getId(), "MIC", null,
                "MIC");

        assertEquals(referenceData.rule().getId(), rule.getId());
        assertEquals(new BigDecimal("8.0000"), rule.getSusceptibleValue());
        assertNull(breakpointService.findBreakpointRule(referenceData.standard().getId(),
                referenceData.organism().getId(), "Enterobacterales", "missing", "MIC", null, "MIC"));
    }
}
