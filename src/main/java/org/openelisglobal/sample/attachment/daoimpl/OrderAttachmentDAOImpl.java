package org.openelisglobal.sample.attachment.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.attachment.dao.OrderAttachmentDAO;
import org.openelisglobal.sample.attachment.valueholder.OrderAttachment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrderAttachmentDAOImpl extends BaseDAOImpl<OrderAttachment, Integer> implements OrderAttachmentDAO {

    public OrderAttachmentDAOImpl() {
        super(OrderAttachment.class);
    }

    @Override
    public List<OrderAttachment> findActiveBySampleId(Long sampleId) {
        try {
            String hql = "from OrderAttachment oa where oa.sampleId = :sampleId and oa.isDeleted = false"
                    + " order by oa.uploadedAt desc";
            TypedQuery<OrderAttachment> query = entityManager.createQuery(hql, OrderAttachment.class);
            query.setParameter("sampleId", sampleId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrderAttachmentDAO findActiveBySampleId()", e);
        }
    }

    @Override
    public int countActiveBySampleId(Long sampleId) {
        try {
            String hql = "select count(*) from OrderAttachment oa where oa.sampleId = :sampleId and oa.isDeleted = false";
            TypedQuery<Long> query = entityManager.createQuery(hql, Long.class);
            query.setParameter("sampleId", sampleId);
            Long count = query.getSingleResult();
            return count != null ? count.intValue() : 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OrderAttachmentDAO countActiveBySampleId()", e);
        }
    }
}
