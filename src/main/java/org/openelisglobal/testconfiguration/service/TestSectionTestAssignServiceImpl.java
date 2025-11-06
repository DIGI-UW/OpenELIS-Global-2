package org.openelisglobal.testconfiguration.service;

import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSectionTestAssignServiceImpl implements TestSectionTestAssignService {

    @Autowired
    private TestService testService;
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional
    public void updateTestAndTestSections(Test test, TestSection testSection, TestSection deActivateTestSection,
            boolean updateTestSection) {
        testService.update(test);

        if (updateTestSection) {
            testSectionService.update(testSection);
        }

        if (deActivateTestSection != null) {
            testSectionService.update(deActivateTestSection);
        }
    }
}
