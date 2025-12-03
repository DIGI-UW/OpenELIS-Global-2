package org.openelisglobal.qc.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.service.evaluator.WestgardRuleEvaluator;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Westgard Rule Evaluation (T097)
 *
 * Orchestrates evaluation of all enabled Westgard rules against QC results.
 * Uses Spring's dependency injection to get all registered evaluators.
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@Service
public class WestgardRuleEvaluationServiceImpl implements WestgardRuleEvaluationService {

    // Maximum historical results to fetch for evaluation (10x rule needs 10)
    private static final int MAX_HISTORICAL_RESULTS = 15;

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private WestgardRuleConfigService ruleConfigService;

    @Autowired
    private List<WestgardRuleEvaluator> evaluators;

    @Override
    @Transactional(readOnly = true)
    public List<RuleEvaluationResult> evaluateAllRules(String resultId) {
        // Fetch the result
        QCResult currentResult = resultDAO.get(resultId).orElse(null);
        if (currentResult == null) {
            LogEvent.logWarn(this.getClass().getName(), "evaluateAllRules", "Result not found: " + resultId);
            return new ArrayList<>();
        }

        // Get historical results for multi-result rules
        List<QCResult> historicalResults = getHistoricalResults(currentResult.getControlLotId(), currentResult.getId(),
                MAX_HISTORICAL_RESULTS);

        return evaluateAllRules(currentResult, historicalResults, currentResult.getTestId(),
                currentResult.getInstrumentId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleEvaluationResult> evaluateAllRules(QCResult currentResult, List<QCResult> historicalResults,
            Integer testId, Integer instrumentId) {

        List<RuleEvaluationResult> results = new ArrayList<>();

        if (currentResult == null) {
            LogEvent.logWarn(this.getClass().getName(), "evaluateAllRules", "Current result is null");
            return results;
        }

        // Get enabled rule configurations
        List<WestgardRuleConfig> enabledRules = ruleConfigService.findEnabledByTestAndInstrument(testId, instrumentId);

        if (enabledRules.isEmpty()) {
            LogEvent.logInfo(this.getClass().getName(), "evaluateAllRules",
                    "No enabled rules for test " + testId + ", instrument " + instrumentId);
            return results;
        }

        // Get statistics for the control lot
        QCStatistics statistics = statisticsDAO.findLatestByControlLot(currentResult.getControlLotId());
        if (statistics == null) {
            LogEvent.logWarn(this.getClass().getName(), "evaluateAllRules",
                    "No statistics found for control lot: " + currentResult.getControlLotId());
            return results;
        }

        // Run each enabled evaluator
        for (WestgardRuleConfig config : enabledRules) {
            WestgardRuleEvaluator evaluator = findEvaluator(config.getRuleCode());
            if (evaluator != null && evaluator.canEvaluate(config)) {
                try {
                    RuleEvaluationResult result = evaluator.evaluate(currentResult, historicalResults, statistics);
                    results.add(result);

                    if (result.isViolated()) {
                        LogEvent.logInfo(this.getClass().getName(), "evaluateAllRules",
                                "Violation detected: " + result.getRuleCode() + " - " + result.getMessage());
                    }
                } catch (Exception e) {
                    LogEvent.logError(this.getClass().getName(), "evaluateAllRules",
                            "Error evaluating rule " + config.getRuleCode() + ": " + e.getMessage());
                    results.add(RuleEvaluationResult.cannotEvaluate(config.getRuleCode(),
                            "Evaluation error: " + e.getMessage()));
                }
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleEvaluationResult> getViolations(String resultId) {
        return evaluateAllRules(resultId).stream().filter(RuleEvaluationResult::isViolated)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRejectionViolation(String resultId) {
        return evaluateAllRules(resultId).stream().anyMatch(r -> r.isViolated() && "REJECTION".equals(r.getSeverity()));
    }

    /**
     * Find the evaluator for a specific rule code.
     */
    private WestgardRuleEvaluator findEvaluator(String ruleCode) {
        return evaluators.stream().filter(e -> e.getRuleCode().equals(ruleCode)).findFirst().orElse(null);
    }

    /**
     * Get historical results for a control lot (excluding current result).
     */
    private List<QCResult> getHistoricalResults(String controlLotId, String excludeResultId, int limit) {
        List<QCResult> allResults = resultDAO.findByControlLotIdOrderByRunDateTime(controlLotId);

        return allResults.stream().filter(r -> !r.getId().equals(excludeResultId))
                .sorted(Comparator.comparing(QCResult::getRunDateTime)).limit(limit).collect(Collectors.toList());
    }
}
