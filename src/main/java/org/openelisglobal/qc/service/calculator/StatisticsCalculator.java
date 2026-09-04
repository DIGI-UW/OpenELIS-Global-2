package org.openelisglobal.qc.service.calculator;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Strategy interface for calculating QC statistics using different methods.
 * Implementations: InitialRunsCalculator, RollingCalculator,
 * ManufacturerFixedCalculator
 */
public interface StatisticsCalculator {

    /**
     * Check if this calculator supports the given calculation method.
     *
     * @param calculationMethod The method from QCControlLot (INITIAL_RUNS, ROLLING,
     *                          MANUFACTURER_FIXED)
     * @return true if this calculator handles the method
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean supports(String calculationMethod);

    /**
     * Calculate statistics for the control lot based on historical results.
     *
     * @param controlLot The control lot to calculate statistics for
     * @param results    Historical QC results for this lot (may be ordered by date)
     * @return Calculated statistics entity, or null if insufficient data
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    QCStatistics calculate(QCControlLot controlLot, List<QCResult> results);
}
