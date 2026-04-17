package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestSectionTestAssignService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void updateTestAndTestSections(Test test, TestSection testSection, TestSection deActivateTestSection,
            boolean updateTestSection);
}
