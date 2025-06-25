package org.openelisglobal.testreflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflexServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService testReflexService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void testGetAllTestReflexs() {
        List<TestReflex> testReflexs = testReflexService.getAllTestReflexs();
        assertNotNull(testReflexs);
        assertEquals(3, testReflexs.size());
        assertEquals("1002", testReflexs.get(2).getId());

    }

    @Test
    public void testGetReflexRuleByAnalyteId() {

        // There are no datasets for a Reflex rule because
        // there's no table in the DB called Reflex Rule

        // This method fails to return the reflex rule as expected.
        ReflexRule reflexRule = testReflexService.getReflexRuleByAnalyteId("4002");

        // Assertions here;
        assertNotNull(reflexRule);
        assertEquals("4002", reflexRule.getId().toString());

        // So it's hard to create assertions because we don't know what
        // supposed to come from the DB!

    }
}
