package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbQualityCheck;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB quality checks.
 */
@Component
@Transactional
public class TbQualityCheckDAOImpl extends BaseDAOImpl<TbQualityCheck, Integer> implements TbQualityCheckDAO {

    public TbQualityCheckDAOImpl() {
        super(TbQualityCheck.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbQualityCheck> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbQualityCheck qc " + "LEFT JOIN FETCH qc.sampleItem " + "LEFT JOIN FETCH qc.checkedBy "
                    + "WHERE qc.sampleItem.id = :sampleItemId";
            Query<TbQualityCheck> query = entityManager.unwrap(Session.class).createQuery(hql, TbQualityCheck.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC by sample item ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbQualityCheck> findByOverallResult(QcResult result) {
        try {
            String hql = "FROM TbQualityCheck qc " + "LEFT JOIN FETCH qc.sampleItem " + "LEFT JOIN FETCH qc.checkedBy "
                    + "WHERE qc.overallResult = :result " + "ORDER BY qc.qcDate DESC";
            Query<TbQualityCheck> query = entityManager.unwrap(Session.class).createQuery(hql, TbQualityCheck.class);
            query.setParameter("result", result);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC by result: " + result, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbQualityCheck> findByCheckedBy(Integer userId) {
        try {
            String hql = "FROM TbQualityCheck qc " + "LEFT JOIN FETCH qc.sampleItem "
                    + "WHERE qc.checkedBy.id = :userId " + "ORDER BY qc.qcDate DESC";
            Query<TbQualityCheck> query = entityManager.unwrap(Session.class).createQuery(hql, TbQualityCheck.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QC by user: " + userId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByOverallResult(QcResult result) {
        try {
            String hql = "SELECT COUNT(qc) FROM TbQualityCheck qc WHERE qc.overallResult = :result";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("result", result);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting QC by result: " + result, e);
        }
    }
}
