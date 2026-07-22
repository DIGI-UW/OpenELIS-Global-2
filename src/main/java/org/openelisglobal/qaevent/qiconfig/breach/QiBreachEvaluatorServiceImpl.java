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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * OGC-712 — v1 covers AMENDMENT only: it is the sole QI indicator whose
 * server-side metric (rate %) matches the qi_config 0–100 threshold model. TAT
 * exposes only a mean duration (no compliance %), REJECTION has no compute yet
 * (OGC-710), and NCE is self-referential — each is excluded until it gains a
 * metric comparable to its configured threshold.
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
    private QiBreachNceService qiBreachNceService;

    // Daily at 01:00 — QI rates move slowly, so once a day is ample; the per-month
    // dedup in QiBreachNceService makes extra runs harmless. A throwing run is
    // logged and suppressed by Spring's scheduled-task error handler; the next
    // day's run continues regardless.
    @Override
    @Scheduled(cron = "0 0 1 * * *")
    public void evaluateBreaches() {
        evaluateAmendment();
    }

    private void evaluateAmendment() {
        ResolvedConfig config = qiConfigService.resolve(QiIndicator.AMENDMENT.name(), null);
        if (!config.isEnabled() || config.getAction() == null) {
            return; // disabled or no action threshold configured — nothing to check
        }
        LocalDate today = LocalDate.now();
        AmendmentSummaryResponse summary = amendmentReportService.getSummary(today.withDayOfMonth(1), today);
        if (summary.getRatePercent() == null) {
            return; // nothing released this period
        }
        BigDecimal actual = BigDecimal.valueOf(summary.getRatePercent());
        if (breaches(actual, config.getAction(), config.getDirection())) {
            qiBreachNceService.createBreachNce(QiIndicator.AMENDMENT.name(), today.format(PERIOD_KEY), actual,
                    config.getAction(), config.getDirection());
            LogEvent.logInfo(this.getClass().getSimpleName(), "evaluateAmendment",
                    "AMENDMENT breach: " + actual + "% vs action " + config.getAction() + "%");
        }
    }

    // LOWER_BETTER breaches when the value rises above the action threshold;
    // HIGHER_BETTER when it drops below.
    private boolean breaches(BigDecimal actual, BigDecimal action, String direction) {
        return LOWER_BETTER.equals(direction) ? actual.compareTo(action) > 0 : actual.compareTo(action) < 0;
    }
}
