package org.openelisglobal.testsamplehandling.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testsamplehandling.dao.TestSampleHandlingDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSampleHandlingServiceImpl extends AuditableBaseObjectServiceImpl<TestSampleHandling, String>
        implements TestSampleHandlingService {

    @Autowired
    protected TestSampleHandlingDAO baseObjectDAO;

    TestSampleHandlingServiceImpl() {
        super(TestSampleHandling.class);
    }

    @Override
    protected TestSampleHandlingDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TestSampleHandling getByTestId(String testId) {
        List<TestSampleHandling> matches = getAllMatching("testId", testId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    @Transactional
    public TestSampleHandling saveForTest(String testId, TestSampleHandling desired, String sysUserId) {
        TestSampleHandling existing = getByTestId(testId);
        TestSampleHandling target = existing != null ? existing : new TestSampleHandling();
        target.setTestId(testId);
        target.setStorageCondition(desired.getStorageCondition());
        target.setStorageConditionCustom(desired.getStorageConditionCustom());
        target.setStorageDuration(desired.getStorageDuration());
        target.setStorageDurationUnit(desired.getStorageDurationUnit());
        target.setStabilityNotes(desired.getStabilityNotes());
        target.setProtectFromLight(desired.getProtectFromLight());
        target.setDoNotFreeze(desired.getDoNotFreeze());
        target.setDoNotRefrigerate(desired.getDoNotRefrigerate());
        target.setDisposalMethod(desired.getDisposalMethod());
        target.setDisposalTimeframe(desired.getDisposalTimeframe());
        target.setDisposalUnit(desired.getDisposalUnit());
        target.setSpecialInstructions(desired.getSpecialInstructions());
        target.setOverrideRestricted(desired.getOverrideRestricted());
        // Bump the app-level config-version counter for the v2 audit trail.
        int prior = existing != null && existing.getVersion() != null ? existing.getVersion() : 0;
        target.setVersion(prior + 1);
        target.setIsActive("Y");
        target.setSysUserId(sysUserId);
        if (existing != null) {
            update(target);
        } else {
            insert(target);
        }
        return target;
    }
}
