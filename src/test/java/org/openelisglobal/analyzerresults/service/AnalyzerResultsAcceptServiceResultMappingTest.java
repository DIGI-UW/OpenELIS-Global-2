package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerResultsAcceptServiceResultMappingTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerResultsAcceptService acceptService;

    @Test
    public void resolvesDictionaryOptionByStableIdAcrossServiceLoads() {
        TestResult detected = option("638", "1378", "component-mtb");
        TestResult notDetected = option("639", "1379", "component-mtb");
        TestResult separatelyLoadedNotDetected = option("639", "1379", "component-mtb");

        TestResultService testResultService = mock(TestResultService.class);
        when(testResultService.getActiveTestResultsByTest("395")).thenReturn(List.of(detected, notDetected));
        when(testResultService.getTestResultsByTestAndDictonaryResult("395", "1379"))
                .thenReturn(separatelyLoadedNotDetected);

        AnalyzerResultsAcceptServiceImpl service = (AnalyzerResultsAcceptServiceImpl) acceptService;
        TestResultService original = (TestResultService) ReflectionTestUtils.getField(service, "testResultService");
        ReflectionTestUtils.setField(service, "testResultService", testResultService);
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("395");
        item.setComponentId("component-mtb");
        item.setResult("1379");

        try {
            assertEquals("639", service.getTestResultForResult(item).getId());
        } finally {
            ReflectionTestUtils.setField(service, "testResultService", original);
        }
    }

    private static TestResult option(String id, String value, String componentId) {
        TestResult option = new TestResult();
        option.setId(id);
        option.setValue(value);
        option.setTestResultType("D");
        option.setComponentId(componentId);
        return option;
    }
}
