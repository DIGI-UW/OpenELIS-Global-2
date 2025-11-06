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
}
