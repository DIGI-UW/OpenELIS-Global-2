package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryLotUnavailableException;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroInventoryUsageLinkDAO;
import org.openelisglobal.microbiology.form.MicroReagentRequirementForm;
import org.openelisglobal.microbiology.form.MicroReagentUsageForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageLink;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;

@RunWith(MockitoJUnitRunner.class)
public class MicroReagentLotServiceTest {

    @Mock
    private MicroCaseAnalysisDAO caseAnalysisDAO;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private TestReagentLinkService reagentLinkService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private InventoryManagementService inventoryManagementService;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryUsageService inventoryUsageService;

    @Mock
    private MicroInventoryUsageLinkDAO usageLinkDAO;

    private MicroReagentLotService service;
    private InventoryItem reagent;
    private TestReagentLink reagentLink;

    @Before
    public void setUp() {
        service = new MicroReagentLotServiceImpl(caseAnalysisDAO, analysisService, reagentLinkService,
                inventoryItemService, inventoryLotService, inventoryManagementService, inventoryUsageService,
                usageLinkDAO);

        MicroCaseAnalysis caseAnalysis = new MicroCaseAnalysis();
        caseAnalysis.setCaseId("case-1");
        caseAnalysis.setAnalysisId("41");
        when(caseAnalysisDAO.getByCaseId("case-1")).thenReturn(List.of(caseAnalysis));
        when(caseAnalysisDAO.getByCaseAndAnalysis("case-1", "41")).thenReturn(caseAnalysis);

        Analysis analysis = new Analysis();
        analysis.setId("41");
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("22");
        test.setName("Blood culture");
        when(analysisService.getAnalysisById("41")).thenReturn(analysis);
        when(analysisService.getTest(analysis)).thenReturn(test);

        reagent = new InventoryItem();
        reagent.setId(13L);
        reagent.setName("Blood agar");
        reagent.setUnits("plate");
        when(inventoryItemService.get(13L)).thenReturn(reagent);

        reagentLink = new TestReagentLink();
        reagentLink.setId("link-1");
        reagentLink.setTestId("22");
        reagentLink.setReagentId(13L);
        reagentLink.setUsageType("PRIMARY");
        reagentLink.setQuantityPerTest(new BigDecimal("1.0"));
        reagentLink.setQuantityUnit("plate");
        when(reagentLinkService.getByTestId("22")).thenReturn(List.of(reagentLink));
        when(reagentLinkService.get("link-1")).thenReturn(reagentLink);
    }

    @Test
    public void requirementsPreserveCatalogRoleAndOrderLotsByEffectiveExpiry() {
        InventoryLot later = lot(8L, "MEDIA-LATER", 20.0, daysFromNow(60));
        InventoryLot fifo = lot(7L, "MEDIA-FIFO", 10.0, daysFromNow(10));
        InventoryLot expired = lot(6L, "MEDIA-EXPIRED", 10.0, daysFromNow(-1));
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(later, expired, fifo));

        List<MicroReagentRequirementForm> requirements = service.getRequirements("case-1");

