package org.openelisglobal.dashboard.management.surveillance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.dashboard.management.surveillance.valueholder.TestSurveillanceMapping;

public interface TestSurveillanceMappingDAO extends BaseDAO<TestSurveillanceMapping, Long> {

    /**
     * Active, non-vector tests matching {@code test} by LOINC, name or description.
     */
    List<String> getActiveTestIds(String test);

    TestSurveillanceMapping find(Long programId, String testId, String metricKey);
}
