package org.openelisglobal.testresultinterpretation.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testresultinterpretation.dao.TestResultInterpretationDAO;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestResultInterpretationDAOImpl extends BaseDAOImpl<TestResultInterpretation, String>
        implements TestResultInterpretationDAO {

    public TestResultInterpretationDAOImpl() {
        super(TestResultInterpretation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultInterpretation> getByComponentId(String componentId) {
        String hql = "from TestResultInterpretation i where i.componentId = :componentId order by i.displayOrder";
        Query<TestResultInterpretation> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestResultInterpretation.class);
        query.setParameter("componentId", componentId);
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultInterpretation> getActiveByComponentId(String componentId) {
        String hql = "from TestResultInterpretation i where i.componentId = :componentId and i.isActive = 'Y'"
                + " order by i.displayOrder";
        Query<TestResultInterpretation> query = entityManager.unwrap(Session.class).createQuery(hql,
                TestResultInterpretation.class);
        query.setParameter("componentId", componentId);
        return query.list();
    }
}
