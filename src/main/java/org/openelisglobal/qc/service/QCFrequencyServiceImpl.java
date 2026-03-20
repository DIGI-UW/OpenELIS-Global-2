package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.qc.dao.QCFrequencyConfigDAO;
import org.openelisglobal.qc.valueholder.QCFrequencyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QCFrequencyServiceImpl implements QCFrequencyService {

    @Autowired
    private QCFrequencyConfigDAO frequencyConfigDAO;

    @Override
    @Transactional(readOnly = true)
    public Optional<QCFrequencyConfig> getFrequencyConfig(Long instrumentId) {
        return frequencyConfigDAO.findByInstrumentId(instrumentId);
    }

    @Override
    public QCFrequencyConfig updateFrequencyConfig(Long instrumentId, String frequencyType, Integer frequencyValue) {
        Optional<QCFrequencyConfig> existing = frequencyConfigDAO.findByInstrumentId(instrumentId);

        QCFrequencyConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setFrequencyType(frequencyType);
            config.setFrequencyValue(frequencyValue);
            config.setLastupdated(new Timestamp(System.currentTimeMillis()));
            frequencyConfigDAO.update(config);
        } else {
            config = new QCFrequencyConfig();
            config.setInstrumentId(instrumentId);
            config.setFrequencyType(frequencyType);
            config.setFrequencyValue(frequencyValue);
            config.setLastupdated(new Timestamp(System.currentTimeMillis()));
            frequencyConfigDAO.insert(config);
        }
        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceMetrics(Long instrumentId) {
        Map<String, Object> metrics = new HashMap<>();
        Optional<QCFrequencyConfig> config = frequencyConfigDAO.findByInstrumentId(instrumentId);

        metrics.put("instrumentId", instrumentId);
        if (config.isPresent()) {
            metrics.put("configured", true);
            metrics.put("frequencyType", config.get().getFrequencyType());
            metrics.put("frequencyValue", config.get().getFrequencyValue());
            metrics.put("lastUpdated", config.get().getLastupdated());
        } else {
            metrics.put("configured", false);
        }

        return metrics;
    }
}
