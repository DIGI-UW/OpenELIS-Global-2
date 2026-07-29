package org.openelisglobal.testmethod.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testmethod.dao.TestMethodDAO;
import org.openelisglobal.testmethod.valueholder.TestMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestMethodDAOImpl extends BaseDAOImpl<TestMethod, String> implements TestMethodDAO {

    public TestMethodDAOImpl() {
        super(TestMethod.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestMethod> getActiveTestMethodsByTestId(String testId) {
        String hql = "from TestMethod tm where tm.testId = :testId and tm.isActive = 'Y' order by tm.effectiveDate";
        Query<TestMethod> query = entityManager.unwrap(Session.class).createQuery(hql, TestMethod.class);
        query.setParameter("testId", testId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean testMethodLinkExists(String testId, String methodId) {
        String hql = "select count(tm) from TestMethod tm where tm.testId = :testId and tm.methodId = :methodId and tm.isActive = 'Y'";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("testId", testId);
        query.setParameter("methodId", methodId);
        return query.uniqueResult() > 0;
    }

    @Override
    public void clearDefaultsForTest(String testId, String sysUserId) {
        String hql = "update TestMethod tm set tm.isDefault = false, tm.lastupdated = current_timestamp where tm.testId = :testId";
        Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
        query.setParameter("testId", testId);
        query.executeUpdate();
    }

    @Override
    public void updateIsDefaultAndEffectiveDate(String id, boolean isDefault, java.sql.Date effectiveDate) {
        String hql = "update TestMethod tm set tm.isDefault = :isDefault, tm.effectiveDate = :effectiveDate, tm.lastupdated = current_timestamp where tm.id = :id";
        Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
        query.setParameter("isDefault", isDefault);
        query.setParameter("effectiveDate", effectiveDate);
        query.setParameter("id", id);
        query.executeUpdate();
    }

    @Override
    public void deactivateLink(String id) {
        String hql = "update TestMethod tm set tm.isActive = 'N', tm.lastupdated = current_timestamp where tm.id = :id";
        Query<?> query = entityManager.unwrap(Session.class).createQuery(hql);
        query.setParameter("id", id);
        query.executeUpdate();
    }
}
