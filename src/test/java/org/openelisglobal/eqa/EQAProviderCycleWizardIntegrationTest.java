package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQADistributionMethod;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-613 [EQA V2.5] — the three writes the provider cycle wizard makes on
 * confirm: the cycle with its distribution method (FR-V2.5-02 step 4), the
 * panel and its sealed targets, and the move into prep that hands the cycle to
 * the prep workbench.
 */
public class EQAProviderCycleWizardIntegrationTest extends EQASpineTestBase {

    private static final long ORG_A = 9970L;
    private static final long ORG_B = 9971L;
    private static final long ANALYTE = 9801L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQAPanelService panelService;

    @Autowired
    private EQAShipmentService shipmentService;

    @Autowired
    private EQAPanelSampleDAO eqaPanelSampleDAO;

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id IN (9970, 9971)");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.organization WHERE id IN ('9970', '9971')");
        }
    }

    private EQAProgram providerScheme() {
        return insertScheme("Provider wizard " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
    }

    private void enrol(EQAProgram scheme, long organizationId) {
        jdbc.update(
                "INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING",
                organizationId, "Participant lab " + organizationId);
        jdbc.update(
                "INSERT INTO clinlims.eqa_program_enrollment (id, eqa_program_id, organization_id,"
                        + " enrollment_date, status, sys_user_id, lastupdated)"
                        + " VALUES (nextval('clinlims.eqa_enrollment_seq'), ?, ?, now(), 'Active', ?, now())",
                scheme.getId(), organizationId, USER);
    }

    private EQAPanel panelFor(EQAProgram scheme, EQACycle cycle, String... targets) {
        EQAPanel panel = new EQAPanel();
        panel.setScheme(scheme);
        panel.setCycle(cycle);
        panel.setPanelName("Provider panel");
        panel.setPanelType("PROVIDER");
        panel.setAliquotsProduced(4);
        List<EQAPanelSample> samples = new java.util.ArrayList<>();
        for (String target : targets) {
            EQAPanelSample sample = new EQAPanelSample();
            sample.setAnalyteId(ANALYTE);
            sample.setTargetValue(target);
            samples.add(sample);
        }
        return panelService.create(panel, samples, USER);
    }

    @Test
    public void createCycleKeepsTheWizardsDistributionMethod() {
        EQAProgram scheme = providerScheme();

        EQACycle created = cycleService.create(scheme.getId(), null, "2026 Round 1", Date.valueOf("2026-09-01"),
                Date.valueOf("2026-10-15"), EQADistributionMethod.MIXED, USER);

        assertEquals(EQADistributionMethod.MIXED, readBack(created.getId()).getDistributionMethod());
        assertEquals("MIXED", jdbc.queryForObject("SELECT distribution_method FROM clinlims.eqa_cycle WHERE id = ?",
                String.class, created.getId()));
        assertEquals(EQACycleStatus.PLANNED, readBack(created.getId()).getStatus());
        assertEquals(Date.valueOf("2026-10-15"), readBack(created.getId()).getPlannedEndDate());
    }

    @Test
    public void aCycleWithoutTheWizardHasNoDistributionMethod() {
        EQAProgram scheme = providerScheme();

        EQACycle created = cycleService.create(scheme.getId(), null, "Legacy round", null, null, USER);

        assertNull("nothing may invent a method the provider never chose",
                readBack(created.getId()).getDistributionMethod());
    }

    @Test
    public void theDatabaseRefusesADistributionMethodOutsideTheEnum() {
        EQAProgram scheme = providerScheme();
        Long cycleId = insertCycle(scheme, 4);

        try {
            // Short enough to reach the CHECK: a longer value trips VARCHAR(10) first
            // and would pass this test for the wrong reason.
            jdbc.update("UPDATE clinlims.eqa_cycle SET distribution_method = 'PIGEON' WHERE id = ?", cycleId);
            fail("eqa_cycle_distribution_method_chk must reject values the enum does not carry");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_cycle_distribution_method_chk");
        }
    }

    @Test
    public void providerPanelsGetANeutralBlindCodePrefix() {
        EQAProgram provider = providerScheme();
        EQACycle providerCycle = cycleService.create(provider.getId(), null, "Shipped round", null,
                Date.valueOf("2026-10-15"), EQADistributionMethod.FHIR, USER);
        EQAPanel providerPanel = panelFor(provider, providerCycle, "4.52", "5.10");

        EQAProgram inHouse = insertScheme("In-house " + System.nanoTime(), EQASchemeType.IN_HOUSE, null);
        EQACycle inHouseCycle = cycleService.create(inHouse.getId(), null, "Blind round", null, null, USER);
        EQAPanel inHousePanel = panelFor(inHouse, inHouseCycle, "9.9");

        assertEquals(List.of("EQA-" + providerPanel.getId() + "-01", "EQA-" + providerPanel.getId() + "-02"),
                blindCodes(providerPanel));
        assertEquals("an in-house panel keeps the prefix its labels already carry",
                List.of("IH-" + inHousePanel.getId() + "-01"), blindCodes(inHousePanel));
    }

    @Test
    public void confirmMovesTheCycleIntoPrepAndSaysWhy() {
        EQAProgram scheme = providerScheme();
        EQACycle cycle = cycleService.create(scheme.getId(), null, "2026 Round 2", null, Date.valueOf("2026-11-01"),
                EQADistributionMethod.CSV, USER);
        panelFor(scheme, cycle, "4.52");

        cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PROVIDER,
                EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, ADMIN_USER_ID,
                "Cycle created from the provider wizard", USER);

        assertEquals(EQACycleStatus.PREP_IN_PROGRESS, readBack(cycle.getId()).getStatus());
        List<EQACycleStateTransition> audit = cycleService.getTransitions(cycle.getId());
        assertEquals(1, audit.size());
        assertEquals(EQACycleStatus.PLANNED.name(), audit.get(0).getPriorState());
        assertEquals(EQACycleStatus.PREP_IN_PROGRESS.name(), audit.get(0).getNewState());
        assertEquals(EQAStateMachine.PROVIDER, audit.get(0).getStateMachine());
        assertEquals("Cycle created from the provider wizard", audit.get(0).getReason());
    }

    @Test
    public void theSchemeListReadsCycleSchemeAndMethodFromOneRow() {
        EQAProgram scheme = providerScheme();
        enrol(scheme, ORG_A);
        enrol(scheme, ORG_B);
        EQACycle cycle = cycleService.create(scheme.getId(), null, "2026 Round 3", null, Date.valueOf("2026-12-01"),
                EQADistributionMethod.FHIR, USER);
        panelFor(scheme, cycle, "4.52", "5.10");

        List<Map<String, Object>> rows = shipmentService.getProviderCycles();

        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals(cycle.getId(), row.get("id"));
        assertEquals("the scheme list groups by this id, so it has to be on the row", scheme.getId(),
                row.get("schemeId"));
        assertEquals("FHIR", row.get("distributionMethod"));
        assertEquals(2, row.get("participantCount"));
        assertEquals(1, row.get("panelCount"));
    }

    private List<String> blindCodes(EQAPanel panel) {
        return eqaPanelSampleDAO.getAllMatchingOrdered("panel.id", panel.getId(), "sampleCode", false).stream()
                .map(EQAPanelSample::getBlindCode).toList();
    }

    @Test
    public void aCycleForAnUnenrolledSchemeStaysOffTheProviderList() {
        EQAProgram scheme = providerScheme();
        EQACycle cycle = cycleService.create(scheme.getId(), null, "Nobody enrolled", null, null,
                EQADistributionMethod.FHIR, USER);

        assertTrue("a cycle nobody takes part in is not a provider cycle yet",
                shipmentService.getProviderCycles().stream().noneMatch(row -> cycle.getId().equals(row.get("id"))));
    }
}
