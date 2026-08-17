package org.openelisglobal.accreditation.daoimpl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.valueholder.TestAccreditation;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestAccreditationDAOImpl extends BaseDAOImpl<TestAccreditation, Long> implements TestAccreditationDAO {

    public TestAccreditationDAOImpl() {
        super(TestAccreditation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getAll() {
        return entityManager.unwrap(Session.class).createQuery("from TestAccreditation ta", TestAccreditation.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByBody(Long accreditingBodyId) {
        Query<TestAccreditation> query = entityManager.unwrap(Session.class)
                .createQuery("from TestAccreditation ta where ta.accreditingBodyId = :bodyId", TestAccreditation.class);
        query.setParameter("bodyId", accreditingBodyId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByTest(String testId) {
        Query<TestAccreditation> query = entityManager.unwrap(Session.class)
                .createQuery("from TestAccreditation ta where ta.testId = :testId", TestAccreditation.class);
        query.setParameter("testId", testId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAccreditation> getByTestIds(Collection<String> testIds) {
        if (testIds == null || testIds.isEmpty()) {
            // An empty IN list is a syntax error in Postgres, so never build the query.
            return Collections.emptyList();
        }
        Query<TestAccreditation> query = entityManager.unwrap(Session.class)
                .createQuery("from TestAccreditation ta where ta.testId in (:testIds)", TestAccreditation.class);
        query.setParameterList("testIds", testIds);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public TestAccreditation getByTestAndBody(String testId, Long accreditingBodyId) {
        Query<TestAccreditation> query = entityManager.unwrap(Session.class).createQuery(
                "from TestAccreditation ta where ta.testId = :testId and ta.accreditingBodyId = :bodyId",
                TestAccreditation.class);
        query.setParameter("testId", testId);
        query.setParameter("bodyId", accreditingBodyId);
        return query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBody(Long accreditingBodyId) {
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(
                "select count(ta.id) from TestAccreditation ta where ta.accreditingBodyId = :bodyId", Long.class);
        query.setParameter("bodyId", accreditingBodyId);
        Long result = query.uniqueResult();
        return result == null ? 0L : result;
    }
}
