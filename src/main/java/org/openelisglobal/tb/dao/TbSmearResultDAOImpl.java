package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbEnums.AfbResult;
import org.openelisglobal.tb.valueholder.TbSmearResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB smear microscopy results.
 */
@Component
@Transactional
public class TbSmearResultDAOImpl extends BaseDAOImpl<TbSmearResult, Integer> implements TbSmearResultDAO {

    public TbSmearResultDAOImpl() {
        super(TbSmearResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbSmearResult> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbSmearResult sr " + "LEFT JOIN FETCH sr.sampleItem " + "LEFT JOIN FETCH sr.testedBy "
                    + "LEFT JOIN FETCH sr.reviewedBy " + "WHERE sr.sampleItem.id = :sampleItemId";
            Query<TbSmearResult> query = entityManager.unwrap(Session.class).createQuery(hql, TbSmearResult.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding smear result by sample ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findByAfbResult(AfbResult result) {
        try {
            String hql = "FROM TbSmearResult sr " + "LEFT JOIN FETCH sr.sampleItem " + "LEFT JOIN FETCH sr.testedBy "
                    + "WHERE sr.afbResult = :result " + "ORDER BY sr.resultDate DESC";
            Query<TbSmearResult> query = entityManager.unwrap(Session.class).createQuery(hql, TbSmearResult.class);
            query.setParameter("result", result);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding smear results by AFB result: " + result, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findPositiveResults() {
        try {
            String hql = "FROM TbSmearResult sr " + "LEFT JOIN FETCH sr.sampleItem " + "LEFT JOIN FETCH sr.testedBy "
                    + "WHERE sr.afbResult != :negativeResult " + "ORDER BY sr.resultDate DESC";
            Query<TbSmearResult> query = entityManager.unwrap(Session.class).createQuery(hql, TbSmearResult.class);
            query.setParameter("negativeResult", AfbResult.NEGATIVE);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding positive smear results", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbSmearResult> findByTestedBy(Integer userId) {
        try {
            String hql = "FROM TbSmearResult sr " + "LEFT JOIN FETCH sr.sampleItem " + "WHERE sr.testedBy.id = :userId "
                    + "ORDER BY sr.resultDate DESC";
            Query<TbSmearResult> query = entityManager.unwrap(Session.class).createQuery(hql, TbSmearResult.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding smear results by user: " + userId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByAfbResult(AfbResult result) {
        try {
            String hql = "SELECT COUNT(sr) FROM TbSmearResult sr WHERE sr.afbResult = :result";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("result", result);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting smear results by AFB result: " + result, e);
        }
    }
}
