package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AstmPendingCode;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AstmPendingCodeDAOImpl extends BaseDAOImpl<AstmPendingCode, String> implements AstmPendingCodeDAO {

    public AstmPendingCodeDAOImpl() {
        super(AstmPendingCode.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmPendingCode> findByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmPendingCode.class);
            var root = cq.from(AstmPendingCode.class);
            cq.select(root).where(cb.equal(root.get("analyzer").get("id"), analyzerId))
                    .orderBy(cb.desc(root.get("lastSeenAt")));
            Query<AstmPendingCode> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmPendingCodeDAOImpl.findByAnalyzerId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AstmPendingCode> findPendingByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(AstmPendingCode.class);
            var root = cq.from(AstmPendingCode.class);
            cq.select(root)
                    .where(cb.and(cb.equal(root.get("analyzer").get("id"), analyzerId),
                            cb.equal(root.get("status"), AstmPendingCode.Status.PENDING.name())))
                    .orderBy(cb.desc(root.get("lastSeenAt")));
            Query<AstmPendingCode> query = entityManager.unwrap(Session.class).createQuery(cq);
            return query.list();
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmPendingCodeDAOImpl.findPendingByAnalyzerId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countPendingByAnalyzerId(String analyzerId) {
        try {
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(AstmPendingCode.class);
            cq.select(cb.count(root)).where(cb.and(cb.equal(root.get("analyzer").get("id"), analyzerId),
                    cb.equal(root.get("status"), AstmPendingCode.Status.PENDING.name())));
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(cq);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AstmPendingCodeDAOImpl.countPendingByAnalyzerId", e);
        }
    }
}
