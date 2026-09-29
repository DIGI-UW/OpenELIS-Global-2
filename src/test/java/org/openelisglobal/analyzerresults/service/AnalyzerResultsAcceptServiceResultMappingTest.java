package org.openelisglobal.analyzerresults.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class AnalyzerResultsAcceptServiceResultMappingTest {

    @Autowired
    private AnalyzerResultsAcceptService acceptService;

    private TestResultService testResultService;
    private AnalyzerResultsAcceptServiceImpl service;

    @Before
    public void setUp() {
        testResultService = mock(TestResultService.class);
        service = new AnalyzerResultsAcceptServiceImpl(mock(TypeOfSampleService.class));
        ReflectionTestUtils.setField(service, "testResultService", testResultService);
    }

    @Test
    public void resolvesDictionaryOptionByStableIdAcrossServiceLoads() {
        TestResult detected = option("638", "1378", "component-mtb");
        TestResult notDetected = option("639", "1379", "component-mtb");
        TestResult separatelyLoadedNotDetected = option("639", "1379", "component-mtb");

        when(testResultService.getActiveTestResultsByTest("395")).thenReturn(List.of(detected, notDetected));
        when(testResultService.getTestResultsByTestAndDictonaryResult("395", "1379"))
                .thenReturn(separatelyLoadedNotDetected);

        // acceptService is now @PreAuthorize-gated, so Spring hands back a JDK
        // proxy that cannot be cast to the impl. Unwrap to the target before
        // reflecting on its fields.
        AnalyzerResultsAcceptServiceImpl service = AopTestUtils.getTargetObject(acceptService);
        TestResultService original = (TestResultService) ReflectionTestUtils.getField(service, "testResultService");
        ReflectionTestUtils.setField(service, "testResultService", testResultService);
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("395");
        item.setComponentId("component-mtb");
        item.setResult("1379");

        assertEquals("639", service.getTestResultForResult(item).getId());
    }

    @Test
    public void absentTestResultsRemainUnresolved() {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("missing");
        when(testResultService.getActiveTestResultsByTest("missing")).thenReturn(null);
        assertNull(service.getTestResultForResult(item));
    }

    @Test
    public void emptyTestResultsRemainUnresolved() {
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("empty");
        when(testResultService.getActiveTestResultsByTest("empty")).thenReturn(List.of());
        assertNull(service.getTestResultForResult(item));
    }

    @Test
    public void dictionaryMatchFromAnotherComponentCannotOverrideSelectedComponent() {
        TestResult selected = option("638", "1378", "component-mtb");
        TestResult otherComponent = option("639", "1379", "component-rif");
        when(testResultService.getActiveTestResultsByTest("395")).thenReturn(List.of(otherComponent, selected));
        when(testResultService.getTestResultsByTestAndDictonaryResult("395", "1379")).thenReturn(otherComponent);
        AnalyzerResultItem item = new AnalyzerResultItem();
        item.setTestId("395");
        item.setComponentId("component-mtb");
        item.setResult("1379");
        assertEquals("638", service.getTestResultForResult(item).getId());
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
