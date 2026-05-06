package org.openelisglobal.vector.daoimpl;

import jakarta.persistence.TypedQuery;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.vector.dao.VectorSamplingSiteDAO;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class VectorSamplingSiteDAOImpl extends BaseDAOImpl<VectorSamplingSite, Integer>
        implements VectorSamplingSiteDAO {

    public VectorSamplingSiteDAOImpl() {
        super(VectorSamplingSite.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSamplingSite> getByType(String type) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorSamplingSite> query = entityManager.createQuery(
                    "select s from VectorSamplingSite s where s.type = :type order by s.name",
                    VectorSamplingSite.class);
            query.setParameter("type", type);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSamplingSiteDAOImpl.getByType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSamplingSite> getActive() throws LIMSRuntimeException {
        try {
            return entityManager.createQuery("select s from VectorSamplingSite s where s.active = true order by s.name",
                    VectorSamplingSite.class).getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSamplingSiteDAOImpl.getActive()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VectorSamplingSite getByCode(String code) throws LIMSRuntimeException {
        try {
            TypedQuery<VectorSamplingSite> query = entityManager
                    .createQuery("select s from VectorSamplingSite s where s.code = :code", VectorSamplingSite.class);
            query.setParameter("code", code);
            List<VectorSamplingSite> list = query.getResultList();
            return list.isEmpty() ? null : list.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in VectorSamplingSiteDAOImpl.getByCode()", e);
        }
    }
}
