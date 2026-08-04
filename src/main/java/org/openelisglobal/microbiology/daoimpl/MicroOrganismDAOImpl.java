package org.openelisglobal.microbiology.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MicroOrganismDAOImpl extends BaseDAOImpl<MicroOrganism, String> implements MicroOrganismDAO {

    public MicroOrganismDAOImpl() {
        super(MicroOrganism.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroOrganism> getActiveOrganisms() {
        return entityManager.unwrap(Session.class)
                .createQuery("from MicroOrganism o where o.isActive = 'Y' order by o.displayName", MicroOrganism.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName) {
        Query<MicroOrganism> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroOrganism o where lower(o.displayName) = lower(:displayName)", MicroOrganism.class);
        query.setParameter("displayName", displayName);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode) {
        Query<MicroOrganism> query = entityManager.unwrap(Session.class).createQuery(
                "from MicroOrganism o where lower(o.whonetCode) = lower(:whonetCode)", MicroOrganism.class);
        query.setParameter("whonetCode", whonetCode);
        return query.uniqueResultOptional();
    }

    @Override
    @Transactional(readOnly = true)
    public long countWorkflowReferences(String organismId) {
        Query<Long> query = entityManager.unwrap(Session.class)
                .createQuery("select count(i.id) from MicroIsolate i where i.organismId = :organismId", Long.class);
        query.setParameter("organismId", organismId);
        return query.uniqueResult();
    }
}
