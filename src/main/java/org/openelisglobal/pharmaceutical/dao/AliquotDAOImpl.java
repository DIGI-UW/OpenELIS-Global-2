package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.Aliquot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AliquotDAOImpl extends BaseDAOImpl<Aliquot, Integer> implements AliquotDAO {

    public AliquotDAOImpl() {
        super(Aliquot.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByParentSampleId(Integer parentSampleId) {
        try {
            String hql = "FROM Aliquot WHERE parentSample.id = :parentSampleId ORDER BY aliquotCode";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            query.setParameter("parentSampleId", parentSampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding Aliquots by parentSampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Aliquot findByBarcode(String barcode) {
        try {
            String hql = "FROM Aliquot WHERE barcode = :barcode";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            query.setParameter("barcode", barcode);
            query.setMaxResults(1);
            List<Aliquot> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding Aliquot by barcode", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByStatus(Aliquot.AliquotStatus status) {
        try {
            String hql = "FROM Aliquot WHERE status = :status ORDER BY createdAt DESC";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding Aliquots by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findByStorageLocation(Integer storageLocationId, String storageLocationType) {
        try {
            String hql = "FROM Aliquot WHERE storageLocationId = :locationId "
                    + "AND storageLocationType = :locationType ORDER BY storagePosition";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            query.setParameter("locationId", storageLocationId);
            query.setParameter("locationType", storageLocationType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding Aliquots by storage location", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findAvailableByParentSample(Integer parentSampleId) {
        try {
            String hql = "FROM Aliquot WHERE parentSample.id = :parentSampleId "
                    + "AND status = 'AVAILABLE' ORDER BY aliquotCode";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            query.setParameter("parentSampleId", parentSampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding available Aliquots", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aliquot> findExceedingFreezeThawLimit() {
        try {
            String hql = "FROM Aliquot WHERE freezeThawLimit IS NOT NULL "
                    + "AND freezeThawCount >= freezeThawLimit AND status = 'AVAILABLE'";
            Query<Aliquot> query = entityManager.unwrap(Session.class).createQuery(hql, Aliquot.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding Aliquots exceeding freeze-thaw limit", e);
        }
    }
}
