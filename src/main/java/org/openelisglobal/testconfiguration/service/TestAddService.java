package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestAddService {

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void addTests(List<TestSet> testSets, Localization nameLocalization, Localization reportingNameLocalization,
            String currentUserId);
}
