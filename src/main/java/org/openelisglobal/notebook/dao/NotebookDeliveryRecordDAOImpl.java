package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NotebookDeliveryRecord;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for NotebookDeliveryRecord entity operations.
 */
@Component
public class NotebookDeliveryRecordDAOImpl extends BaseDAOImpl<NotebookDeliveryRecord, Integer>
        implements NotebookDeliveryRecordDAO {

    public NotebookDeliveryRecordDAOImpl() {
        super(NotebookDeliveryRecord.class);
    }

    @Override
    public List<NotebookDeliveryRecord> getByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookDeliveryRecord ndr " + "WHERE ndr.notebook.id = :notebookId "
                + "ORDER BY ndr.deliveredAt DESC";
        return session.createQuery(hql, NotebookDeliveryRecord.class).setParameter("notebookId", notebookId)
                .getResultList();
    }

    @Override
    public List<NotebookDeliveryRecord> getByNotebookIdAndDeliveryType(Integer notebookId, String deliveryType) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NotebookDeliveryRecord ndr "
                + "WHERE ndr.notebook.id = :notebookId AND ndr.deliveryType = :deliveryType "
                + "ORDER BY ndr.deliveredAt DESC";
        return session.createQuery(hql, NotebookDeliveryRecord.class).setParameter("notebookId", notebookId)
                .setParameter("deliveryType", deliveryType).getResultList();
    }

    @Override
    public long getCountByNotebookId(Integer notebookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(ndr) FROM NotebookDeliveryRecord ndr " + "WHERE ndr.notebook.id = :notebookId";
        return session.createQuery(hql, Long.class).setParameter("notebookId", notebookId).getSingleResult();
    }
}
