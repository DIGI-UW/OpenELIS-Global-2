package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;

public interface AstmTestMappingConfigService {

    List<AstmTestMappingConfig> findByAnalyzerId(String analyzerId);

    AstmTestMappingConfig create(String analyzerId, Map<String, Object> payload);

    AstmTestMappingConfig update(String configId, Map<String, Object> payload);

    void delete(String configId);

    String applyTransform(String analyzerTestName, String rawValue, List<AstmTestMappingConfig> configs);
}
