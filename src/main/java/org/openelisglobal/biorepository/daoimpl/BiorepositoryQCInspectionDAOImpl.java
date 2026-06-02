package org.openelisglobal.biorepository.daoimpl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;
import org.openelisglobal.biorepository.dao.BiorepositoryQCInspectionDAO;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection;
import org.openelisglobal.biorepository.valueholder.BiorepositoryQCInspection.QCResult;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;

/**
 * DAO implementation for BiorepositoryQCInspection entity operations.
 */
@Component
public class BiorepositoryQCInspectionDAOImpl extends BaseDAOImpl<BiorepositoryQCInspection, Integer>
        implements BiorepositoryQCInspectionDAO {

    public BiorepositoryQCInspectionDAOImpl() {
        super(BiorepositoryQCInspection.class);
    }

    @Override
    public List<BiorepositoryQCInspection> getByBioSampleId(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.bioSample.id = :bioSampleId "
                + "ORDER BY qc.inspectionDate DESC";
        return session.createQuery(hql, BiorepositoryQCInspection.class).setParameter("bioSampleId", bioSampleId)
                .getResultList();
    }

    @Override
    public BiorepositoryQCInspection getMostRecentByBioSampleId(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.bioSample.id = :bioSampleId "
                + "ORDER BY qc.inspectionDate DESC";
        List<BiorepositoryQCInspection> results = session.createQuery(hql, BiorepositoryQCInspection.class)
                .setParameter("bioSampleId", bioSampleId).setMaxResults(1).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

        @Override
        public Map<Integer, BiorepositoryQCInspection> getMostRecentByBioSampleIds(List<Integer> bioSampleIds) {
                if (bioSampleIds == null || bioSampleIds.isEmpty()) {
                        return Map.of();
                }

                Session session = entityManager.unwrap(Session.class);
                String hql = "SELECT qc FROM BiorepositoryQCInspection qc "
                                + "JOIN FETCH qc.bioSample bs "
                                + "WHERE bs.id IN :bioSampleIds "
                                + "ORDER BY bs.id ASC, qc.inspectionDate DESC, qc.id DESC";

                List<BiorepositoryQCInspection> inspections = session.createQuery(hql, BiorepositoryQCInspection.class)
                                .setParameter("bioSampleIds", bioSampleIds)
                                .getResultList();

                Map<Integer, BiorepositoryQCInspection> mostRecentBySampleId = new HashMap<>();
                for (BiorepositoryQCInspection inspection : inspections) {
                        Integer sampleId = inspection.getBioSample() != null ? inspection.getBioSample().getId() : null;
                        if (sampleId != null && !mostRecentBySampleId.containsKey(sampleId)) {
                                mostRecentBySampleId.put(sampleId, inspection);
                        }
                }
                return mostRecentBySampleId;
        }

    @Override
    public List<BiorepositoryQCInspection> getByQCResult(QCResult qcResult) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.qcResult = :qcResult "
                + "ORDER BY qc.inspectionDate DESC";
        return session.createQuery(hql, BiorepositoryQCInspection.class).setParameter("qcResult", qcResult.name())
                .getResultList();
    }

    @Override
    public List<BiorepositoryQCInspection> getByInspectorName(String inspectorName) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.inspectorName = :inspectorName "
                + "ORDER BY qc.inspectionDate DESC";
        return session.createQuery(hql, BiorepositoryQCInspection.class).setParameter("inspectorName", inspectorName)
                .getResultList();
    }

    @Override
    public List<BiorepositoryQCInspection> getByQcBatchId(String qcBatchId) {
        if (qcBatchId == null || qcBatchId.isBlank()) {
            return List.of();
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.qcBatchId = :qcBatchId "
                + "ORDER BY qc.inspectionDate ASC, qc.id ASC";
        return session.createQuery(hql, BiorepositoryQCInspection.class).setParameter("qcBatchId", qcBatchId.trim())
                .getResultList();
    }

    @Override
    public long countByQCResult(QCResult qcResult) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(qc) FROM BiorepositoryQCInspection qc WHERE qc.qcResult = :qcResult";
        return session.createQuery(hql, Long.class).setParameter("qcResult", qcResult.name()).getSingleResult();
    }

    @Override
    public boolean existsByBioSampleId(Integer bioSampleId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(qc) FROM BiorepositoryQCInspection qc WHERE qc.bioSample.id = :bioSampleId";
        Long count = session.createQuery(hql, Long.class).setParameter("bioSampleId", bioSampleId).getSingleResult();
        return count > 0;
    }

    @Override
    public List<BiorepositoryQCInspection> getInspectionsByDateRange(Timestamp startDate, Timestamp endDate) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM BiorepositoryQCInspection qc WHERE qc.inspectionDate BETWEEN :startDate AND :endDate "
                + "ORDER BY qc.inspectionDate ASC";
        return session.createQuery(hql, BiorepositoryQCInspection.class).setParameter("startDate", startDate)
                .setParameter("endDate", endDate).getResultList();
    }

    @Override
    public boolean hasInspectionBetween(Integer bioSampleId, Timestamp startDate, Timestamp endDate) {
        if (bioSampleId == null || startDate == null || endDate == null) {
            return false;
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT COUNT(qc) FROM BiorepositoryQCInspection qc WHERE qc.bioSample.id = :bioSampleId "
                + "AND qc.inspectionDate >= :startDate AND qc.inspectionDate <= :endDate";
        Long count = session.createQuery(hql, Long.class).setParameter("bioSampleId", bioSampleId)
                .setParameter("startDate", startDate).setParameter("endDate", endDate).getSingleResult();
        return count > 0;
    }

    @Override
    public Set<Integer> getBioSampleIdsWithAnyInspection(List<Integer> bioSampleIds) {
        if (bioSampleIds == null || bioSampleIds.isEmpty()) {
            return Set.of();
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT qc.bioSample.id FROM BiorepositoryQCInspection qc "
                + "WHERE qc.bioSample.id IN :bioSampleIds";
        List<Integer> ids = session.createQuery(hql, Integer.class).setParameter("bioSampleIds", bioSampleIds)
                .getResultList();
        return new HashSet<>(ids);
    }

    @Override
    public Set<Integer> getBioSampleIdsInspectedBetween(List<Integer> bioSampleIds, Timestamp startDate,
            Timestamp endDate) {
        if (bioSampleIds == null || bioSampleIds.isEmpty() || startDate == null || endDate == null) {
            return Set.of();
        }
        Session session = entityManager.unwrap(Session.class);
        String hql = "SELECT DISTINCT qc.bioSample.id FROM BiorepositoryQCInspection qc "
                + "WHERE qc.bioSample.id IN :bioSampleIds "
                + "AND qc.inspectionDate >= :startDate AND qc.inspectionDate <= :endDate";
        List<Integer> ids = session.createQuery(hql, Integer.class).setParameter("bioSampleIds", bioSampleIds)
                .setParameter("startDate", startDate).setParameter("endDate", endDate).getResultList();
        return new HashSet<>(ids);
    }
}
