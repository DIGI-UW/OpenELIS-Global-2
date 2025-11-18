package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerConfigurationDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerConfigurationServiceImpl extends BaseObjectServiceImpl<AnalyzerConfiguration, String>
        implements AnalyzerConfigurationService {

    @Autowired
    private AnalyzerConfigurationDAO analyzerConfigurationDAO;

    @Autowired
    private AnalyzerService analyzerService;

    public AnalyzerConfigurationServiceImpl() {
        super(AnalyzerConfiguration.class);
    }

    @Override
    protected AnalyzerConfigurationDAO getBaseObjectDAO() {
        return analyzerConfigurationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> getByAnalyzerId(String analyzerId) {
        return analyzerConfigurationDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    public String createConfiguration(Analyzer analyzer, String ipAddress, Integer port, List<String> testUnitIds) {
        // Check if configuration already exists
        Optional<AnalyzerConfiguration> existing = analyzerConfigurationDAO.findByAnalyzerId(analyzer.getId());
        if (existing.isPresent()) {
            throw new LIMSRuntimeException("AnalyzerConfiguration already exists for analyzer: " + analyzer.getId());
        }

        AnalyzerConfiguration config = new AnalyzerConfiguration();
        config.setAnalyzer(analyzer);
        config.setIpAddress(ipAddress);
        config.setPort(port);
        config.setProtocolVersion("ASTM LIS2-A2");
        config.setTestUnitIds(testUnitIds);
        config.setSysUserId("1"); // Default system user (should come from security context)

        return analyzerConfigurationDAO.insert(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerConfiguration> getAllWithAnalyzers() {
        return analyzerConfigurationDAO.getAll();
    }
}
