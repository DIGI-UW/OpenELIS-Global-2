package org.openelisglobal.dashboard.management.surveillance.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.dashboard.management.surveillance.dao.TestSurveillanceMappingDAO;
import org.openelisglobal.dashboard.management.surveillance.valueholder.TestSurveillanceMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestSurveillanceMappingDAOImpl extends BaseDAOImpl<TestSurveillanceMapping, Long>
        implements TestSurveillanceMappingDAO {

    public TestSurveillanceMappingDAOImpl() {
        super(TestSurveillanceMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> getActiveTestIds(String test) {
        List<Number> ids = entityManager.createNativeQuery("SELECT t.id FROM clinlims.test t"
                + " LEFT JOIN clinlims.test_section ts ON ts.id = t.test_section_id"
                + " WHERE t.is_active = 'Y' AND COALESCE(ts.domain, '') <> 'VECTOR'"
                + " AND (t.loinc = :test OR lower(t.name) = lower(:test) OR lower(t.description) = lower(:test))"
                + " ORDER BY t.id").setParameter("test", test).getResultList();
        return ids.stream().map(id -> String.valueOf(id.longValue())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TestSurveillanceMapping find(Long programId, String testId, String metricKey) {
        return entityManager.unwrap(Session.class)
                .createQuery("from TestSurveillanceMapping m where m.programId = :programId"
                        + " and m.testId = :testId and m.metricKey = :metricKey", TestSurveillanceMapping.class)
                .setParameter("programId", programId).setParameter("testId", testId)
                .setParameter("metricKey", metricKey).uniqueResult();
    }
}
