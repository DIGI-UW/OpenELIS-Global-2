package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.service.InventoryUsageService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroInventoryUsageLinkDAO;
import org.openelisglobal.microbiology.form.MicroReagentLotForm;
import org.openelisglobal.microbiology.form.MicroReagentRequirementForm;
import org.openelisglobal.microbiology.form.MicroReagentUsageForm;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageContext;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageLink;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroReagentLotServiceImpl implements MicroReagentLotService {

    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final AnalysisService analysisService;
    private final TestReagentLinkService reagentLinkService;
    private final InventoryItemService inventoryItemService;
    private final InventoryLotService inventoryLotService;
    private final InventoryManagementService inventoryManagementService;
    private final InventoryUsageService inventoryUsageService;
    private final MicroInventoryUsageLinkDAO usageLinkDAO;

    public MicroReagentLotServiceImpl(MicroCaseAnalysisDAO caseAnalysisDAO, AnalysisService analysisService,
            TestReagentLinkService reagentLinkService, InventoryItemService inventoryItemService,
            InventoryLotService inventoryLotService, InventoryManagementService inventoryManagementService,
            InventoryUsageService inventoryUsageService, MicroInventoryUsageLinkDAO usageLinkDAO) {
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.analysisService = analysisService;
        this.reagentLinkService = reagentLinkService;
        this.inventoryItemService = inventoryItemService;
        this.inventoryLotService = inventoryLotService;
        this.inventoryManagementService = inventoryManagementService;
        this.inventoryUsageService = inventoryUsageService;
        this.usageLinkDAO = usageLinkDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReagentRequirementForm> getRequirements(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        List<MicroReagentRequirementForm> requirements = new ArrayList<>();
        for (MicroCaseAnalysis caseAnalysis : caseAnalysisDAO.getByCaseId(caseId)) {
            Analysis analysis = requireAnalysis(caseAnalysis.getAnalysisId());
            Test test = requireTest(analysis);
            for (TestReagentLink link : reagentLinkService.getByTestId(test.getId())) {
                requirements.add(toRequirement(caseAnalysis, test, link));
            }
        }
        return requirements;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroReagentUsageForm> getUsageHistory(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        List<MicroReagentUsageForm> history = new ArrayList<>();
        for (MicroInventoryUsageLink link : usageLinkDAO.getByCaseId(caseId)) {
            InventoryUsage usage = inventoryUsageService.get(link.getInventoryUsageId());
            if (usage != null) {
                history.add(toUsageForm(link, usage));
            }
        }
        return history;
    }

    @Override
    @Transactional
    public void recordSelections(String caseId, MicroInventoryUsageContext context, String actionId,
            List<MicroLotSelection> selections, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCaseServiceImpl.requireText(actionId, "actionId");
        MicroCaseServiceImpl.requireText(performedBy, "performedBy");
        if (context == null) {
            throw new IllegalArgumentException("usageContext is required");
        }
        if (selections == null || selections.isEmpty()) {
            return;
        }

        Set<String> selectedLinks = new HashSet<>();
        for (MicroLotSelection selection : selections) {
            String selectionKey = selection.analysisId() + ":" + selection.testReagentLinkId();
            if (!selectedLinks.add(selectionKey)) {
                throw new IllegalArgumentException("MICROBIOLOGY_DUPLICATE_LOT_SELECTION");
            }
            recordSelection(caseId, context, actionId, selection, performedBy);
        }
    }

    private void recordSelection(String caseId, MicroInventoryUsageContext context, String actionId,
            MicroLotSelection selection, String performedBy) {
        if (selection == null || selection.lotId() == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_LOT_SELECTION_INCOMPLETE");
        }
        MicroCaseAnalysis caseAnalysis = caseAnalysisDAO.getByCaseAndAnalysis(caseId, selection.analysisId());
        if (caseAnalysis == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_ANALYSIS_CASE_MISMATCH");
        }
        Analysis analysis = requireAnalysis(caseAnalysis.getAnalysisId());
        Test test = requireTest(analysis);
        TestReagentLink reagentLink = reagentLinkService.get(selection.testReagentLinkId());
        if (reagentLink == null || !test.getId().equals(reagentLink.getTestId())) {
            throw new IllegalArgumentException("MICROBIOLOGY_REAGENT_LINK_MISMATCH");
        }
        InventoryLot lot = inventoryLotService.get(selection.lotId());
        if (lot == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_LOT_NOT_FOUND");
        }
        if (lot.getInventoryItem() == null || !reagentLink.getReagentId().equals(lot.getInventoryItem().getId())) {
            throw new IllegalArgumentException("MICROBIOLOGY_LOT_REAGENT_MISMATCH");
        }

        double quantity = quantityFor(reagentLink);
        InventoryUsage usage = inventoryManagementService.consumeSelectedLot(lot.getId(), quantity, null,
                Long.valueOf(caseAnalysis.getAnalysisId()), performedBy);
        MicroInventoryUsageLink usageLink = new MicroInventoryUsageLink();
        usageLink.setCaseId(caseId);
        usageLink.setInventoryUsageId(usage.getId());
        usageLink.setUsageContext(context.name());
        usageLink.setTestReagentLinkId(reagentLink.getId());
        if (MicroInventoryUsageContext.CULTURE_SETUP.equals(context)) {
            usageLink.setActivityId(actionId);
        } else {
            usageLink.setAstRunId(actionId);
        }
        usageLinkDAO.insert(usageLink);
    }

    private MicroReagentRequirementForm toRequirement(MicroCaseAnalysis caseAnalysis, Test test, TestReagentLink link) {
        MicroReagentRequirementForm form = new MicroReagentRequirementForm();
        form.analysisId = caseAnalysis.getAnalysisId();
        form.testId = test.getId();
        form.testName = test.getName();
        form.linkId = link.getId();
        form.reagentId = link.getReagentId();
        form.usageType = link.getUsageType();
        form.quantityPerTest = link.getQuantityPerTest();
        form.quantityUnit = link.getQuantityUnit();
        InventoryItem item = inventoryItemService.get(link.getReagentId());
        form.reagentName = item == null ? String.valueOf(link.getReagentId()) : item.getName();

        List<InventoryLot> lots = new ArrayList<>(inventoryLotService.getByInventoryItemId(link.getReagentId()));
        lots.sort(Comparator
                .comparing(InventoryLot::getEffectiveExpirationDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InventoryLot::getId));
        boolean recommendedAssigned = false;
        for (InventoryLot lot : lots) {
            MicroReagentLotForm lotForm = toLotForm(lot, quantityFor(link));
            if (lotForm.available && !recommendedAssigned) {
                lotForm.fefoRecommended = true;
                recommendedAssigned = true;
            }
            form.lots.add(lotForm);
        }
        return form;
    }

    private MicroReagentLotForm toLotForm(InventoryLot lot, double quantityNeeded) {
        MicroReagentLotForm form = new MicroReagentLotForm();
        form.id = lot.getId();
        form.lotNumber = lot.getLotNumber();
        form.effectiveExpirationDate = lot.getEffectiveExpirationDate();
        form.openedDate = lot.getDateOpened();
        form.currentQuantity = lot.getCurrentQuantity();
        form.status = lot.getStatus() == null ? null : lot.getStatus().name();
        form.qcStatus = lot.getQcStatus() == null ? null : lot.getQcStatus().name();
        form.unavailableReason = unavailableReason(lot, quantityNeeded);
        form.available = form.unavailableReason == null;
        return form;
    }

    private MicroReagentUsageForm toUsageForm(MicroInventoryUsageLink link, InventoryUsage usage) {
        MicroReagentUsageForm form = new MicroReagentUsageForm();
        form.id = link.getId();
        form.usageContext = link.getUsageContext();
        form.actionId = link.getActivityId() == null ? link.getAstRunId() : link.getActivityId();
        form.quantityUsed = usage.getQuantityUsed();
        form.usageDate = usage.getUsageDate();
        form.performedByUser = usage.getPerformedByUser();
        if (usage.getInventoryItem() != null) {
            form.reagentName = usage.getInventoryItem().getName();
            form.quantityUnit = usage.getInventoryItem().getUnits();
        }
        if (usage.getLot() != null) {
            form.lotNumber = usage.getLot().getLotNumber();
            form.effectiveExpirationDate = usage.getLot().getEffectiveExpirationDate();
            form.currentLotStatus = usage.getLot().getStatus() == null ? null : usage.getLot().getStatus().name();
            form.currentQcStatus = usage.getLot().getQcStatus() == null ? null : usage.getLot().getQcStatus().name();
        }
        return form;
    }

    private String unavailableReason(InventoryLot lot, double quantityNeeded) {
        if (lot.isExpired()) {
            return "INVENTORY_LOT_EXPIRED";
        }
        if (lot.getQcStatus() == QCStatus.FAILED) {
            return "INVENTORY_LOT_QC_FAILED";
        }
        if (lot.getQcStatus() != QCStatus.PASSED) {
            return "INVENTORY_LOT_QC_NOT_PASSED";
        }
        if (lot.getStatus() != LotStatus.ACTIVE && lot.getStatus() != LotStatus.IN_USE) {
            String status = lot.getStatus() == null ? "STATUS_UNKNOWN" : lot.getStatus().name();
            return "INVENTORY_LOT_" + status;
        }
        if (lot.getCurrentQuantity() == null || lot.getCurrentQuantity() < quantityNeeded) {
            return "INVENTORY_LOT_INSUFFICIENT_QUANTITY";
        }
        return null;
    }

    private double quantityFor(TestReagentLink reagentLink) {
        BigDecimal configured = reagentLink.getQuantityPerTest();
        return configured == null || configured.signum() <= 0 ? 1.0 : configured.doubleValue();
    }

    private Analysis requireAnalysis(String analysisId) {
        Analysis analysis = analysisService.getAnalysisById(analysisId);
        if (analysis == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_ANALYSIS_NOT_FOUND");
        }
        return analysis;
    }

    private Test requireTest(Analysis analysis) {
        Test test = analysisService.getTest(analysis);
        if (test == null) {
            throw new IllegalArgumentException("MICROBIOLOGY_ANALYSIS_TEST_NOT_FOUND");
        }
        return test;
    }
}
