package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestSectionTestAssignService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void updateTestAndTestSections(Test test, TestSection testSection, TestSection deActivateTestSection,
            boolean updateTestSection);

    /**
     * Move the given tests into the target lab unit (test section) in one
     * transaction. Activates the target section if it was inactive (a section with
     * tests is orderable — mirrors the single-test assign flow). Throws
     * LIMSRuntimeException if the section or any test id is unknown.
     *
     * @return the updated tests
     */
    List<Test> assignTestsToSection(List<String> testIds, String targetSectionId, String sysUserId);
}
