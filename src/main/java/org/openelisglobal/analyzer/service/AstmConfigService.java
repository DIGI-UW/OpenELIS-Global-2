package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;

public interface AstmConfigService {

    AstmAnalyzerConfig getOrCreateConfig(String analyzerId);

    AstmAnalyzerConfig getConfig(String analyzerId);

    AstmAnalyzerConfig updateConfig(String analyzerId, Map<String, Object> update);

    List<AstmFieldExtractionConfig> getExtractionConfigs(String analyzerId);

    List<AstmFlagMapping> getFlagMappings(String analyzerId);

    boolean hasActiveQcRules(String analyzerId);
}
