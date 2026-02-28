package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
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
        String hql = "FROM AnalyzerPluginConfig c WHERE c.analyzerId = :analyzerId";
        Query<AnalyzerPluginConfig> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerPluginConfig.class);
        query.setParameter("analyzerId", analyzerId.trim());
        return Optional.ofNullable(query.uniqueResult());
    }
}
