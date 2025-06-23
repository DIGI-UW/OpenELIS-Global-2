package org.openelisglobal.testreflex;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflexServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService testReflexService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void testGetReflexRuleByAnalyteId() {

        // This method fails to return the reflex rule as expected.
        ReflexRule reflexRule = testReflexService.getReflexRuleByAnalyteId("3002");

        // Assertions here;
    }

}
