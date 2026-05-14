package org.openelisglobal.qc.service;

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
 * ACTIVE status requires statistics to exist
 */
@Component
public class QCControlLotValidator {

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    /**
     * Validate all control lot invariants.
     *
     * @throws IllegalArgumentException if any invariant is violated
     */
    public void validate(QCControlLot lot) {
        validateCalculationMethodConfig(lot);
        validateActiveRequiresStatistics(lot);
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
