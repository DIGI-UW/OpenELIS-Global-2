package org.openelisglobal.qaevent.qiconfig.breach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.service.QiConfigService;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiIndicator;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * OGC-712 — every threshold-bearing indicator is evaluated: AMENDMENT and
 * REJECTION as month-to-date rate percentages, TAT as the month-to-date mean
 * receipt→validation duration in hours (matching its qi_config unit; C.3 gap #3
 * decision). NCE stays out — it is self-referential and ships without numeric
 * thresholds by design. All three are LOWER_BETTER, but the comparison goes
 * through the config's direction so a future flip is config-driven.
 *
 * <p>
 * Window and dedup key are the current calendar month, so a breach fires at
 * most one NCE per indicator per month; a re-breach next month re-fires under a
 * new period key (the trigger-source unique constraint is all-time).
 */
@Service
public class QiBreachEvaluatorServiceImpl implements QiBreachEvaluatorService {

    private static final DateTimeFormatter PERIOD_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String LOWER_BETTER = "LOWER_BETTER";

    @Autowired
    private QiConfigService qiConfigService;
    @Autowired
    private AmendmentReportService amendmentReportService;
    @Autowired
    private RejectionReportService rejectionReportService;
    @Autowired
    private TATReportService tatReportService;
    @Autowired
    private QiBreachNceService qiBreachNceService;

    // Daily at 01:00 — QI rates move slowly, so once a day is ample; the per-month
    // dedup in QiBreachNceService makes extra runs harmless. Each indicator is
    // evaluated independently: one indicator's failure (e.g. an absent config
    // row or a compute error) is logged and must not suppress the others.
    @Override
    @Scheduled(cron = "0 0 1 * * *")
    public void evaluateBreaches() {
        evaluateSafely("AMENDMENT", this::evaluateAmendment);
        evaluateSafely("REJECTION", this::evaluateRejection);
        evaluateSafely("TAT", this::evaluateTat);
    }

    private void evaluateSafely(String indicator, Runnable evaluation) {
        try {
            evaluation.run();
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "evaluateBreaches",
                    indicator + " breach evaluation failed: " + e.getMessage());
        }
    }

    private void evaluateAmendment() {
        ResolvedConfig config = resolveActionable(QiIndicator.AMENDMENT);
        if (config == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        AmendmentSummaryResponse summary = amendmentReportService.getSummary(today.withDayOfMonth(1), today);
        if (summary.getRatePercent() == null) {
            return; // nothing released this period
        }
        checkAndFire(QiIndicator.AMENDMENT, BigDecimal.valueOf(summary.getRatePercent()), config, "%");
    }

    private void evaluateRejection() {
        ResolvedConfig config = resolveActionable(QiIndicator.REJECTION);
        if (config == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        RejectionSummaryResponse summary = rejectionReportService.getSummary(today.withDayOfMonth(1), today);
        if (summary.getRatePercent() == null) {
            return; // nothing started this period
        }
        checkAndFire(QiIndicator.REJECTION, BigDecimal.valueOf(summary.getRatePercent()), config, "%");
    }

    private void evaluateTat() {
        ResolvedConfig config = resolveActionable(QiIndicator.TAT);
        if (config == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        // Same segment/mode as the QI dashboard tile, so the auto-NCE and the
        // tile a lab manager checks against it agree.
        TATSummaryResponse summary = tatReportService.getSummary(today.withDayOfMonth(1), today,
                TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null, null, null, null, null,
                false, null);
        if (summary == null || summary.getMean() == null) {
            return; // nothing validated this period
        }
        checkAndFire(QiIndicator.TAT, summary.getMean(), config, "h");
    }

    /** Resolved config, or null when disabled / no action threshold set. */
    private ResolvedConfig resolveActionable(QiIndicator indicator) {
        ResolvedConfig config = qiConfigService.resolve(indicator.name(), null);
        if (!config.isEnabled() || config.getAction() == null) {
            return null;
        }
        return config;
    }

    private void checkAndFire(QiIndicator indicator, BigDecimal actual, ResolvedConfig config, String unit) {
        if (breaches(actual, config.getAction(), config.getDirection())) {
            LocalDate today = LocalDate.now();
            qiBreachNceService.createBreachNce(indicator.name(), today.format(PERIOD_KEY), actual, config.getAction(),
                    config.getDirection(), unit);
            LogEvent.logInfo(this.getClass().getSimpleName(), "checkAndFire",
                    indicator.name() + " breach: " + actual + unit + " vs action " + config.getAction() + unit);
        }
    }

    // LOWER_BETTER breaches when the value rises above the action threshold;
    // HIGHER_BETTER when it drops below.
    private boolean breaches(BigDecimal actual, BigDecimal action, String direction) {
        return LOWER_BETTER.equals(direction) ? actual.compareTo(action) > 0 : actual.compareTo(action) < 0;
    }
}
