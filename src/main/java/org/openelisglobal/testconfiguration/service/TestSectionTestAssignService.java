package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

public interface TestSectionTestAssignService {

    void updateTestAndTestSections(Test test, TestSection testSection, TestSection deActivateTestSection,
            boolean updateTestSection);
}
