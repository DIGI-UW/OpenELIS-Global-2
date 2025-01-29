package org.openelisglobal.testconfiguration.service;

import java.util.List;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestSet;

public interface TestModifyService {

    void updateTestSets(List<TestSet> testSets, TestAddParams testAddParams, Localization nameLocalization,
            Localization reportingNameLocalization, String currentUserId);

    void updateTestSetsRest(
            List<org.openelisglobal.testconfiguration.controller.rest.TestModifyEntryRestController.TestSet> testSets,
            org.openelisglobal.testconfiguration.controller.rest.TestModifyEntryRestController.TestAddParams testAddParams,
            Localization nameLocalization, Localization reportingNameLocalization, String currentUserId);
}
