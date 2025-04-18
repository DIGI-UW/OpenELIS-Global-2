package org.openelisglobal.TestReflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflexServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService testReflexService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/testreflex.xml");
    }

    @Test
    public void g0etAll_ShouldReturnAllTestReflexes() {
        List<TestReflex> reflexes = testReflexService.getAllTestReflexs();
        assertNotNull(reflexes);
        assertFalse(reflexes.isEmpty());
        assertEquals(3, reflexes.size());
    }

    @Test
    public void getByTestAnalyteId_ShouldReturnMatchingReflexes() {
        List<TestReflex> reflexes = testReflexService.getTestReflexsByTestAnalyteId("101");
        assertNotNull(reflexes);
        assertFalse(reflexes.isEmpty());
        assertEquals(2, reflexes.size());
    }

//    @Test
//    public void getByTestResult_ShouldReturnMatchingReflexes() {
//        TestResult testResult = testResultService.getById("201"); // use service or DAO to get real entity
//
//        List<TestReflex> reflexes = testReflexService.getTestReflexesByTestResult(testResult);
//        assertNotNull(reflexes);
//        assertFalse(reflexes.isEmpty());
//        assertEquals(1, reflexes.size());
//        assertEquals("201", reflexes.get(0).getTestResultId());
//    }
//
//    @Test
//    public void getByTestAndFlag_ShouldReturnFlaggedReflexes() {
//        Test test = testService.getById("301"); // again, get from service not create manually
//
//        List<TestReflex> reflexes = testReflexService.getTestReflexsByTestAndFlag(test);
//        assertNotNull(reflexes);
//        assertFalse(reflexes.isEmpty());
//        assertEquals(1, reflexes.size());
//        assertEquals("301", reflexes.get(0).getTestId());
//    }
    @Test
    public void getByAnalyteAndTest_ShouldReturnMatchingReflexes() {
        List<TestReflex> reflexes = testReflexService.getTestReflexsByAnalyteAndTest("401", "402");
        assertNotNull(reflexes);
        assertFalse(reflexes.isEmpty());
        assertEquals("401", reflexes.get(0).getTestAnalyteId());
        assertEquals("402", reflexes.get(0).getTestId());
    }

//    @Test
//    public void update_ShouldModifyExistingReflex() {
//        List<TestReflex> reflexes = testReflexService.getTestReflexesByTestResult("201");
//        assertFalse(reflexes.isEmpty());
//
//        TestReflex reflex = reflexes.get(0);
//        String originalNote = reflex.getInternalNote();
//
//        reflex.setInternalNote("Updated note");
//        TestReflex updatedReflex = testReflexService.update(reflex);
//
//        assertNotNull(updatedReflex);
//        assertEquals("Updated note", updatedReflex.getInternalNote());
//
//        // Optional: check against old note if stored elsewhere
//        assertFalse(originalNote.equals(updatedReflex.getInternalNote()));
//    }
//
//    @Test
//    public void delete_ShouldRemoveReflex() {
//        List<TestReflex> reflexes = testReflexService.getTestReflexesByTestResult("201");
//        assertFalse(reflexes.isEmpty());
//
//        TestReflex reflex = reflexes.get(0);
//        testReflexService.delete(reflex);
//
//        reflexes = testReflexService.getTestReflexesByTestResult("201");
//        assertTrue(reflexes.isEmpty());
//    }
}
