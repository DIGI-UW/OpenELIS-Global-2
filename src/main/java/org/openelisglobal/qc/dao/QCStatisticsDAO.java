package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * DAO interface for QCStatistics entity operations.
 */
public interface QCStatisticsDAO extends BaseDAO<QCStatistics, String> {

    /**
     * Get latest statistics for a control lot.
     */
    QCStatistics findLatestByControlLot(String controlLotId) throws LIMSRuntimeException;

    /**
     * Get statistics by calculation method.
     */
    List<QCStatistics> findByCalculationMethod(String controlLotId, String calculationMethod)
            throws LIMSRuntimeException;

    /**
     * Get all statistics for a control lot ordered by calculation date.
     */
    List<QCStatistics> findAllByControlLot(String controlLotId) throws LIMSRuntimeException;
}
