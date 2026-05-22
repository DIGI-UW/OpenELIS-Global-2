package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dto.TestInstrumentPair;
import org.openelisglobal.qc.valueholder.QCControlLot;

/**
 * DAO interface for QCControlLot entity operations.
 */
public interface QCControlLotDAO extends BaseDAO<QCControlLot, String> {

    /**
     * Get all control lots for a specific test and instrument.
     */
    List<QCControlLot> getByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException;

    /**
     * Get active control lots for a test and instrument.
     */
    List<QCControlLot> getActiveByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException;

    /**
     * Get all ACTIVE control lots for an instrument across every test it's mapped
     * to. Used by bridge registration so the bridge can disambiguate lots embedded
     * in inbound sample names (FILE) or cross-check Q-segment lots (ASTM).
     */
    List<QCControlLot> getActiveByInstrument(String instrumentId) throws LIMSRuntimeException;

    /**
     * Get control lot by lot number.
     */
    QCControlLot getByLotNumber(String lotNumber) throws LIMSRuntimeException;

    /**
     * Count active control lots for a specific instrument.
     */
    long countActiveByInstrument(String instrumentId) throws LIMSRuntimeException;

    /**
     * Count active control lots for a specific test and instrument.
     */
    long countActiveByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException;

    /**
     * Get all distinct (testId, instrumentId) pairs from control lots.
     */
    List<TestInstrumentPair> findDistinctTestInstrumentPairs() throws LIMSRuntimeException;
}
