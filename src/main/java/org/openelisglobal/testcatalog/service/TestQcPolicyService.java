package org.openelisglobal.testcatalog.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;

/** OGC-1200 FR-H1: the QC policy each run of a test is held to. */
public interface TestQcPolicyService {

    /**
     * The test's policy, or the empty one (nothing required) when none is stored.
     */
    TestQcPolicy policyOf(String testId);

    /**
     * The policies of the given tests, each test present, defaulted when none is
     * stored.
     */
    Map<String, TestQcPolicy> policiesOf(Collection<String> testIds);

    /**
     * Saves the Test Catalog QC section in one transaction: the targets, and the
     * policy when one is given (an older client that sends none leaves it as it
     * is). A changed policy re-evaluates the open and held runs of the test, so
     * their state and holds follow it at once.
     */
    List<TestQcTarget> saveSection(String testId, List<TestQcTarget> targets, TestQcPolicy policy, String sysUserId);
}
