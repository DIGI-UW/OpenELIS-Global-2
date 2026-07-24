package org.openelisglobal.testreagentlink.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testreagentlink.dao.TestReagentLinkDAO;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestReagentLinkDAOImpl extends BaseDAOImpl<TestReagentLink, String> implements TestReagentLinkDAO {

    public TestReagentLinkDAOImpl() {
        super(TestReagentLink.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReagentLink> getByTestId(String testId) {
        String hql = "from TestReagentLink l where l.testId = :testId order by l.usageType, l.id";
        Query<TestReagentLink> query = entityManager.unwrap(Session.class).createQuery(hql, TestReagentLink.class);
        query.setParameter("testId", testId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public TestReagentLink getByTestIdAndReagentId(String testId, Long reagentId) {
        String hql = "from TestReagentLink l where l.testId = :testId and l.reagentId = :reagentId";
        Query<TestReagentLink> query = entityManager.unwrap(Session.class).createQuery(hql, TestReagentLink.class);
        query.setParameter("testId", testId);
        query.setParameter("reagentId", reagentId);
        return query.uniqueResultOptional().orElse(null);
    }
}
