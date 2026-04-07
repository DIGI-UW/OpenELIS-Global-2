package org.openelisglobal.qaevent.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qaevent.dao.NceSpecimenDAO;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NceSpecimenDAOImpl extends BaseDAOImpl<NceSpecimen, Integer> implements NceSpecimenDAO {

    public NceSpecimenDAOImpl() {
        super(NceSpecimen.class);
    }

    @Override
    public List<NceSpecimen> getSpecimenByNceId(Integer nceId) throws LIMSRuntimeException {
        try {
            String sql = "from NceSpecimen ns where ns.nceId = :nceId";
            TypedQuery<NceSpecimen> query = entityManager.createQuery(sql, NceSpecimen.class);
            query.setParameter("nceId", nceId);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimen getSpecimenByNceId(Integer nceId)", e);
        }
    }

    @Override
    public List<NceSpecimen> getSpecimenBySampleId(Integer sampleId) {
        String sql = "from NceSpecimen ns where ns.sampleItemId = :sampleId";
        TypedQuery<NceSpecimen> query = entityManager.createQuery(sql, NceSpecimen.class);
        query.setParameter("sampleId", sampleId);
        return query.getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId) {
        try {
            String sql = "SELECT COUNT(*) FROM NceSpecimen ns WHERE ns.nceId = :nceId AND ns.sampleItemId = :sampleItemId";
            TypedQuery<Long> query = entityManager.createQuery(sql, Long.class);
            query.setParameter("nceId", nceId);
            query.setParameter("sampleItemId", sampleItemId);
            Long count = query.getSingleResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimenDAOImpl existsByNceIdAndSampleItemId", e);
        }
    }
}
