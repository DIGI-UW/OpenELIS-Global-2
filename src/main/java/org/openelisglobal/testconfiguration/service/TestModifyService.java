package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestSet;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestModifyService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void updateTestSets(List<TestSet> testSets, TestAddParams testAddParams, Localization nameLocalization,
            Localization reportingNameLocalization, String currentUserId);
}
