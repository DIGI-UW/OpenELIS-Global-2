package org.openelisglobal.dashboard.management.surveillance.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.dashboard.management.surveillance.service.SurveillanceMappingView;
import org.openelisglobal.dashboard.management.surveillance.valueholder.TestSurveillanceMapping;

public interface TestSurveillanceMappingDAO extends BaseDAO<TestSurveillanceMapping, Long> {

    /** Active tests whose LOINC, name or description equals {@code test}. */
    List<String> getActiveTestIds(String test);

    TestSurveillanceMapping find(Long programId, String testId, String metricKey);

    List<SurveillanceMappingView> getAllViews();
}
