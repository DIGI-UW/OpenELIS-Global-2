package org.openelisglobal.testsamplehandling.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testsamplehandling.dao.TestSampleHandlingHistoryDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestSampleHandlingHistoryDAOImpl extends BaseDAOImpl<TestSampleHandlingHistory, String>
        implements TestSampleHandlingHistoryDAO {

    public TestSampleHandlingHistoryDAOImpl() {
        super(TestSampleHandlingHistory.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSampleHandlingHistory> getByHandlingId(String handlingId) {
        String hql = "from TestSampleHandlingHistory h where h.testSampleHandlingId = :hid"
                + " order by h.changedAt desc, h.id desc";
        Query<TestSampleHandlingHistory> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestSampleHandlingHistory.class);
        query.setParameter("hid", handlingId);
        return query.list();
    }
}
