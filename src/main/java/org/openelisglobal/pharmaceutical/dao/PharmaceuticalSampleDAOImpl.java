package org.openelisglobal.pharmaceutical.dao;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PharmaceuticalSampleDAOImpl extends BaseDAOImpl<PharmaceuticalSample, Integer>
        implements PharmaceuticalSampleDAO {

    public PharmaceuticalSampleDAOImpl() {
        super(PharmaceuticalSample.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalSample findByUniqueSampleId(String uniqueSampleId) {
        try {
            String hql = "FROM PharmaceuticalSample WHERE uniqueSampleId = :uniqueSampleId";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("uniqueSampleId", uniqueSampleId);
            query.setMaxResults(1);
            List<PharmaceuticalSample> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PharmaceuticalSample by uniqueSampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PharmaceuticalSample findByBarcode(String barcode) {
        try {
            String hql = "FROM PharmaceuticalSample WHERE barcode = :barcode";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("barcode", barcode);
            query.setMaxResults(1);
            List<PharmaceuticalSample> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PharmaceuticalSample by barcode", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findByStatus(PharmaceuticalSample.SampleStatus status) {
        try {
            String hql = "FROM PharmaceuticalSample WHERE status = :status ORDER BY registeredAt DESC";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PharmaceuticalSamples by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findByLabType(PharmaceuticalSample.LabType labType) {
        try {
            String hql = "FROM PharmaceuticalSample WHERE labType = :labType ORDER BY registeredAt DESC";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("labType", labType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding PharmaceuticalSamples by labType", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> findExpiringSoon(int daysAhead) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, daysAhead);
            Timestamp expiryThreshold = new Timestamp(calendar.getTimeInMillis());

            String hql = "FROM PharmaceuticalSample WHERE expiryRetestDate <= :threshold "
                    + "AND status NOT IN ('DISPOSED', 'COMPLETED') ORDER BY expiryRetestDate ASC";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("threshold", expiryThreshold);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding expiring PharmaceuticalSamples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmaceuticalSample> searchByName(String searchTerm) {
        try {
            String hql = "FROM PharmaceuticalSample WHERE LOWER(sampleName) LIKE LOWER(:term) "
                    + "OR LOWER(iupacName) LIKE LOWER(:term) ORDER BY sampleName";
            Query<PharmaceuticalSample> query = entityManager.unwrap(Session.class)
                    .createQuery(hql, PharmaceuticalSample.class);
            query.setParameter("term", "%" + searchTerm + "%");
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching PharmaceuticalSamples by name", e);
        }
    }
}
