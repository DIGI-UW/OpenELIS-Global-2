package org.openelisglobal.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestService testService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test.xml");
    }

    @Test
    public void getAllTests_ShouldReturnAllTests() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAllTests(false);
        assertEquals("Blood Test", tests.get(0).getDescription());
        assertEquals("Urine Test", tests.get(1).getDescription());
    }

    @Test
    public void getData_shouldReturnDataGivenTest() {
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        testService.getData(test1);
        assertEquals("Blood Test", test1.getDescription());
        assertEquals("therapy", test1.getMethod().getMethodName());

        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        testService.getData(test2);
        assertEquals("Urine Test", test2.getDescription());
        assertEquals("imagining", test2.getMethod().getMethodName());
    }

    @Test
    public void getActiveTestById_ShouldReturnActiveTest() {
        org.openelisglobal.test.valueholder.Test test = testService.getActiveTestById(1);
        assertEquals("Y", test.getIsActive());
    }

    @Test
    public void getTotalTestCount_ShouldReturnTotalTestCount() {
        List<org.openelisglobal.test.valueholder.Test> tests = testService.getAll();
        int totalTestCount = testService.getTotalTestCount();
        assertEquals(tests.size(), totalTestCount);
    }

}
