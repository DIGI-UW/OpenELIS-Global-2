package org.openelisglobal.qc.service.calculator;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Calculator for manufacturer fixed values method. Uses pre-defined mean and
 * standard deviation from manufacturer (no calculation). Per US6: Should be
 * immediately ACTIVE and ready for evaluation.
 */
@Component
public class ManufacturerFixedCalculator implements StatisticsCalculator {

    @Override
    public boolean supports(String calculationMethod) {
        return "MANUFACTURER_FIXED".equals(calculationMethod);
    }

    @Override
    public QCStatistics calculate(QCControlLot controlLot, List<QCResult> results) {
        // Validate manufacturer values are present
        if (controlLot.getManufacturerMean() == null || controlLot.getManufacturerStdDev() == null) {
            throw new IllegalArgumentException("Manufacturer fixed method requires both mean and standard deviation");
        }

        // Create statistics entity using manufacturer values (no calculation needed)
        QCStatistics statistics = new QCStatistics();
        statistics.setControlLotId(controlLot.getId());
        statistics.setCalculationDate(new Timestamp(System.currentTimeMillis()));
        statistics.setMean(java.math.BigDecimal.valueOf(controlLot.getManufacturerMean()));
        statistics.setStandardDeviation(java.math.BigDecimal.valueOf(controlLot.getManufacturerStdDev()));
        statistics.setNumValues(0); // Not calculated from results
        statistics.setCalculationMethod("MANUFACTURER_FIXED");

        return statistics;
    }
}
