package org.openelisglobal.testconfiguration.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
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

    @Override
    @Transactional
    public List<Test> assignTestsToSection(List<String> testIds, String targetSectionId, String sysUserId) {
        TestSection targetSection = testSectionService.getTestSectionById(targetSectionId);
        if (targetSection == null) {
            throw new LIMSRuntimeException("Test section not found: " + targetSectionId);
        }

        List<Test> updated = new ArrayList<>();
        for (String testId : testIds) {
            Test test = testService.get(testId);
            if (test == null) {
                throw new LIMSRuntimeException("Test not found: " + testId);
            }
            if (test.getTestSection() != null && targetSectionId.equals(test.getTestSection().getId())) {
                continue;
            }
            test.setTestSection(targetSection);
            test.setSysUserId(sysUserId);
            testService.update(test);
            updated.add(test);
        }

        if ("N".equals(targetSection.getIsActive())) {
            targetSection.setIsActive("Y");
            targetSection.setSysUserId(sysUserId);
            testSectionService.update(targetSection);
        }

        return updated;
    }
}
