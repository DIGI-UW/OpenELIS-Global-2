package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.compliance.dao.ComplianceStandardDAO;
import org.openelisglobal.compliance.form.SuggestedTest;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ParameterGroup;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;

@RunWith(MockitoJUnitRunner.class)
public class ComplianceStandardSuggestedTestsTest {

    @Mock
    private ComplianceStandardDAO standardDAO;

    @Mock
    private TypeOfSampleService typeOfSampleService;

    @Mock
    private TypeOfSampleTestService typeOfSampleTestService;

    @InjectMocks
    private ComplianceStandardServiceImpl service;

    @Test
    public void differentDescriptiveRequirementsConflict() {
        SuggestedTest test = suggestedTest("Absent", "Present");

        assertTrue("Absent and Present disagree", test.conflicting());
        assertEquals(List.of("Absent", "Present"),
                test.thresholds().stream().map(SuggestedTest.Threshold::valueDescriptive).toList());
    }

    @Test
    public void identicalDescriptiveRequirementsDoNotConflict() {
        SuggestedTest test = suggestedTest("Absent", "Absent");

        assertFalse("Both standards require Absent", test.conflicting());
        assertEquals(List.of("Absent", "Absent"),
                test.thresholds().stream().map(SuggestedTest.Threshold::valueDescriptive).toList());
    }

    private SuggestedTest suggestedTest(String firstValue, String secondValue) {
        when(standardDAO.getActiveTestThresholds(List.of("1", "2")))
                .thenReturn(List.of(threshold("1", firstValue), threshold("2", secondValue)));

        List<SuggestedTest> tests = service.suggestedTests(List.of("1", "2"));

        assertEquals(1, tests.size());
        assertEquals(2, tests.get(0).coveredBy().size());
        return tests.get(0);
    }

    private ComplianceThreshold threshold(String standardId, String value) {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setId(standardId);
        standard.setRegulationNumber("REG-" + standardId);
        ParameterGroup group = new ParameterGroup();
        group.setStandard(standard);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("10");
        ComplianceThreshold threshold = new ComplianceThreshold();
        threshold.setGroup(group);
        threshold.setTest(test);
        threshold.setThresholdType(ThresholdType.DESCRIPTIVE);
        threshold.setValueDescriptive(value);
        return threshold;
    }
}
