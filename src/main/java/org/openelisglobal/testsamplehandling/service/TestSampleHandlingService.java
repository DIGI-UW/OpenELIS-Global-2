package org.openelisglobal.testsamplehandling.service;

import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;

public interface TestSampleHandlingService extends BaseObjectService<TestSampleHandling, String> {

    /**
     * The single storage/handling config for a test, or null if none exists yet.
     */
    TestSampleHandling getByTestId(String testId);

    /**
     * OGC-949 M8: upsert a test's storage/handling config (singleton per test).
     * Inserts when none exists, otherwise updates the existing row in place. The
     * app-level config {@code version} counter is bumped on each save (for the v2
     * audit trail). Returns the persisted row.
     */
    TestSampleHandling saveForTest(String testId, TestSampleHandling desired, String sysUserId);

    /**
     * {@link #saveForTest} for several tests in one transaction: either every test
     * gets its storage or none does (the group editor's Storage save).
     */
    void saveForTests(Map<String, TestSampleHandling> desiredByTestId, String sysUserId);
}
