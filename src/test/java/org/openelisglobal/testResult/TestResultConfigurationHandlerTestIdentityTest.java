package org.openelisglobal.testResult;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultConfigurationHandler;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A results row names its test the way the tests file does: by description. A
 * legacy test whose English display name is the same word (Hémoglobine(Whole
 * Blood), shown as "Hemoglobin") must not take the row.
 */
public class TestResultConfigurationHandlerTestIdentityTest {

    private DisplayListService previousDisplayListService;
    private TestService testService;
    private TestResultService testResultService;
    private TestResultConfigurationHandler handler;

    @Before
    public void setUp() {
        previousDisplayListService = (DisplayListService) ReflectionTestUtils.getField(DisplayListService.class,
                "instance");
        ReflectionTestUtils.setField(DisplayListService.class, "instance", mock(DisplayListService.class));

        testService = mock(TestService.class);
        testResultService = mock(TestResultService.class);
        handler = new TestResultConfigurationHandler();
        ReflectionTestUtils.setField(handler, "testService", testService);
        ReflectionTestUtils.setField(handler, "testResultService", testResultService);
        ReflectionTestUtils.setField(handler, "dictionaryService", mock(DictionaryService.class));
        ReflectionTestUtils.setField(handler, "testResultComponentService", mock(TestResultComponentService.class));
        when(testResultService.insert(any(TestResult.class))).thenReturn("100");
        when(testResultService.getActiveTestResultsByTest(any())).thenReturn(Collections.emptyList());
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(DisplayListService.class, "instance", previousDisplayListService);
    }

    private static org.openelisglobal.test.valueholder.Test test(String id, String description) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setDescription(description);
        return test;
    }

    @Test
    public void rowGoesToTheTestWithThatDescription_notALegacyTestSharingItsDisplayName() throws Exception {
        org.openelisglobal.test.valueholder.Test legacy = test("15", "Hémoglobine(Whole Blood)");
        org.openelisglobal.test.valueholder.Test hemoglobin = test("402", "Hemoglobin");
        when(testService.getTestByLocalizedName("Hemoglobin")).thenReturn(legacy);
        when(testService.getTestByDescription("Hemoglobin")).thenReturn(hemoglobin);

        handler.processConfiguration(
                new ByteArrayInputStream(
                        "testName,resultType,significantDigits\nHemoglobin,N,0\n".getBytes(StandardCharsets.UTF_8)),
                "analyzer-harness-results.csv");

        ArgumentCaptor<TestResult> created = ArgumentCaptor.forClass(TestResult.class);
        verify(testResultService, times(1)).insert(created.capture());
        assertEquals("402", created.getValue().getTest().getId());
    }

    @Test
    public void displayNameStillFindsATestWhoseDescriptionDiffers() throws Exception {
        org.openelisglobal.test.valueholder.Test legacy = test("15", "Hémoglobine(Whole Blood)");
        when(testService.getTestByLocalizedName("Hemoglobin")).thenReturn(legacy);

        handler.processConfiguration(
                new ByteArrayInputStream(
                        "testName,resultType,significantDigits\nHemoglobin,N,0\n".getBytes(StandardCharsets.UTF_8)),
                "legacy-results.csv");

        ArgumentCaptor<TestResult> created = ArgumentCaptor.forClass(TestResult.class);
        verify(testResultService, times(1)).insert(created.capture());
        assertEquals("15", created.getValue().getTest().getId());
    }
}
