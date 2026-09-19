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
     * left alone — targets are deactivated through {@code active}, never deleted.
     * Returns the test's full target list afterwards.
     */
    List<TestQcTarget> saveTargetsForTest(String testId, List<TestQcTarget> desired, String sysUserId);

    /**
     * The target Results Entry control capture prefills from (FR-B4 precedence):
     * the active override for the given lot, else the active level default, else
     * null. {@code componentId} null means the primary component.
     */
    TestQcTarget resolveEffectiveTarget(String testId, String componentId, String controlLevel, String qcControlLotId);
}
