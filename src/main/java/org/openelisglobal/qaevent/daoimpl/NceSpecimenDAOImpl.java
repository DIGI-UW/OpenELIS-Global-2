package org.openelisglobal.qaevent.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
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
        List<NceSpecimen> list;
        try {
            String sql = "from NceSpecimen ns where ns.nceId=:nceId ";
            Query<NceSpecimen> query = entityManager.unwrap(Session.class).createQuery(sql, NceSpecimen.class);
            query.setParameter("nceId", nceId);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimen getSpecimenByNceId(Integer nceId)", e);
        }
        return list;
    }

    @Override
    public List<NceSpecimen> getSpecimenBySampleId(Integer sampleId) {
        List<NceSpecimen> list;
        String sql = "from NceSpecimen ns where ns.sampleItemId=:sampleId ";
        Query<NceSpecimen> query = entityManager.unwrap(Session.class).createQuery(sql, NceSpecimen.class);
        query.setParameter("sampleId", sampleId);
        list = query.list();

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNceIdAndSampleItemId(Integer nceId, Integer sampleItemId) {
        try {
            String sql = "SELECT COUNT(*) FROM NceSpecimen ns WHERE ns.nceId = :nceId AND ns.sampleItemId = :sampleItemId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("nceId", nceId);
            query.setParameter("sampleItemId", sampleItemId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimenDAOImpl existsByNceIdAndSampleItemId", e);
        }
    }
}
