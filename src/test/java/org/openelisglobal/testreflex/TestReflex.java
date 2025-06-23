package org.openelisglobal.testreflex;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflex extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService reflexService;
    @Autowired
    private TestReflexService testReflexService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void testGetReflexRuleByAnalyteId() {
        ReflexRule reflexRule = testReflexService.getReflexRuleByAnalyteId("3002");
    }

}
