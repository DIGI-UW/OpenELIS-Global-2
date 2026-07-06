package org.openelisglobal.testcatalog.service;

import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.service.LocalizationServiceImpl.LocalizationType;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCatalogCreationServiceImpl implements TestCatalogCreationService {

    @Autowired
    private TestService testService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    @Transactional(readOnly = true)
    public boolean codeInUse(String code) {
        if (GenericValidator.isBlankOrNull(code)) {
            return false;
        }
        for (Test test : testService.getAllTests(false)) {
            if (code.equalsIgnoreCase(test.getLocalCode())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public String createInactiveTest(CreateTestParams params, String sysUserId) {
        Localization nameLocalization = LocalizationServiceImpl.createNewLocalization(params.name, params.name,
                LocalizationType.TEST_NAME);
        nameLocalization.setSysUserId(sysUserId);
        String nameLocalizationId = localizationService.insert(nameLocalization);

        String reportingName = GenericValidator.isBlankOrNull(params.reportingName) ? params.name
                : params.reportingName;
        Localization reportingLocalization = LocalizationServiceImpl.createNewLocalization(reportingName, reportingName,
                LocalizationType.REPORTING_TEST_NAME);
        reportingLocalization.setSysUserId(sysUserId);
        String reportingLocalizationId = localizationService.insert(reportingLocalization);

        Test test = new Test();
        test.setLocalizedTestName(localizationService.get(nameLocalizationId));
        test.setLocalizedReportingName(localizationService.get(reportingLocalizationId));
        // description doubles as the legacy base name; default it to the name.
        test.setDescription(GenericValidator.isBlankOrNull(params.description) ? params.name : params.description);
        test.setLocalCode(params.code);
        test.setDomain(params.domain);
        if (!GenericValidator.isBlankOrNull(params.labUnitId)) {
            TestSection labUnit = testSectionService.get(params.labUnitId);
            if (labUnit != null) {
                test.setTestSection(labUnit);
            }
        }
        // New tests start Inactive so they aren't orderable until configured (FR-3).
        test.setIsActive("N");
        test.setOrderable(Boolean.TRUE.equals(params.orderable));
        test.setAntimicrobialResistance(Boolean.TRUE.equals(params.amr));
        test.setIsReportable("N");
        test.setGuid(UUID.randomUUID().toString());
        test.setSortOrder("0");
        test.setSysUserId(sysUserId);
        String testId = testService.insert(test);

        if (!GenericValidator.isBlankOrNull(params.sampleTypeId)) {
            TypeOfSampleTest sampleTypeLink = new TypeOfSampleTest();
            sampleTypeLink.setTypeOfSampleId(params.sampleTypeId);
            sampleTypeLink.setTestId(testId);
            sampleTypeLink.setSysUserId(sysUserId);
            typeOfSampleTestService.insert(sampleTypeLink);
        }
        return testId;
    }
}
