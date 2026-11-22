package org.openelisglobal.vector.identification.daoimpl;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.identification.dao.VectorSpecimenIdentificationDAO;
import org.openelisglobal.vector.identification.valueholder.VectorSpecimenIdentification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorSpecimenIdentificationDAOImpl extends BaseDAOImpl<VectorSpecimenIdentification, Long>
        implements VectorSpecimenIdentificationDAO {

    public VectorSpecimenIdentificationDAOImpl() {
        super(VectorSpecimenIdentification.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VectorSpecimenIdentification> getBySampleItemId(Long sampleItemId) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorSpecimenIdentification> query = entityManager.createQuery(
                    "select v from VectorSpecimenIdentification v where v.sampleItemId = :sampleItemId",
                    VectorSpecimenIdentification.class);
            query.setParameter("sampleItemId", sampleItemId);
            List<VectorSpecimenIdentification> list = query.getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSpecimenIdentificationDAOImpl.getBySampleItemId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<VectorSpecimenIdentification> getBySampleId(Long sampleId) throws LIMSRuntimeException {
        try {
            Query query = entityManager.createNativeQuery("SELECT v.* FROM clinlims.vector_specimen_identification v"
                    + " WHERE v.sample_item_id IN (SELECT id FROM clinlims.sample_item WHERE samp_id = :sampleId)"
                    + " ORDER BY v.sample_item_id", VectorSpecimenIdentification.class);
            query.setParameter("sampleId", sampleId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSpecimenIdentificationDAOImpl.getBySampleId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySampleId(Long sampleId) throws LIMSRuntimeException {
        try {
            Query query = entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM clinlims.vector_specimen_identification v"
                            + " WHERE v.sample_item_id IN (SELECT id FROM clinlims.sample_item WHERE samp_id = :sampleId)");
            query.setParameter("sampleId", sampleId);
            return ((Number) query.getSingleResult()).longValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSpecimenIdentificationDAOImpl.countBySampleId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySampleItemIds(List<Long> sampleItemIds) throws LIMSRuntimeException {
        if (sampleItemIds == null || sampleItemIds.isEmpty()) {
            return 0L;
        }
        try {
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT COUNT(v) FROM VectorSpecimenIdentification v WHERE v.sampleItemId IN :ids", Long.class);
            query.setParameter("ids", sampleItemIds);
            return query.getSingleResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSpecimenIdentificationDAOImpl.countBySampleItemIds()", e);
        }
    }
}
