package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCControlLot;

/**
 * DAO interface for QCControlLot entity operations.
 */
public interface QCControlLotDAO extends BaseDAO<QCControlLot, String> {

    /**
     * Get all control lots for a specific test and instrument.
     */
    List<QCControlLot> getByTestAndInstrument(Integer testId, Integer instrumentId) throws LIMSRuntimeException;

    /**
     * Get active control lots for a test and instrument.
     */
    List<QCControlLot> getActiveByTestAndInstrument(Integer testId, Integer instrumentId) throws LIMSRuntimeException;

    /**
     * Get control lot by lot number.
     */
    QCControlLot getByLotNumber(String lotNumber) throws LIMSRuntimeException;
}
