package testresult;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresult.service.TestResultService;
import org.springframework.beans.factory.annotation.Autowired;


public class TestResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestResultService testResultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-result.xml");
    }

    @Test
    public void testGetAllTestResults() {
        List<TestResult> results = testResultService.getAllTestResults();
        assertNotNull("Test result list should not be null", results);
        assertEquals("Should return 3 test results", 3, results.size());
    }

    @Test
    public void testGetActiveTestResultsByTest() {
        List<TestResult> results = testResultService.getActiveTestResultsByTest("1");
        assertNotNull("Active test result list should not be null", results);
        assertEquals("Should return 2 active test results for test id 1", 2, results.size());
    }

    @Test
    public void testGetAllActiveTestResultsPerTest() {
        org.openelisglobal.test.valueholder.Test test = Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
        Mockito.when(test.getId()).thenReturn("1");
        List<TestResult> results = testResultService.getAllActiveTestResultsPerTest(test);

        assertNotNull("Active test result list per test should not be null", results);
        assertEquals("Should return 2 active test results for test id 1", 2, results.size());
    }

}
