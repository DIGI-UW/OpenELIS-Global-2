package org.openelisglobal.dashboard.management.surveillance.daoimpl;

import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.dashboard.management.surveillance.dao.TestSurveillanceMappingDAO;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingView;
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
        List<Number> ids = entityManager.createNativeQuery("SELECT id FROM clinlims.test WHERE is_active = 'Y'"
                + " AND (loinc = :test OR lower(name) = lower(:test) OR lower(description) = lower(:test))"
                + " ORDER BY id").setParameter("test", test).getResultList();
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

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<SurveillanceMappingView> getAllViews() {
        List<Object[]> rows = entityManager.createNativeQuery("SELECT m.id, p.code, m.test_id, t.description,"
                + " t.loinc, m.metric_label, m.metric_key, m.match_mode, m.positive_result_codes,"
                + " m.denominator_result_codes, m.numeric_threshold, m.display_order, m.is_active"
                + " FROM clinlims.test_surveillance_mapping m"
                + " JOIN clinlims.surveillance_program p ON p.id = m.program_id"
                + " JOIN clinlims.test t ON t.id = m.test_id"
                + " ORDER BY p.display_order, m.display_order, m.metric_key, t.description").getResultList();
        return rows.stream()
                .map(r -> new SurveillanceMappingView(((Number) r[0]).longValue(), (String) r[1],
                        String.valueOf(((Number) r[2]).longValue()), (String) r[3], (String) r[4], (String) r[5],
                        (String) r[6], (String) r[7], (String) r[8], (String) r[9], (BigDecimal) r[10],
                        ((Number) r[11]).intValue(), (Boolean) r[12]))
                .toList();
    }
}
