package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroAntibioticDAOImpl extends BaseDAOImpl<MicroAntibiotic, String> implements MicroAntibioticDAO {

    public MicroAntibioticDAOImpl() {
        super(MicroAntibiotic.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroAntibiotic> getActiveAntibiotics() {
        return entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where a.isActive = 'Y' order by a.displayName", MicroAntibiotic.class).list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAntibiotic> findByDisplayNameIgnoreCase(String displayName) {
        Query<MicroAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where lower(a.displayName) = lower(:displayName)", MicroAntibiotic.class);
        query.setParameter("displayName", displayName);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroAntibiotic> findByWhonetCodeIgnoreCase(String whonetCode) {
        Query<MicroAntibiotic> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroAntibiotic a where lower(a.whonetCode) = lower(:whonetCode)", MicroAntibiotic.class);
        query.setParameter("whonetCode", whonetCode);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWorkflowReferences(String antibioticId) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(r.id) from MicroAstReading r where r.antibioticId = :antibioticId", Long.class);
        query.setParameter("antibioticId", antibioticId);
        return query.uniqueResult();
    }
}
