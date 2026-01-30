package org.openelisglobal.tb.dao;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB media preparation batches.
 */
@Component
@Transactional
public class TbMediaPreparationDAOImpl extends BaseDAOImpl<TbMediaPreparation, Integer>
        implements TbMediaPreparationDAO {

    public TbMediaPreparationDAOImpl() {
        super(TbMediaPreparation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> getAll() {
        try {
            String hql = "FROM TbMediaPreparation mp LEFT JOIN FETCH mp.preparedBy ORDER BY mp.preparationDate DESC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error fetching all media preparation batches", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbMediaPreparation> findByBatchId(String batchId) {
        try {
            String hql = "FROM TbMediaPreparation mp " + "LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.batchId = :batchId";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("batchId", batchId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding media preparation by batch ID: " + batchId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findByMediaType(MediaType mediaType) {
        try {
            String hql = "FROM TbMediaPreparation mp " + "LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.mediaType = :mediaType " + "ORDER BY mp.preparationDate DESC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("mediaType", mediaType.name());
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding media preparation by type: " + mediaType, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findByQcStatus(MediaQcStatus qcStatus) {
        try {
            String hql = "FROM TbMediaPreparation mp " + "LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.qcStatus = :qcStatus " + "ORDER BY mp.preparationDate DESC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("qcStatus", qcStatus.name());
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding media preparation by QC status: " + qcStatus, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findAvailableForInoculation() {
        try {
            String hql = "FROM TbMediaPreparation mp " + "LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.qcStatus = :qcStatus " + "AND mp.expiryDate > :today " + "ORDER BY mp.expiryDate ASC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("qcStatus", MediaQcStatus.PASSED.name());
            query.setParameter("today", new Date(System.currentTimeMillis()));
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding available media for inoculation", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findAvailableByMediaType(MediaType mediaType) {
        try {
            String hql = "FROM TbMediaPreparation mp " + "LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.qcStatus = :qcStatus " + "AND mp.mediaType = :mediaType "
                    + "AND mp.expiryDate > :today " + "ORDER BY mp.expiryDate ASC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("qcStatus", MediaQcStatus.PASSED.name());
            query.setParameter("mediaType", mediaType.name());
            query.setParameter("today", new Date(System.currentTimeMillis()));
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding available media by type: " + mediaType, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(String batchId) {
        try {
            String hql = "SELECT COUNT(mp) FROM TbMediaPreparation mp WHERE mp.batchId = :batchId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("batchId", batchId);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking batch ID existence: " + batchId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByQcStatus(MediaQcStatus qcStatus) {
        try {
            String hql = "SELECT COUNT(mp) FROM TbMediaPreparation mp WHERE mp.qcStatus = :qcStatus";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("qcStatus", qcStatus.name());
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting media by QC status: " + qcStatus, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbMediaPreparation> findByNotebookEntryId(Integer notebookEntryId) {
        try {
            String hql = "FROM TbMediaPreparation mp LEFT JOIN FETCH mp.preparedBy "
                    + "WHERE mp.notebookEntryId = :notebookEntryId ORDER BY mp.preparationDate DESC";
            Query<TbMediaPreparation> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbMediaPreparation.class);
            query.setParameter("notebookEntryId", notebookEntryId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding media preparation by notebook entry ID: " + notebookEntryId,
                    e);
        }
    }
}
