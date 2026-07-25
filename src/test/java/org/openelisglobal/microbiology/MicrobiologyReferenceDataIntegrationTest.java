package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroBreakpointService;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
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

    @Autowired
    private MicrobiologyUatScenarioService uatScenarioService;

    private String methodId;
    private ReferenceData referenceData;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.firstMethodId();
        referenceData = fixtures.createReferenceData(methodId);
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
                referenceService.getActiveCultureSetupForMethod(methodId, MicroWorkflowType.BACTERIOLOGY).getId());
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

    @Test
    public void uatScenariosReuseReferenceConfigurationAndGeneratedCaseIdentity() {
        MicrobiologyUatScenarioRequestForm firstRequest = scenarioRequest("MVP");
        MicrobiologyUatScenarioForm first = uatScenarioService.provision(firstRequest, fixtures.defaultUserId());
        MicrobiologyUatScenarioForm retry = uatScenarioService.provision(firstRequest, fixtures.defaultUserId());
        MicrobiologyUatScenarioForm second = uatScenarioService.provision(scenarioRequest("CASE"),
                fixtures.defaultUserId());

        assertEquals(first.caseId, retry.caseId);
        assertNotEquals(first.caseId, second.caseId);
        assertEquals(1L, referenceService.getActiveAntibiotics().stream()
                .filter(antibiotic -> "CIPUAT".equals(antibiotic.getWhonetCode())).count());
        assertEquals(1L, referenceService.getActiveAstPanels(MicroWorkflowType.BACTERIOLOGY).stream()
                .filter(panel -> "Gram negative AST panel (UAT)".equals(panel.getName())).count());
    }

    private MicrobiologyUatScenarioRequestForm scenarioRequest(String scenario) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = scenario;
        request.scenarioKey = UUID.randomUUID().toString();
        return request;
    }
}
