package org.openelisglobal.qaevent.qiconfig.breach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackSummaryResponse;
import org.openelisglobal.qaevent.criticalcallback.service.CriticalCallbackService;
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
 * decision), CALLBACK as the month-to-date critical read-back compliance
 * percentage. NCE stays out — it is self-referential and ships without numeric
 * thresholds by design. CALLBACK is HIGHER_BETTER and the rest LOWER_BETTER;
 * the comparison goes through the config's direction either way.
 *
 * <p>
 * Each run evaluates two windows: the current month-to-date AND the just-closed
 * month. The second matters because the computes attribute events to the month
 * the work arrived (cohort semantics — e.g. a rejection noted July 1 on an
 * analysis started June 30 belongs to June), so late-landing events would
 * otherwise fall into a month that is never looked at again. The dedup key
 * embeds the month, so a breach fires at most one NCE per indicator per month
 * and the prior-month pass is a no-op once its NCE exists. (Events landing two
 * or more months after intake are still missed — widen to N months if that ever
 * matters.)
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
    private CriticalCallbackService criticalCallbackService;
    @Autowired
    private QiBreachNceService qiBreachNceService;

    // Fixed-rate poller, same shape as the FHIR task poller
    // (FhirApiWorkFlowServiceImpl): first run 10s after startup, then every 2
    // minutes, overridable via org.openelisglobal.qi.breach.poll.frequency (ms).
    // Frequent runs are harmless — the per-month dedup in QiBreachNceService
    // means a breach creates its NCE once and every later pass is a no-op read.
    // Each indicator is evaluated independently: one indicator's failure (e.g.
    // an absent config row or a compute error) is logged and must not suppress
    // the others.
    @Override
    @Scheduled(initialDelay = 10 * 1000, fixedRateString = "${org.openelisglobal.qi.breach.poll.frequency:120000}")
    public void evaluateBreaches() {
        LocalDate today = LocalDate.now();
        evaluateWindow(today.withDayOfMonth(1), today);
        // just-closed month: catches events that land after the month ends but
        // belong to its cohort (see class javadoc)
        LocalDate prevFirst = today.withDayOfMonth(1).minusMonths(1);
        evaluateWindow(prevFirst, today.withDayOfMonth(1).minusDays(1));
    }

    private void evaluateWindow(LocalDate from, LocalDate to) {
        String periodKey = from.format(PERIOD_KEY);
        evaluateSafely("AMENDMENT", () -> evaluateAmendment(from, to, periodKey));
        evaluateSafely("REJECTION", () -> evaluateRejection(from, to, periodKey));
        evaluateSafely("TAT", () -> evaluateTat(from, to, periodKey));
        evaluateSafely("CALLBACK", () -> evaluateCallback(from, to, periodKey));
    }

    private void evaluateSafely(String indicator, Runnable evaluation) {
        try {
            evaluation.run();
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "evaluateBreaches",
                    indicator + " breach evaluation failed: " + e.getMessage());
        }
    }

    private void evaluateAmendment(LocalDate from, LocalDate to, String periodKey) {
        ResolvedConfig config = resolveActionable(QiIndicator.AMENDMENT);
        if (config == null) {
            return;
        }
        AmendmentSummaryResponse summary = amendmentReportService.getSummary(from, to);
        if (summary.getRatePercent() == null) {
            return; // nothing released this period
        }
        checkAndFire(QiIndicator.AMENDMENT, BigDecimal.valueOf(summary.getRatePercent()), config, "%", periodKey);
    }

    private void evaluateRejection(LocalDate from, LocalDate to, String periodKey) {
        ResolvedConfig config = resolveActionable(QiIndicator.REJECTION);
        if (config == null) {
            return;
        }
        RejectionSummaryResponse summary = rejectionReportService.getSummary(from, to);
        if (summary.getRatePercent() == null) {
            return; // nothing started this period
        }
        checkAndFire(QiIndicator.REJECTION, BigDecimal.valueOf(summary.getRatePercent()), config, "%", periodKey);
    }

    private void evaluateTat(LocalDate from, LocalDate to, String periodKey) {
        ResolvedConfig config = resolveActionable(QiIndicator.TAT);
        if (config == null) {
            return;
        }
        // Same segment/mode as the QI dashboard tile, so the auto-NCE and the
        // tile a lab manager checks against it agree.
        TATSummaryResponse summary = tatReportService.getSummary(from, to, TATSegment.RECEIPT_TO_VALIDATION,
                TATCalculationMode.CALENDAR, null, null, null, null, null, null, false, null);
        if (summary == null || summary.getMean() == null) {
            return; // nothing validated this period
        }
        checkAndFire(QiIndicator.TAT, summary.getMean(), config, "h", periodKey);
    }

    private void evaluateCallback(LocalDate from, LocalDate to, String periodKey) {
        ResolvedConfig config = resolveActionable(QiIndicator.CALLBACK);
        if (config == null) {
            return;
        }
        // Opt-in indicator: getSummary re-checks enabled and short-circuits, so a
        // lab that never turns CALLBACK on pays nothing here either.
        CallbackSummaryResponse summary = criticalCallbackService.getSummary(from, to);
        if (summary.getCompliancePercent() == null) {
            return; // no critical results released this period
        }
        checkAndFire(QiIndicator.CALLBACK, BigDecimal.valueOf(summary.getCompliancePercent()), config, "%", periodKey);
    }

    /** Resolved config, or null when disabled / no action threshold set. */
    private ResolvedConfig resolveActionable(QiIndicator indicator) {
        ResolvedConfig config = qiConfigService.resolve(indicator.name(), null);
        if (!config.isEnabled() || config.getAction() == null) {
            return null;
        }
        return config;
    }

    private void checkAndFire(QiIndicator indicator, BigDecimal actual, ResolvedConfig config, String unit,
            String periodKey) {
        if (breaches(actual, config.getAction(), config.getDirection())) {
            qiBreachNceService.createBreachNce(indicator.name(), periodKey, actual, config.getAction(),
                    config.getDirection(), unit);
            LogEvent.logInfo(this.getClass().getSimpleName(), "checkAndFire", indicator.name() + " breach (" + periodKey
                    + "): " + actual + unit + " vs action " + config.getAction() + unit);
        }
    }

    // LOWER_BETTER breaches when the value rises above the action threshold;
    // HIGHER_BETTER when it drops below.
    private boolean breaches(BigDecimal actual, BigDecimal action, String direction) {
        return LOWER_BETTER.equals(direction) ? actual.compareTo(action) > 0 : actual.compareTo(action) < 0;
    }
}
