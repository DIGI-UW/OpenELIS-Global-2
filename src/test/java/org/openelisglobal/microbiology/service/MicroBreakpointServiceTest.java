package org.openelisglobal.microbiology.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

@RunWith(MockitoJUnitRunner.class)
public class MicroBreakpointServiceTest {

    @Mock
    private MicroBreakpointStandardDAO standardDAO;

    @Mock
    private MicroBreakpointRuleDAO ruleDAO;

    @Test
    public void getActiveStandardUsesAuthorityAndVersion() {
        MicroBreakpointStandard standard = new MicroBreakpointStandard();
        when(standardDAO.getActiveStandard("CLSI", "2026")).thenReturn(standard);

        MicroBreakpointService service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO);

        org.junit.Assert.assertTrue(standard == service.getActiveStandard("CLSI", "2026"));
    }

    @Test
    public void findBreakpointRuleDelegatesTheMatchingDimensions() {
        MicroBreakpointRule rule = new MicroBreakpointRule();
        when(ruleDAO.findBestRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC")).thenReturn(rule);

        MicroBreakpointService service = new MicroBreakpointServiceImpl(standardDAO, ruleDAO);

        org.junit.Assert.assertTrue(
                rule == service.findBreakpointRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC"));
        verify(ruleDAO).findBestRule("std", "org", "Enterobacterales", "abx", "MIC", "7", "MIC");
    }
}
