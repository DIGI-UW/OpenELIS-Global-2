package org.openelisglobal.qaevent.daoimpl;

import java.util.List;
import java.util.Optional;
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
public class NceSpecimenDAOImpl extends BaseDAOImpl<NceSpecimen, String> implements NceSpecimenDAO {

    public NceSpecimenDAOImpl() {
        super(NceSpecimen.class);
    }

    @Override
    public List<NceSpecimen> getSpecimenByNceId(String nceId) throws LIMSRuntimeException {
        List<NceSpecimen> list;
        try {
            String sql = "from NceSpecimen ns where ns.nceId=:nceId ";
            Query<NceSpecimen> query = entityManager.unwrap(Session.class).createQuery(sql, NceSpecimen.class);
            query.setParameter("nceId", Integer.parseInt(nceId));
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimen getSpecimenByNceId(String nceId)", e);
        }
        return list;
    }

    @Override
    public List<NceSpecimen> getSpecimenBySampleId(String sampleId) {
        List<NceSpecimen> list;
        String sql = "from NceSpecimen ns where ns.sampleItemId=:sampleId ";
        Query<NceSpecimen> query = entityManager.unwrap(Session.class).createQuery(sql, NceSpecimen.class);
        query.setParameter("sampleId", Integer.parseInt(sampleId));
        list = query.list();

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NceSpecimen> get(String id) {
        try {
            Integer intId = id != null ? Integer.valueOf(id) : null;
            NceSpecimen object = entityManager.find(NceSpecimen.class, intId);
            return Optional.ofNullable(object);
        } catch (NumberFormatException e) {
            LogEvent.logError(e);
            return Optional.empty();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NceSpecimenDAOImpl get", e);
        }
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
