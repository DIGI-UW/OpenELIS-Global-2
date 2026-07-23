package org.openelisglobal.testalertrule.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testalertrule.dao.TestAlertRuleDAO;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestAlertRuleDAOImpl extends BaseDAOImpl<TestAlertRule, String> implements TestAlertRuleDAO {

    public TestAlertRuleDAOImpl() {
        super(TestAlertRule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAlertRule> getByTestId(String testId) {
        String hql = "from TestAlertRule r where r.testId = :testId order by r.name, r.id";
        Query<TestAlertRule> query = entityManager.unwrap(Session.class).createQuery(hql, TestAlertRule.class);
        query.setParameter("testId", testId);
        return query.list();
    }
}
