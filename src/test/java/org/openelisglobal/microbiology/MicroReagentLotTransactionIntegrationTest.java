package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryLotUnavailableException;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseInoculationService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.service.MicroReagentLotService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class MicroReagentLotTransactionIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicrobiologyUatScenarioService scenarioService;

    @Autowired
    private MicroReagentLotService reagentLotService;

    @Autowired
    private MicroCaseStateService caseStateService;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCaseInoculationService inoculationService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
    }

    @Test
    public void staleEligibleLotIsRevalidatedAndRejectedWithoutBenchMutation() {
        SelectedLot fixture = createSelectedLot("stale", "UAT-MICRO-MEDIA-FEFO");
        InventoryLot lot = inventoryLotService.get(fixture.lotId());
        lot.setExpirationDate(new Timestamp(System.currentTimeMillis() - 60_000));
        lot.setSysUserId(fixture.userId());
        inventoryLotService.update(lot);

        try {
            caseStateService.advanceStage(fixture.caseId(), MicroCaseStage.SETUP_RECORDED, fixture.userId(),
                    "Reject a lot that changed after selection", List.of(fixture.selection()));
            fail("Expected the stale lot selection to be rejected");
        } catch (InventoryLotUnavailableException expected) {
            assertEquals("INVENTORY_LOT_EXPIRED", expected.getCode());
            assertEquals("UAT-MICRO-MEDIA-FEFO", expected.getLotNumber());
        }

        assertEquals(fixture.quantityBefore(), inventoryLotService.get(fixture.lotId()).getCurrentQuantity(), 0.001);
        assertTrue(inventoryUsageService.getByAnalysisId(Long.valueOf(fixture.analysisId())).isEmpty());
        assertTrue(reagentLotService.getUsageHistory(fixture.caseId()).isEmpty());
        assertEquals(MicroCaseStage.RECEIVED.name(), caseService.getCase(fixture.caseId()).getStage());
    }

    @Test
    public void downstreamProvenanceFailureRollsBackLotTransactionUsageAndQuantity() {
        SelectedLot fixture = createSelectedLot("rollback", "UAT-MICRO-CARD-FEFO");
        int transactionsBefore = inventoryTransactionService.getByLotId(fixture.lotId()).size();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        try {
            transaction.executeWithoutResult(status -> {
                reagentLotService.recordSelections(fixture.caseId(), MicroInventoryUsageContext.CULTURE_SETUP,
                        UUID.randomUUID().toString(), List.of(fixture.selection()), fixture.userId());
                entityManager.flush();
            });
            fail("Expected the invalid provenance link to roll back");
        } catch (RuntimeException expected) {
            assertTrue("Expected the activity foreign-key failure but got " + expected,
                    hasMessage(expected, "fk_micro_inventory_usage_activity"));
        }

        assertEquals(fixture.quantityBefore(), inventoryLotService.get(fixture.lotId()).getCurrentQuantity(), 0.001);
        assertEquals(transactionsBefore, inventoryTransactionService.getByLotId(fixture.lotId()).size());
        assertTrue(inventoryUsageService.getByAnalysisId(Long.valueOf(fixture.analysisId())).isEmpty());
        assertTrue(reagentLotService.getUsageHistory(fixture.caseId()).isEmpty());
    }

    @Test
    public void inoculationPersistsNumericMethodReferenceAndSelectedLotProvenance() {
        SelectedLot fixture = createSelectedLot("inoculation", "UAT-MICRO-MEDIA-FEFO");

        var inoculation = inoculationService.record(fixture.caseId(), null,
                "UAT-INTEGRATION-BOTTLE-" + UUID.randomUUID(), "Blood culture bottle", null, null,
                List.of(fixture.selection()), fixture.userId());

        assertEquals(fixture.caseId(), inoculation.getCaseId());
        assertTrue(inoculation.getMethodId().matches("\\d+"));
        assertEquals(1, reagentLotService.getUsageHistory(fixture.caseId()).size());
        assertEquals("UAT-MICRO-MEDIA-FEFO", reagentLotService.getUsageHistory(fixture.caseId()).get(0).lotNumber);
    }

    private SelectedLot createSelectedLot(String scenarioName, String lotNumber) {
        String userId = fixtures.defaultUserId();
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "MVP";
        request.scenarioKey = "lot-transaction-integration-" + scenarioName + "-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = scenarioService.provision(request, userId);
        var requirement = reagentLotService.getRequirements(scenario.caseId).stream()
                .filter(candidate -> candidate.lots.stream().anyMatch(lot -> lotNumber.equals(lot.lotNumber)))
                .findFirst().orElseThrow();
        var lot = requirement.lots.stream().filter(candidate -> lotNumber.equals(candidate.lotNumber)).findFirst()
                .orElseThrow();
        assertTrue(lot.available);
        return new SelectedLot(scenario.caseId, scenario.analysisId, userId, lot.id, lot.currentQuantity,
                new MicroLotSelection(scenario.analysisId, requirement.linkId, lot.id));
    }

    private boolean hasMessage(Throwable throwable, String expectedText) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    private record SelectedLot(String caseId, String analysisId, String userId, Long lotId, double quantityBefore,
            MicroLotSelection selection) {
    }
}
