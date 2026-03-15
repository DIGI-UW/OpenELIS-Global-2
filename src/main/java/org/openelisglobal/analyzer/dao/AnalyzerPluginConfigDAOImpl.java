package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerPluginConfigDAOImpl extends BaseDAOImpl<AnalyzerPluginConfig, String>
        implements AnalyzerPluginConfigDAO {

    public AnalyzerPluginConfigDAOImpl() {
        super(AnalyzerPluginConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerPluginConfig> findByAnalyzerId(String analyzerId) {
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            return Optional.empty();
        }
        // analyzerId is the @Id field — entityManager.find() handles @Convert correctly
        return Optional.ofNullable(entityManager.find(AnalyzerPluginConfig.class, analyzerId.trim()));
    }
}
