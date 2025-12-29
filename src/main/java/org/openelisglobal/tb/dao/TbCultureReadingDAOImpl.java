package org.openelisglobal.tb.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for TB culture readings.
 */
@Component
@Transactional
public class TbCultureReadingDAOImpl extends BaseDAOImpl<TbCultureReading, Integer> implements TbCultureReadingDAO {

    public TbCultureReadingDAOImpl() {
        super(TbCultureReading.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem " + "LEFT JOIN FETCH cr.readBy "
                    + "WHERE cr.sampleItem.id = :sampleItemId " + "ORDER BY cr.weekNumber ASC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding culture readings by sample ID: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbCultureReading> findBySampleItemIdAndWeek(String sampleItemId, Integer weekNumber) {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem " + "LEFT JOIN FETCH cr.readBy "
                    + "WHERE cr.sampleItem.id = :sampleItemId " + "AND cr.weekNumber = :weekNumber";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setParameter("weekNumber", weekNumber);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException(
                    "Error finding culture reading for sample " + sampleItemId + " week " + weekNumber, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findByGrowthObservation(GrowthObservation observation) {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem "
                    + "WHERE cr.growthObservation = :observation " + "ORDER BY cr.readingDate DESC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            query.setParameter("observation", observation);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding culture readings by observation: " + observation, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TbCultureReading> findLatestBySampleItemId(String sampleItemId) {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem " + "LEFT JOIN FETCH cr.readBy "
                    + "WHERE cr.sampleItem.id = :sampleItemId " + "ORDER BY cr.weekNumber DESC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding latest culture reading for sample: " + sampleItemId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long countGrowthDetected() {
        try {
            String hql = "SELECT COUNT(DISTINCT cr.sampleItem.id) FROM TbCultureReading cr "
                    + "WHERE cr.growthObservation = :observation";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("observation", GrowthObservation.GROWTH_DETECTED);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting growth detected samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findSampleItemIdsWithoutReadingForWeek(Integer weekNumber) {
        try {
            String sql = "SELECT DISTINCT si.id FROM clinlims.sample_item si "
                    + "JOIN clinlims.tb_sample_registration tsr ON tsr.sample_item_id = si.id " + "WHERE NOT EXISTS ("
                    + "  SELECT 1 FROM clinlims.tb_culture_reading cr "
                    + "  WHERE cr.sample_item_id = si.id AND cr.week_number = :weekNumber" + ")";
            @SuppressWarnings("unchecked")
            Query<String> query = (Query<String>) entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("weekNumber", weekNumber);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding samples without reading for week: " + weekNumber, e);
        }
    }

    // ====== Stage 4: Inoculation & Incubation Monitoring Methods ======

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findInoculatedSamples() {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem "
                    + "LEFT JOIN FETCH cr.mediaBatch " + "LEFT JOIN FETCH cr.sampleProcessing "
                    + "WHERE cr.inoculationDate IS NOT NULL " + "ORDER BY cr.inoculationDate DESC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding inoculated samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findIncubatingSamples() {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem "
                    + "LEFT JOIN FETCH cr.mediaBatch " + "LEFT JOIN FETCH cr.readBy "
                    + "WHERE cr.inoculationDate IS NOT NULL " + "AND cr.cultureResult IS NULL "
                    + "ORDER BY cr.inoculationDate ASC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding incubating samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findByCultureResult(CultureResult result) {
        try {
            String hql = "FROM TbCultureReading cr " + "LEFT JOIN FETCH cr.sampleItem "
                    + "WHERE cr.cultureResult = :result " + "ORDER BY cr.finalResultDate DESC";
            Query<TbCultureReading> query = entityManager.unwrap(Session.class).createQuery(hql,
                    TbCultureReading.class);
            query.setParameter("result", result);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding samples by culture result: " + result, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TbCultureReading> findCulturePositiveSamples() {
        return findByCultureResult(CultureResult.POSITIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByIncubationWeekRange(int startWeek, int endWeek) {
        try {
            String hql = "SELECT COUNT(DISTINCT cr.sampleItem.id) FROM TbCultureReading cr "
                    + "WHERE cr.inoculationDate IS NOT NULL " + "AND cr.cultureResult IS NULL "
                    + "AND cr.weekNumber >= :startWeek " + "AND cr.weekNumber <= :endWeek";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
            query.setParameter("startWeek", startWeek);
            query.setParameter("endWeek", endWeek);
            Long result = query.uniqueResult();
            return result != null ? result : 0L;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error counting samples by incubation week range", e);
        }
    }
}
