package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AstmFlagMappingDAOImpl extends BaseDAOImpl<AstmFlagMapping, String> implements AstmFlagMappingDAO {

    public AstmFlagMappingDAOImpl() {
        super(AstmFlagMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmFlagMapping> findByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmFlagMapping.class);
            var root = cq.from(AstmFlagMapping.class);
            cq.select(root).where(cb.equal(root.get("analyzer").get("id"), analyzerId))
                    .orderBy(cb.asc(root.get("analyzerFlag")));
            Query<AstmFlagMapping> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmFlagMappingDAOImpl.findByAnalyzerId", e);
        }
    }
}