        assertEquals(1, requirements.size());
        MicroReagentRequirementForm requirement = requirements.get(0);
        assertEquals("PRIMARY", requirement.usageType);
        assertEquals("MEDIA-EXPIRED", requirement.lots.get(0).lotNumber);
        assertFalse(requirement.lots.get(0).available);
        assertEquals("INVENTORY_LOT_EXPIRED", requirement.lots.get(0).unavailableReason);
        assertEquals("MEDIA-FIFO", requirement.lots.get(1).lotNumber);
        assertTrue(requirement.lots.get(1).available);
        assertTrue(requirement.lots.get(1).fefoRecommended);
        assertEquals("MEDIA-LATER", requirement.lots.get(2).lotNumber);
        assertFalse(requirement.lots.get(2).fefoRecommended);
    }

    @Test
    public void requirementsBlockLotBelowConfiguredPerTestQuantity() {
        reagentLink.setQuantityPerTest(new BigDecimal("2.0"));
        InventoryLot tooSmall = lot(7L, "MEDIA-LOW", 1.5, daysFromNow(10));
        InventoryLot sufficient = lot(8L, "MEDIA-FIFO", 2.0, daysFromNow(20));
        when(inventoryLotService.getByInventoryItemId(13L)).thenReturn(List.of(tooSmall, sufficient));

        List<MicroReagentRequirementForm> requirements = service.getRequirements("case-1");

        assertFalse(requirements.get(0).lots.get(0).available);
        assertEquals("INVENTORY_LOT_INSUFFICIENT_QUANTITY", requirements.get(0).lots.get(0).unavailableReason);
        assertTrue(requirements.get(0).lots.get(1).available);
        assertTrue(requirements.get(0).lots.get(1).fefoRecommended);
    }

    @Test
    public void recordSelectionsConsumesThroughInventoryAndLinksUsageToAstAction() {
        InventoryLot selectedLot = lot(7L, "MEDIA-FIFO", 10.0, daysFromNow(10));
        when(inventoryLotService.get(7L)).thenReturn(selectedLot);
        InventoryUsage usage = new InventoryUsage();
        usage.setId(31L);
        when(inventoryManagementService.consumeSelectedLot(7L, 1.0, null, 41L, "9")).thenReturn(usage);

        service.recordSelections("case-1", MicroInventoryUsageContext.AST_SETUP, "run-1",
                List.of(new MicroLotSelection("41", "link-1", 7L)), "9");

        verify(inventoryManagementService).consumeSelectedLot(7L, 1.0, null, 41L, "9");
        ArgumentCaptor<MicroInventoryUsageLink> linkCaptor = ArgumentCaptor.forClass(MicroInventoryUsageLink.class);
        verify(usageLinkDAO).insert(linkCaptor.capture());
        assertEquals("case-1", linkCaptor.getValue().getCaseId());
        assertEquals(Long.valueOf(31L), linkCaptor.getValue().getInventoryUsageId());
        assertEquals("run-1", linkCaptor.getValue().getAstRunId());
        assertEquals(MicroInventoryUsageContext.AST_SETUP.name(), linkCaptor.getValue().getUsageContext());
    }

    @Test
    public void recordSelectionsPreservesLockedInventoryConflictWithoutCreatingMicrobiologyLink() {
        InventoryLot selectedLot = lot(7L, "MEDIA-FIFO", 10.0, daysFromNow(10));
        when(inventoryLotService.get(7L)).thenReturn(selectedLot);
        when(inventoryManagementService.consumeSelectedLot(7L, 1.0, null, 41L, "9"))
                .thenThrow(new InventoryLotUnavailableException("INVENTORY_LOT_EXPIRED", "MEDIA-FIFO"));

        try {
            service.recordSelections("case-1", MicroInventoryUsageContext.CULTURE_SETUP, "activity-1",
                    List.of(new MicroLotSelection("41", "link-1", 7L)), "9");
            fail("Expected a save-time inventory conflict");
        } catch (InventoryLotUnavailableException expected) {
            assertEquals("INVENTORY_LOT_EXPIRED", expected.getCode());
            assertEquals("MEDIA-FIFO", expected.getLotNumber());
        }

        verify(usageLinkDAO, never()).insert(any(MicroInventoryUsageLink.class));
    }

    @Test
    public void recordSelectionsRejectsLotFromAnotherCatalogItemBeforeConsumption() {
        InventoryItem otherItem = new InventoryItem();
        otherItem.setId(99L);
        InventoryLot wrongLot = lot(7L, "WRONG-ITEM", 10.0, daysFromNow(10));
        wrongLot.setInventoryItem(otherItem);
        when(inventoryLotService.get(7L)).thenReturn(wrongLot);

        try {
            service.recordSelections("case-1", MicroInventoryUsageContext.CULTURE_SETUP, "activity-1",
                    List.of(new MicroLotSelection("41", "link-1", 7L)), "9");
            fail("Expected a lot from another item to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("MICROBIOLOGY_LOT_REAGENT_MISMATCH", expected.getMessage());
        }

        verify(inventoryManagementService, never()).consumeSelectedLot(any(Long.class), any(Double.class), any(), any(),
                any(String.class));
        verify(usageLinkDAO, never()).insert(any(MicroInventoryUsageLink.class));
    }

    @Test
    public void recordSelectionsRejectsCatalogLinkThatDoesNotBelongToCaseTest() {
        reagentLink.setTestId("different-test");

        try {
            service.recordSelections("case-1", MicroInventoryUsageContext.AST_SETUP, "run-1",
                    List.of(new MicroLotSelection("41", "link-1", 7L)), "9");
            fail("Expected foreign test linkage to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("MICROBIOLOGY_REAGENT_LINK_MISMATCH", expected.getMessage());
        }

        verify(inventoryManagementService, never()).consumeSelectedLot(any(Long.class), any(Double.class), any(), any(),
                any(String.class));
    }

    @Test
    public void usageHistoryKeepsConsumedLotVisibleAfterItsStatusChanges() {
        InventoryLot consumedLot = lot(7L, "MEDIA-CONSUMED", 0.0, daysFromNow(10));
        consumedLot.setStatus(LotStatus.CONSUMED);
        InventoryUsage usage = new InventoryUsage();
        usage.setId(31L);
        usage.setInventoryItem(reagent);
        usage.setLot(consumedLot);
        usage.setQuantityUsed(1.0);
        usage.setUsageDate(new Timestamp(System.currentTimeMillis()));
        usage.setPerformedByUser(9);
        MicroInventoryUsageLink link = new MicroInventoryUsageLink();
        link.setId("micro-usage-1");
        link.setCaseId("case-1");
        link.setInventoryUsageId(31L);
        link.setUsageContext(MicroInventoryUsageContext.CULTURE_SETUP.name());
        link.setActivityId("activity-1");
        when(usageLinkDAO.getByCaseId("case-1")).thenReturn(List.of(link));
        when(inventoryUsageService.get(31L)).thenReturn(usage);

        List<MicroReagentUsageForm> history = service.getUsageHistory("case-1");

        assertEquals(1, history.size());
        assertEquals("MEDIA-CONSUMED", history.get(0).lotNumber);
        assertEquals(LotStatus.CONSUMED.name(), history.get(0).currentLotStatus);
        assertEquals("activity-1", history.get(0).actionId);
    }

    private InventoryLot lot(Long id, String number, Double quantity, Timestamp expiration) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        lot.setInventoryItem(reagent);
        lot.setLotNumber(number);
        lot.setCurrentQuantity(quantity);
        lot.setInitialQuantity(quantity);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setExpirationDate(expiration);
        return lot;
    }

    private Timestamp daysFromNow(int days) {
        return new Timestamp(System.currentTimeMillis() + days * 86_400_000L);
    }
}
