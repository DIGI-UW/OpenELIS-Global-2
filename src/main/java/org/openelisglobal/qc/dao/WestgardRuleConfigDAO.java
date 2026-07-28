package org.openelisglobal.qc.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.dto.TestInstrumentPair;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * DAO interface for WestgardRuleConfig entity operations.
 */
public interface WestgardRuleConfigDAO extends BaseDAO<WestgardRuleConfig, String> {

    /**
     * Get all enabled rules for a specific instrument.
     */
    List<WestgardRuleConfig> findEnabledByInstrument(String instrumentId) throws LIMSRuntimeException;

    /**
     * Get all rules for a specific test and instrument.
     */
    List<WestgardRuleConfig> findByTestAndInstrument(String testId, String instrumentId) throws LIMSRuntimeException;

    /**
     * Get specific rule configuration by test, instrument, and rule code.
     */
    WestgardRuleConfig findByTestInstrumentAndRule(String testId, String instrumentId, String ruleCode)
            throws LIMSRuntimeException;

    /**
     * Get all enabled rules for a test and instrument.
     */
    List<WestgardRuleConfig> findEnabledByTestAndInstrument(String testId, String instrumentId)
            throws LIMSRuntimeException;

    /**
     * Get all distinct (testId, instrumentId) pairs that have rule configurations.
     */
    List<TestInstrumentPair> findDistinctTestInstrumentPairs() throws LIMSRuntimeException;
}
