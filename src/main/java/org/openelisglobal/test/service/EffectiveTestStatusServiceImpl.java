package org.openelisglobal.test.service;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** OGC-189 (M4). See {@link EffectiveTestStatusService}. */
@Service
public class EffectiveTestStatusServiceImpl implements EffectiveTestStatusService {

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional(readOnly = true)
    public boolean isEffectivelyActive(Test test) {
        if (test == null) {
            return false;
        }
        if (!"Y".equals(test.getIsActive())) {
            return false;
        }
        return !isLabUnitInactive(resolveTestSection(test));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEffectivelyActiveById(String testId) {
        if (GenericValidator.isBlankOrNull(testId)) {
            return false;
        }
        return isEffectivelyActive(testService.getTestById(testId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEffectivelyOrderable(Test test) {
        // getOrderable() is a nullable Boolean on legacy rows; null means not
        // orderable, matching how the manual picker already reads it.
        if (test == null || !Boolean.TRUE.equals(test.getOrderable())) {
            return false;
        }
        return isEffectivelyActive(test);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLabUnitInactive(TestSection testSection) {
        // Null-safe by design: a test with no lab unit must not be blocked by a
        // rule about lab units.
        return testSection != null && !"Y".equals(testSection.getIsActive());
    }

    /**
     * The test's lab unit. {@code Test.testSection} is a ValueHolder rather than a
     * Hibernate lazy proxy, so this is safe on a detached instance — but a holder
     * populated by id alone can carry a TestSection whose isActive was never
     * loaded, and reading that as "active" would make the gate quietly do nothing.
     * Re-read by id in that case.
     */
    private TestSection resolveTestSection(Test test) {
        TestSection section = test.getTestSection();
        if (section == null) {
            return null;
        }
        if (section.getIsActive() != null) {
            return section;
        }
        return GenericValidator.isBlankOrNull(section.getId()) ? section : testSectionService.get(section.getId());
    }
}
