package org.openelisglobal.qc.service;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Centralized validator for QCControlLot domain invariants. Called from all
 * mutation paths (create, update, activate).
 *
 * Invariants enforced: 1. MANUFACTURER_FIXED requires both manufacturerMean and
 * manufacturerStdDev 2. INITIAL_RUNS requires a positive initialRunsCount 3.
 * ACTIVE status requires statistics to exist 4. a lot with no analyzer (bench)
 * must use MANUFACTURER_FIXED 5. (lotNumber, testId, controlLevel) is unique
 * among non-retired lots
 */
@Component
public class QCControlLotValidator {

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private QCControlLotDAO controlLotDAO;

    /**
     * Validate all control lot invariants.
     *
     * @throws IllegalArgumentException if any invariant is violated
     */
    public void validate(QCControlLot lot) {
        validateCalculationMethodConfig(lot);
        validateBenchLotUsesFixedTargets(lot);
        validateActiveRequiresStatistics(lot);
        validateNoDuplicateLot(lot);
    }

    /**
     * GAP-5: capture, statistics and charting all key on controlLotId, so two live
     * rows for "the same" lot silently split its statistics between them. The
     * uniqueness key is (lotNumber, testId, controlLevel): the same physical lot
     * number legitimately recurs across different tests, and EXPIRED (retired)
     * lots never block reuse. Mirrored by the partial unique index
     * uq_qc_control_lot_active (qc-028) per the inversion-test convention.
     */
    private void validateNoDuplicateLot(QCControlLot lot) {
        if (StringUtils.isBlank(lot.getLotNumber()) || StringUtils.isBlank(lot.getTestId())) {
            return;
        }
        List<QCControlLot> existing = controlLotDAO.getNonExpiredByLotTestAndLevel(lot.getLotNumber(), lot.getTestId(),
                lot.getControlLevel());
        for (QCControlLot other : existing) {
            if (!other.getId().equals(lot.getId())) {
                throw new IllegalArgumentException("A control lot with lot number '" + lot.getLotNumber()
                        + "' already exists for this test and control level (status " + other.getStatus()
                        + "). Use the existing lot, or retire it before creating a replacement.");
            }
        }
    }

    /**
     * A bench lot has no analyzer, so nothing accumulates runs on its behalf:
     * INITIAL_RUNS and ROLLING both wait for enough results to establish statistics
     * from, which is the 20-run establishment protocol OGC-1147 decision D3
     * rejected for manual methods. Configured either way the lot would sit in
     * ESTABLISHMENT forever and silently never plot, so refuse it at configuration
     * time instead.
     */
    private void validateBenchLotUsesFixedTargets(QCControlLot lot) {
        if (StringUtils.isNotBlank(lot.getInstrumentId())) {
            return;
        }
        if (!"MANUFACTURER_FIXED".equals(lot.getCalculationMethod())) {
            throw new IllegalArgumentException("A control lot with no analyzer must use the MANUFACTURER_FIXED"
                    + " calculation method: " + lot.getCalculationMethod() + " establishes statistics from accumulated"
                    + " runs, which a manual bench method never performs. Enter the manufacturer mean and standard"
                    + " deviation instead, or select an analyzer for this lot.");
        }
    }

    private void validateCalculationMethodConfig(QCControlLot lot) {
        String method = lot.getCalculationMethod();

        if ("MANUFACTURER_FIXED".equals(method)) {
            if (lot.getManufacturerMean() == null || lot.getManufacturerStdDev() == null) {
                throw new IllegalArgumentException(
                        "Manufacturer fixed method requires both mean and standard deviation");
            }
        }

        if ("INITIAL_RUNS".equals(method)) {
            Integer count = lot.getInitialRunsCount();
            if (count == null || count <= 0) {
                throw new IllegalArgumentException("Initial runs method requires a positive initial runs count");
            }
        }
    }

    private void validateActiveRequiresStatistics(QCControlLot lot) {
        if (!"ACTIVE".equals(lot.getStatus())) {
            return;
        }

        QCStatistics stats = statisticsDAO.findLatestByControlLot(lot.getId());
        if (stats == null) {
            throw new IllegalArgumentException("Cannot set control lot to ACTIVE: no statistics exist. "
                    + "Statistics must be computed before activation.");
        }
    }
}
