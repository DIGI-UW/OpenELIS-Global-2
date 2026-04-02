package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
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
            TypedQuery<NceAttachment> query = entityManager.createQuery(sql, NceAttachment.class);
            query.setParameter("nceId", nceId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO findByNceId()", e);
        }
    }

    @Override
    public void deleteByNceId(Integer nceId) {
        try {
            String sql = "delete from NceAttachment na where na.nceId = :nceId";
            entityManager.createQuery(sql).setParameter("nceId", nceId).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO deleteByNceId()", e);
        }
    }

    @Override
    public int countByNceId(Integer nceId) {
        try {
            String sql = "select count(*) from NceAttachment na where na.nceId = :nceId";
            TypedQuery<Long> query = entityManager.createQuery(sql, Long.class);
            query.setParameter("nceId", nceId);
            Long count = query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceAttachmentDAO countByNceId()", e);
        }
    }
}
