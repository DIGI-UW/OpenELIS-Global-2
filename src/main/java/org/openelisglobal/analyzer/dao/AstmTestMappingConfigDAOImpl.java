package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AstmTestMappingConfigDAOImpl extends BaseDAOImpl<AstmTestMappingConfig, String>
        implements AstmTestMappingConfigDAO {

    public AstmTestMappingConfigDAOImpl() {
        super(AstmTestMappingConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmTestMappingConfig> findByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmTestMappingConfig.class);
            var root = cq.from(AstmTestMappingConfig.class);
            cq.select(root).where(cb.equal(root.get("analyzer").get("id"), analyzerId))
                    .orderBy(cb.asc(root.get("analyzerTestName")));
            Query<AstmTestMappingConfig> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmTestMappingConfigDAOImpl.findByAnalyzerId", e);
        }
    }
}
