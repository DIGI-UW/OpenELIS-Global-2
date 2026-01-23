package org.openelisglobal.biorepository.daoimpl;

import java.sql.Timestamp;
import java.util.List;
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
}
