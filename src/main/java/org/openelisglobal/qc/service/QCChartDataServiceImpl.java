package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QCChartDataServiceImpl implements QCChartDataService {

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCStatisticsService statisticsService;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private QCControlLotService controlLotService;

    @Autowired
    private TestService testService;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getResultsByControlLotAndDateRange(String controlLotId, Timestamp startDate,
            Timestamp endDate) {
        return resultDAO.findByControlLotAndDateRange(controlLotId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> getViolationsForResults(List<String> resultIds) {
        List<QCRuleViolation> violations = new ArrayList<>();
        for (String resultId : resultIds) {
            violations.addAll(violationDAO.findByTriggeringResultId(resultId));
        }
        return violations;
    }

    @Override
    @Transactional(readOnly = true)
    public QCStatistics getLatestStatistics(String controlLotId) {
        return statisticsService.getLatestStatistics(controlLotId);
    }

    @Override
    @Transactional(readOnly = true)
    public StatsWithSigma getStatisticsWithSigma(String controlLotId) {
        QCStatistics stats = statisticsService.getLatestStatistics(controlLotId);
        if (stats == null) {
            return null;
        }
        return new StatsWithSigma(stats, computeSigmaForLot(controlLotService.get(controlLotId), stats));
    }

    @Override
    @Transactional(readOnly = true)
    public QCExportModel getExportModel(String instrumentId, String testId, String controlLevel, Timestamp start,
            Timestamp end, int maxRows) {
        String instrumentName = analyzerService.getWithType(instrumentId).map(Analyzer::getName).orElse(instrumentId);

        // Scope to ACTIVE lots, matching the on-screen QC chart/dashboard workflow
        // (getActiveControlLots*). An ACTIVE lot is guaranteed to have a
        // qc_statistics row, so its exported chart carries the same z-scores and
        // control limits the dashboard shows. Lots with no runs in the window are
        // dropped below.
        List<QCControlLot> lots = (testId != null && !testId.isBlank())
                ? controlLotService.getActiveControlLots(testId, instrumentId)
                : controlLotService.getActiveControlLotsByInstrument(instrumentId);
        if (controlLevel != null && !controlLevel.isBlank()) {
            lots = lots.stream().filter(l -> controlLevel.equalsIgnoreCase(l.getControlLevel())).toList();
        }

        List<LotSection> sections = new ArrayList<>();
        int totalRuns = 0;
        int totalViolations = 0;
        boolean truncated = false;

        for (QCControlLot lot : lots) {
            List<QCResult> results = getResultsByControlLotAndDateRange(lot.getId(), start, end);
            if (results.isEmpty()) {
                continue;
            }
            // Cap total run rows across lots (CSV bound; §01 #4). The PDF is scope-
            // bounded so this rarely triggers there.
            if (totalRuns + results.size() > maxRows) {
                results = new ArrayList<>(results.subList(0, Math.max(0, maxRows - totalRuns)));
                truncated = true;
            }
            List<String> resultIds = results.stream().map(QCResult::getId).toList();
            List<QCRuleViolation> violations = getViolationsForResults(resultIds);
            QCStatistics stats = statisticsService.getLatestStatistics(lot.getId());
            SigmaMetrics.SigmaResult sigma = computeSigmaForLot(lot, stats);
            sections.add(new LotSection(lot, resolveTestName(lot.getTestId()), results, violations, stats, sigma));
            totalRuns += results.size();
            totalViolations += violations.size();
            if (truncated) {
                break;
            }
        }
        return new QCExportModel(instrumentName, sections, totalRuns, totalViolations, truncated);
    }

    /**
     * The OGC-704 sigma wiring, shared by {@link #getStatisticsWithSigma} and the
     * export: resolve the lot's test TEa, then derive sigma from the control
     * mean/SD. Null lot or unset TEa yields NOT_CALCULABLE (handled by
     * {@link SigmaMetrics#compute}).
     */
    private SigmaMetrics.SigmaResult computeSigmaForLot(QCControlLot lot, QCStatistics stats) {
        Double tea = null;
        if (lot != null && lot.getTestId() != null) {
            Test test = testService.getTestById(lot.getTestId());
            if (test != null) {
                tea = test.getTea();
            }
        }
        return SigmaMetrics.compute(stats == null ? null : stats.getMean(),
                stats == null ? null : stats.getStandardDeviation(), tea);
    }

    private String resolveTestName(String testId) {
        if (testId == null) {
            return "";
        }
        Test test = testService.getTestById(testId);
        if (test == null) {
            return testId;
        }
        String name = test.getLocalizedName();
        return (name == null || name.isBlank()) ? testId : name;
    }
}
