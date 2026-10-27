package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstQcLotDAO;
import org.openelisglobal.microbiology.dao.MicroAstQcResultDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroInventoryUsageLinkDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroAstQcRequestForm;
import org.openelisglobal.microbiology.form.MicroAstQcResultForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstQcLot;
import org.openelisglobal.microbiology.valueholder.MicroAstQcResult;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroQcRange;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.service.QcHoldService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroAstQcService {
    public static final String AST_QC_TEST_SETTING = "microbiologyAstQcTest";

    private final MicroAstRunDAO runs;
    private final MicroIsolateDAO isolates;
    private final SiteInformationService siteInformation;
    private final QCControlLotService controlLots;
    private final MicroAstRunAntibioticDAO runAntibiotics;
    private final MicroAntibioticDAO antibiotics;
    private final MicroQcRangeService qcRanges;
    private final QCResultService qcResults;
    private final MicroAstQcResultDAO astQcResults;
    private final MicroAstQcLotDAO astQcLots;
    private final MicroInventoryUsageLinkDAO usageLinks;
    private final InventoryLotService inventoryLots;
    private final NCEventService ncEvents;
    private final QcHoldService qcHold;
    private final MicroCaseAuthorizationService authorization;

    public MicroAstQcService(MicroAstRunDAO runs, MicroIsolateDAO isolates, SiteInformationService siteInformation,
            QCControlLotService controlLots, MicroAstRunAntibioticDAO runAntibiotics, MicroAntibioticDAO antibiotics,
            MicroQcRangeService qcRanges, QCResultService qcResults, MicroAstQcResultDAO astQcResults,
            MicroAstQcLotDAO astQcLots, MicroInventoryUsageLinkDAO usageLinks, InventoryLotService inventoryLots,
            NCEventService ncEvents, QcHoldService qcHold, MicroCaseAuthorizationService authorization) {
        this.runs = runs;
        this.isolates = isolates;
        this.siteInformation = siteInformation;
        this.controlLots = controlLots;
        this.runAntibiotics = runAntibiotics;
        this.antibiotics = antibiotics;
        this.qcRanges = qcRanges;
        this.qcResults = qcResults;
        this.astQcResults = astQcResults;
        this.astQcLots = astQcLots;
        this.usageLinks = usageLinks;
        this.inventoryLots = inventoryLots;
        this.ncEvents = ncEvents;
        this.qcHold = qcHold;
        this.authorization = authorization;
    }

    @Transactional
    public MicroAstQcResultForm recordControl(String runId, MicroAstQcRequestForm request, String actor) {
        MicroAstRun run = runs.get(runId).orElseThrow(() -> new IllegalArgumentException("AST run not found"));
        MicroIsolate isolate = isolates.get(run.getIsolateId())
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        authorization.requireWrite(isolate.getCaseId(), actor, Constants.ROLE_RESULTS);
        if (request == null || request.controlLotId == null || request.controlLotId.isBlank()) {
            throw new IllegalArgumentException("AST control lot is required");
        }

        SiteInformation setting = siteInformation.getSiteInformationByName(AST_QC_TEST_SETTING);
        if (setting == null || setting.getValue() == null || setting.getValue().isBlank()) {
            throw new MicroAstConflictException("AST_QC_TEST_NOT_CONFIGURED");
        }
        String qcTestId = setting.getValue().trim();
        Test qcTest;
        try {
            qcTest = SpringContext.getBean(TestService.class).get(qcTestId);
        } catch (ObjectNotFoundException e) {
            throw new MicroAstConflictException("AST_QC_TEST_NOT_CONFIGURED");
        }
        if (qcTest == null || qcTest.getTestSection() == null) {
            throw new MicroAstConflictException("AST_QC_TEST_NOT_CONFIGURED");
        }
        String testSectionId = qcTest.getTestSection().getId();

        QCControlLot controlLot;
        try {
            controlLot = controlLots.get(request.controlLotId);
        } catch (ObjectNotFoundException e) {
            throw new IllegalArgumentException("AST control lot not found");
        }
        if (controlLot == null || !qcTestId.equals(controlLot.getTestId())) {
            throw new IllegalArgumentException("AST control lot does not match the QC test");
        }
        String strain = controlLot.getProductName();

        List<MicroAstRunAntibiotic> ordered = runAntibiotics.getByRunId(runId);
        Map<String, BigDecimal> measured = new HashMap<>();
        if (request.results != null) {
            for (MicroAstQcRequestForm.Measurement measurement : request.results) {
                if (measurement == null || measurement.antibioticId == null || measurement.antibioticId.isBlank()
                        || measurement.measuredValue == null) {
                    throw new IllegalArgumentException("AST QC needs one value per antibiotic");
                }
                if (measured.put(measurement.antibioticId, measurement.measuredValue) != null) {
                    throw new IllegalArgumentException("AST QC needs one value per antibiotic");
                }
            }
        }
        Set<String> orderedIds = new HashSet<>();
        for (MicroAstRunAntibiotic runAntibiotic : ordered) {
            orderedIds.add(runAntibiotic.getAntibioticId());
        }
        if (!orderedIds.equals(measured.keySet())) {
            throw new IllegalArgumentException("AST QC needs one value per antibiotic");
        }

        MicroAstMethod method = MicroAstTechnique.valueOf(run.getTechnique()).measurementType();
        String standardId = run.getBreakpointStandardId();
        List<PlannedRow> planned = new ArrayList<>();
        for (MicroAstRunAntibiotic runAntibiotic : ordered) {
            Optional<MicroQcRange> range = qcRanges.find(strain, runAntibiotic.getAntibioticId(), method,
                    runAntibiotic.getDiskContent(), standardId);
            if (range.isEmpty()) {
                throw new MicroAstConflictException("AST_QC_RANGE_MISSING");
            }
            planned.add(new PlannedRow(runAntibiotic.getAntibioticId(), measured.get(runAntibiotic.getAntibioticId()),
                    range.get()));
        }

        String controlId = UUID.randomUUID().toString();
        List<MicroAstQcResultForm.Row> rows = new ArrayList<>();
        boolean passed = true;
        int sysUserId = Integer.parseInt(actor);
        LocalDateTime runDateTime = LocalDateTime.now();
        for (PlannedRow row : planned) {
            MicroQcRange range = row.range();
            BigDecimal value = row.measuredValue();
            boolean rowPassed = value.compareTo(range.getLow()) >= 0 && value.compareTo(range.getHigh()) <= 0;
            QCQualitativeOutcome outcome = rowPassed ? QCQualitativeOutcome.PASS : QCQualitativeOutcome.FAIL;
            String displayName = antibiotics.get(row.antibioticId()).map(MicroAntibiotic::getDisplayName)
                    .orElse(row.antibioticId());
            BigDecimal expectedValue = range.getLow().add(range.getHigh()).divide(BigDecimal.valueOf(2));
            BigDecimal uncertainty = range.getHigh().subtract(range.getLow()).divide(BigDecimal.valueOf(2));
            QCResult qcResult = qcResults
                    .createBenchQCResult(new BenchQCCaptureForm(QCSource.MANUAL, qcTestId, testSectionId,
                            request.controlLotId, displayName, value, method == MicroAstMethod.MIC ? "ug/mL" : "mm",
                            outcome, expectedValue, uncertainty, runDateTime, "AST run " + runId), sysUserId);
            MicroAstQcResult stored = new MicroAstQcResult();
            stored.setControlId(controlId);
            stored.setAstRunId(runId);
            stored.setAntibioticId(row.antibioticId());
            stored.setQcResultId(qcResult.getId());
            astQcResults.insert(stored);

            MicroAstQcResultForm.Row resultRow = new MicroAstQcResultForm.Row();
            resultRow.antibioticId = row.antibioticId();
            resultRow.low = range.getLow();
            resultRow.high = range.getHigh();
            resultRow.measuredValue = value;
            resultRow.outcome = outcome.name();
            resultRow.qcResultId = qcResult.getId();
            rows.add(resultRow);
            passed = passed && rowPassed;
        }

        List<Long> lotIds = request.lotIds == null ? List.of() : request.lotIds;
        for (Long lotId : lotIds) {
            MicroAstQcLot astQcLot = new MicroAstQcLot();
            astQcLot.setControlId(controlId);
            astQcLot.setInventoryLotId(lotId);
            astQcLots.insert(astQcLot);
        }
        if (passed) {
            for (Long lotId : lotIds) {
                InventoryLot lot = inventoryLots.get(lotId);
                if (lot != null && QCStatus.PENDING.equals(lot.getQcStatus())) {
                    inventoryLots.updateQCStatus(lotId, QCStatus.PASSED, "AST control passed", actor);
                }
            }
        }

        MicroAstQcResultForm form = new MicroAstQcResultForm();
        form.passed = passed;
        form.rows = rows;
        return form;
    }

    public boolean blocksValidation(String runId) {
        return qcHold.blocksRelease() && isHeld(runId);
    }

    public boolean isHeld(String runId) {
        Set<String> controlIds = new HashSet<>();
        for (MicroAstQcResult result : astQcResults.getByRunId(runId)) {
            controlIds.add(result.getControlId());
        }
        controlIds.addAll(astQcLots.getControlIdsByLotIds(usageLinks.getAstRunLotIds(runId)));
        if (controlIds.isEmpty()) {
            return false;
        }
        for (MicroAstQcResult result : astQcResults.getByControlIds(controlIds)) {
            QCResult qcResult = qcResults.get(result.getQcResultId());
            if (qcResult == null || !"REJECTED".equals(qcResult.getResultStatus())) {
                continue;
            }
            Optional<QCRuleViolation> violation = astQcResults.getViolationByTriggeringResultId(result.getQcResultId());
            if (violation.isEmpty()) {
                return true;
            }
            NcEvent nce = ncEvents.findByTriggerSource("QC_VIOLATION", violation.get().getId());
            if (nce == null || (!"Closed".equals(nce.getStatus()) && !"Completed".equals(nce.getStatus()))) {
                return true;
            }
        }
        return false;
    }

    private record PlannedRow(String antibioticId, BigDecimal measuredValue, MicroQcRange range) {
    }
}
