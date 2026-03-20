package org.openelisglobal.qc.service;

import java.util.Map;
import java.util.Optional;
import org.openelisglobal.qc.valueholder.QCFrequencyConfig;

public interface QCFrequencyService {

    Optional<QCFrequencyConfig> getFrequencyConfig(Long instrumentId);

    QCFrequencyConfig updateFrequencyConfig(Long instrumentId, String frequencyType, Integer frequencyValue);

    Map<String, Object> getComplianceMetrics(Long instrumentId);
}
