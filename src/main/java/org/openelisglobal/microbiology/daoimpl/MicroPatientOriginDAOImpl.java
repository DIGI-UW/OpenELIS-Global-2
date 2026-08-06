package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroPatientOriginDAOImpl extends BaseDAOImpl<MicroPatientOrigin, String>
        implements MicroPatientOriginDAO {

    public MicroPatientOriginDAOImpl() {
        super(MicroPatientOrigin.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroPatientOrigin> getActivePatientOrigins() {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroPatientOrigin o where o.isActive = 'Y' order by o.sortOrder, o.displayName",
                        MicroPatientOrigin.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveCode(String code) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(o.id) from MicroPatientOrigin o where o.isActive = 'Y' and o.code = :code", Long.class);
        query.setParameter("code", code);
        return query.getSingleResult() > 0;
    }
}
