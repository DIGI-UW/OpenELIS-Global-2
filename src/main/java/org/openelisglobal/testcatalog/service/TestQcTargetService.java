package org.openelisglobal.testcatalog.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;

public interface TestQcTargetService extends BaseObjectService<TestQcTarget, String> {

    /**
     * Every target of a test, active and deactivated, level defaults before lot
     * overrides.
     */
    List<TestQcTarget> getByTestId(String testId);

    /**
     * Upserts the given targets: a row whose id matches an existing target of the
     * test is updated in place, one without is inserted. Rows not in the list are
     * left alone — targets are deactivated through {@code active}, never deleted. A
     * deactivated row sent with no values keeps the values it holds (OGC-1462).
     * Returns the test's full target list afterwards.
     */
    List<TestQcTarget> saveTargetsForTest(String testId, List<TestQcTarget> desired, String sysUserId);

    /**
     * Deactivates the test's active targets that do not fit its result type: one
     * without an expected value and uncertainty on a numeric test, or without an
     * expected answer on a coded test. Their values are kept (OGC-1462).
     */
    void retireTargetsNotFitting(String testId, boolean quantitative, String sysUserId);

    /**
     * The target Results Entry control capture prefills from (FR-B4 precedence):
     * the active override for the given lot, else the active level default, else
     * null. {@code componentId} null means the primary component.
     */
    TestQcTarget resolveEffectiveTarget(String testId, String componentId, String controlLevel, String qcControlLotId);
}
