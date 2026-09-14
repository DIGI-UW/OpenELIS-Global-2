package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryLotUnavailableException;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.service.InventoryTransactionService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures.ReferenceData;
import org.openelisglobal.microbiology.service.MicroCaseAnalysisService;
import org.openelisglobal.microbiology.service.MicroCaseInoculationService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.service.MicroReagentLotService;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class MicroReagentLotTransactionIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private MicroCaseAnalysisService caseAnalysisService;

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
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @Autowired
    private InventoryUsageService inventoryUsageService;

    @Autowired
    private InventoryTransactionService inventoryTransactionService;

    @Autowired
    private TestReagentLinkService testReagentLinkService;

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
        SelectedLot fixture = createSelectedLot("stale");
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
            assertEquals(fixture.lotNumber(), expected.getLotNumber());
        }

        assertEquals(fixture.quantityBefore(), inventoryLotService.get(fixture.lotId()).getCurrentQuantity(), 0.001);
        assertTrue(inventoryUsageService.getByAnalysisId(Long.valueOf(fixture.analysisId())).isEmpty());
        assertTrue(reagentLotService.getUsageHistory(fixture.caseId()).isEmpty());
        assertEquals(MicroCaseStage.RECEIVED.name(), caseService.getCase(fixture.caseId()).getStage());
    }

    @Test
    public void downstreamProvenanceFailureRollsBackLotTransactionUsageAndQuantity() {
        SelectedLot fixture = createSelectedLot("rollback");
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
        SelectedLot fixture = createSelectedLot("inoculation");

        var inoculation = inoculationService.record(fixture.caseId(), null,
                "UAT-INTEGRATION-BOTTLE-" + UUID.randomUUID(), "Blood culture bottle", null, null,
                List.of(fixture.selection()), fixture.userId());

        assertEquals(fixture.caseId(), inoculation.getCaseId());
        assertTrue(inoculation.getMethodId().matches("\\d+"));
        assertEquals(1, reagentLotService.getUsageHistory(fixture.caseId()).size());
        assertEquals(fixture.lotNumber(), reagentLotService.getUsageHistory(fixture.caseId()).get(0).lotNumber);
    }

    private SelectedLot createSelectedLot(String scenarioName) {
        String userId = fixtures.defaultUserId();
        String methodId = fixtures.createMethodId();
        ReferenceData referenceData = fixtures.createReferenceData(methodId);
        SampleItem sampleItem = fixtures.createSampleWithSampleItem("OGC782M8L");
        var microCase = caseService.createOrGetCase(sampleItem.getId(), MicroWorkflowType.BACTERIOLOGY, methodId,
                userId);
        var test = fixtures.createCatalogTest();
        Analysis analysis = createAnalysis(sampleItem, test, userId);
        caseAnalysisService.linkAnalysis(microCase, analysis, referenceData.cultureSetup());

        InventoryItem item = getOrCreateInventoryItem(userId);
        TestReagentLink reagentLink = new TestReagentLink();
        reagentLink.setTestId(test.getId());
        reagentLink.setReagentId(item.getId());
        reagentLink.setUsageType("PRIMARY");
        reagentLink.setQuantityPerTest(BigDecimal.ONE);
        reagentLink.setQuantityUnit(item.getUnits() == null ? "unit" : item.getUnits());
        reagentLink.setSysUserId(userId);
        testReagentLinkService.insert(reagentLink);

        String lotNumber = "M8-" + scenarioName + "-" + UUID.randomUUID().toString().substring(0, 12);
        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setLotNumber(lotNumber);
        lot.setExpirationDate(Timestamp.from(Instant.now().plusSeconds(30L * 86_400L)));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        InventoryLot receivedLot = inventoryManagementService.receiveInventory(lot, userId);

        var requirement = reagentLotService.getRequirements(microCase.getId()).stream()
                .filter(candidate -> reagentLink.getId().equals(candidate.linkId)).findFirst().orElseThrow();
        var selectedLot = requirement.lots.stream().filter(candidate -> receivedLot.getId().equals(candidate.id))
                .findFirst().orElseThrow();
        assertTrue(selectedLot.available);
        return new SelectedLot(microCase.getId(), analysis.getId(), userId, selectedLot.id, lotNumber,
                selectedLot.currentQuantity,
                new MicroLotSelection(analysis.getId(), reagentLink.getId(), receivedLot.getId()));
    }

    private Analysis createAnalysis(SampleItem sampleItem, org.openelisglobal.test.valueholder.Test test,
            String userId) {
        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        analysis.setAnalysisType("MANUAL");
        analysis.setIsReportable(IActionConstants.YES);
        analysis.setRevision("0");
        analysis.setStartedDate(Timestamp.from(Instant.now()));
        analysis.setStatusId(fixtures.ensureAnalysisNotStartedStatus());
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setSysUserId(userId);
        analysisService.insert(analysis);
        return analysis;
    }

    private InventoryItem getOrCreateInventoryItem(String userId) {
        List<InventoryItem> existingItems = inventoryItemService.getAll();
        if (!existingItems.isEmpty()) {
            return existingItems.get(0);
        }

        InventoryItem item = new InventoryItem();
        item.setFhirUuid(UUID.randomUUID());
        item.setName("M8 reagent " + UUID.randomUUID().toString().substring(0, 12));
        item.setDescription("Service-created M8 transaction fixture");
        item.setItemType(ItemType.REAGENT);
        item.setCategory("Microbiology test");
        item.setUnits("unit");
        item.setQuantityPerUnit(1);
        item.setLowStockThreshold(1);
        item.setExpirationAlertDays(30);
        item.setIsActive(IActionConstants.YES);
        item.setSysUserId(userId);
        inventoryItemService.insert(item);
        return item;
    }

    private boolean hasMessage(Throwable throwable, String expectedText) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    private record SelectedLot(String caseId, String analysisId, String userId, Long lotId, String lotNumber,
            double quantityBefore, MicroLotSelection selection) {
    }
}
