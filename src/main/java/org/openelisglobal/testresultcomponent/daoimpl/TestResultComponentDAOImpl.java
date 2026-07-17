package org.openelisglobal.testresultcomponent.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testresultcomponent.dao.TestResultComponentDAO;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestResultComponentDAOImpl extends BaseDAOImpl<TestResultComponent, String>
        implements TestResultComponentDAO {

    public TestResultComponentDAOImpl() {
        super(TestResultComponent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultComponent> getComponentsByTestId(String testId) {
        String hql = "from TestResultComponent c where c.testId = :testId order by c.displayOrder, c.code";
        Query<TestResultComponent> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestResultComponent.class);
        query.setParameter("testId", testId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultComponent> getActiveComponentsByTestId(String testId) {
        String hql = "from TestResultComponent c where c.testId = :testId and c.isActive = 'Y'"
                + " order by c.displayOrder, c.code";
        Query<TestResultComponent> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestResultComponent.class);
        query.setParameter("testId", testId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultComponent getByTestIdAndCode(String testId, String code) {
        String hql = "from TestResultComponent c where c.testId = :testId and c.code = :code";
        Query<TestResultComponent> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestResultComponent.class);
        query.setParameter("testId", testId);
        query.setParameter("code", code);
        return query.uniqueResultOptional().orElse(null);
    }
}
