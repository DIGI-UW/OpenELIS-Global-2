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

    @Test
    public void getByAnalyteAndTest_ShouldReturnMatchingReflexes() {
        List<TestReflex> reflexes = testReflexService.getTestReflexsByAnalyteAndTest("401", "402");
        assertNotNull(reflexes);
        assertFalse(reflexes.isEmpty());
        assertEquals("401", reflexes.get(0).getTestAnalyteId());
        assertEquals("402", reflexes.get(0).getTestId());
    }

}
