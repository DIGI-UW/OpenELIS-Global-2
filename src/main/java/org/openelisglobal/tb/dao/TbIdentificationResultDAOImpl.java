package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationResult;
import org.openelisglobal.tb.valueholder.TbIdentificationResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB species identification results.
 */
@Component
@Transactional
public class TbIdentificationResultDAOImpl extends BaseDAOImpl<TbIdentificationResult, Integer>
        implements TbIdentificationResultDAO {

    public TbIdentificationResultDAOImpl() {
        super(TbIdentificationResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbIdentificationResult> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbIdentificationResult ir " + "LEFT JOIN FETCH ir.sampleItem "
                    + "LEFT JOIN FETCH ir.testedBy " + "LEFT JOIN FETCH ir.reviewedBy "
                    + "WHERE ir.sampleItem.id = :sampleItemId";
            Query<TbIdentificationResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbIdentificationResult.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding identification result by sample ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findByResult(IdentificationResult result) {
        try {
            String hql = "FROM TbIdentificationResult ir " + "LEFT JOIN FETCH ir.sampleItem "
                    + "LEFT JOIN FETCH ir.testedBy " + "WHERE ir.result = :result " + "ORDER BY ir.resultDate DESC";
            Query<TbIdentificationResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbIdentificationResult.class);
            query.setParameter("result", result);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding identification results by result: " + result, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findMtbPositive() {
        try {
            String hql = "FROM TbIdentificationResult ir " + "LEFT JOIN FETCH ir.sampleItem "
                    + "LEFT JOIN FETCH ir.testedBy " + "WHERE ir.result = :mtbResult " + "ORDER BY ir.resultDate DESC";
            Query<TbIdentificationResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbIdentificationResult.class);
            query.setParameter("mtbResult", IdentificationResult.MTB);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding MTB positive samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbIdentificationResult> findByTestedBy(Integer userId) {
        try {
            String hql = "FROM TbIdentificationResult ir " + "LEFT JOIN FETCH ir.sampleItem "
                    + "WHERE ir.testedBy.id = :userId " + "ORDER BY ir.resultDate DESC";
            Query<TbIdentificationResult> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbIdentificationResult.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding identification results by user: " + userId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByResult(IdentificationResult result) {
        try {
            String hql = "SELECT COUNT(ir) FROM TbIdentificationResult ir WHERE ir.result = :result";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("result", result);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting identification results by result: " + result, e);
        }
    }
}
