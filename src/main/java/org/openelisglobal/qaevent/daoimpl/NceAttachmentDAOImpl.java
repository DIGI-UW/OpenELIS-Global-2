package org.openelisglobal.qaevent.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceAttachmentDAO;
import org.openelisglobal.qaevent.valueholder.NceAttachment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for NceAttachment entities.
 */
@Component
@Transactional
public class NceAttachmentDAOImpl extends BaseDAOImpl<NceAttachment, Integer> implements NceAttachmentDAO {

    public NceAttachmentDAOImpl() {
        super(NceAttachment.class);
    }

    @Override
    public List<NceAttachment> findByNceId(Integer nceId) {
        try {
            String sql = "from NceAttachment na where na.nceId = :nceId order by na.uploadedDate desc";
            Query<NceAttachment> query = entityManager.unwrap(Session.class).createQuery(sql, NceAttachment.class);
            query.setParameter("nceId", nceId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO findByNceId()", e);
        }
    }

    @Override
    public void deleteByNceId(Integer nceId) {
        try {
            String sql = "delete from NceAttachment na where na.nceId = :nceId";
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("nceId", nceId);
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO deleteByNceId()", e);
        }
    }

    @Override
    public int countByNceId(Integer nceId) {
        try {
            String sql = "select count(*) from NceAttachment na where na.nceId = :nceId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("nceId", nceId);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO countByNceId()", e);
        }
    }
}
