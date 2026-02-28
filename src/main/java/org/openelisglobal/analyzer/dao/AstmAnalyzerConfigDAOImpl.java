package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AstmAnalyzerConfigDAOImpl extends BaseDAOImpl<AstmAnalyzerConfig, String>
        implements AstmAnalyzerConfigDAO {

    public AstmAnalyzerConfigDAOImpl() {
        super(AstmAnalyzerConfig.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AstmAnalyzerConfig> findByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmAnalyzerConfig.class);
            var root = cq.from(AstmAnalyzerConfig.class);
            cq.select(root).where(cb.equal(root.get("analyzer").get("id"), analyzerId));
            Query<AstmAnalyzerConfig> query = entityManager.unwrap(Session.class).createQuery(cq);
            List<AstmAnalyzerConfig> list = query.list();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmAnalyzerConfigDAOImpl.findByAnalyzerId", e);
        }
    }
}
