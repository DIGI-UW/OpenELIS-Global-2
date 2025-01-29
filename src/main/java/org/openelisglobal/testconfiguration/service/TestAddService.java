package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;

public interface TestAddService {

    void addTests(List<TestSet> testSets, Localization nameLocalization, Localization reportingNameLocalization,
            String currentUserId);

    void addTestsRest(List<org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.TestSet> testSets,
            Localization nameLocalization, Localization reportingNameLocalization, String currentUserId);
}
