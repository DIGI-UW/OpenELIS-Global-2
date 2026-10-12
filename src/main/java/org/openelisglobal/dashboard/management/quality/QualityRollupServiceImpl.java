package org.openelisglobal.dashboard.management.quality;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.dao.ManagementDashboardDAO;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.dashboard.management.quality.QualityRollup.Component;
import org.openelisglobal.eqa.service.EQALabPerformanceService;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualityRollupServiceImpl implements QualityRollupService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Autowired
    private QiWeightsService qiWeightsService;

    @Autowired
    private KpiService kpiService;

    @Autowired
    private EQALabPerformanceService eqaLabPerformanceService;

    @Autowired
    private AmendmentReportService amendmentReportService;

    @Autowired
    private QualityDAO qualityDAO;

    @Autowired
    private ManagementDashboardDAO managementDashboardDAO;

    @Override
    @Transactional(readOnly = true)
    public QualityRollup getRollup(ScopeRange scope) {
        WeightsView weights = qiWeightsService.getWeights();
        QualityRollup prior = QualityRollup.of(components(scope.priorFrom(), scope.priorTo(), weights), null, scope);
        return QualityRollup.of(components(scope.from(), scope.to(), weights), prior.composite(), scope);
    }

    private List<Component> components(LocalDate from, LocalDate to, WeightsView weights) {
        Integer eqa = eqaLabPerformanceService.getAcceptanceRate(from, to);
        Double amendmentRate = amendmentReportService.getSummary(from, to).getRatePercent();
        return List.of(new Component("qcPass", kpiService.qcPassRate(from, to), weights.qc(), null),
                new Component("eqa", eqa == null ? null : BigDecimal.valueOf(eqa), weights.eqa(), null),
                new Component("nceInverse", nceInverse(from, to), weights.nceInverse(), null),
                new Component("amendmentInverse",
                        amendmentRate == null ? null : HUNDRED.subtract(BigDecimal.valueOf(amendmentRate)),
                        weights.amendmentInverse(), null));
    }

    /** 100 × (1 − NCEs reported / samples received), floored at 0. */
    private BigDecimal nceInverse(LocalDate from, LocalDate to) {
        long samples = managementDashboardDAO.countSamplesReceived(from, to);
        if (samples == 0) {
            return null;
        }
        BigDecimal ratio = BigDecimal.valueOf(qualityDAO.countNcesReported(from, to))
                .divide(BigDecimal.valueOf(samples), 4, RoundingMode.HALF_UP);
        return HUNDRED.multiply(BigDecimal.ONE.subtract(ratio)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
