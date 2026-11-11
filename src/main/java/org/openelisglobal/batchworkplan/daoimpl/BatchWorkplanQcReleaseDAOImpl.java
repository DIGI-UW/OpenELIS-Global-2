package org.openelisglobal.batchworkplan.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanQcReleaseDAO;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanQcRelease;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BatchWorkplanQcReleaseDAOImpl extends BaseDAOImpl<BatchWorkplanQcRelease, Long>
        implements BatchWorkplanQcReleaseDAO {

    public BatchWorkplanQcReleaseDAOImpl() {
        super(BatchWorkplanQcRelease.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchWorkplanQcRelease> getForBatch(Long batchWorkplanId) {
        try {
            return entityManager
                    .createQuery("FROM BatchWorkplanQcRelease r WHERE r.batchWorkplanId = :batchId"
                            + " ORDER BY r.releasedAt DESC, r.id DESC", BatchWorkplanQcRelease.class)
                    .setParameter("batchId", batchWorkplanId).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading batch workplan QC releases", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getLastReleaseAt(String controlLotId) {
        try {
            // MAX() comes back typed as java.util.Date, which a TypedQuery of
            // Timestamp refuses; the value itself is the column's timestamp.
            Object latest = entityManager
                    .createQuery(
                            "SELECT MAX(r.releasedAt) FROM BatchWorkplanQcRelease r" + " WHERE r.controlLotId = :lotId")
                    .setParameter("lotId", controlLotId).getSingleResult();
            return latest == null ? null : new Timestamp(((java.util.Date) latest).getTime());
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error reading the last batch release for a control lot", e);
        }
    }
}
