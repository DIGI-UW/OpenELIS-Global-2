package org.openelisglobal.qc.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qc.valueholder.QCFrequencyConfig;

public interface QCFrequencyConfigDAO extends BaseDAO<QCFrequencyConfig, Long> {

    Optional<QCFrequencyConfig> findByInstrumentId(Long instrumentId);
}
