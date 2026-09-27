package org.openelisglobal.common.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.dao.ReflexRuleDAO;
import org.openelisglobal.testreflex.service.TestReflexServiceImpl;
import org.openelisglobal.testreflex.valueholder.TestReflex;

/**
 * A deactivated reflex rule generates nothing.
 *
 * <p>
 * Deactivating a rule deletes its executable rows and reactivating writes them
 * back, so the presence of a row looks like the switch — but saving a rule
 * rebuilds its rows whatever its state, so a deactivated rule could keep a full
 * set of them and go on adding tests. The switch is the flag on the rule, and
 * it is read at execution: it can change between a row being written and the
 * next result arriving, and the answer has to be the current one, not the one
 * that held when the row was made.
 *
 * <p>
 * This is the state check only. Whether a result is the measurement the rule
 * names is a separate question, answered by {@link RuleResultScope} and
 * unchanged.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReflexRuleActivationTest {

    private static final String RULE_ANALYTE = "283";

    @Mock
    private ReflexRuleDAO reflexRuleDAO;

    @InjectMocks
    private TestReflexServiceImpl service;

    private TestReflex reflex;

    @Before
    public void setUp() {
        Analyte analyte = new Analyte();
        analyte.setId(RULE_ANALYTE);
        analyte.setAnalyteName("Covid-Reflex");

        TestAnalyte testAnalyte = new TestAnalyte();
        testAnalyte.setId("299");
        testAnalyte.setAnalyte(analyte);

        reflex = new TestReflex();
        reflex.setId("13");
        reflex.setTestAnalyte(testAnalyte);
    }

    private ReflexRule rule(Boolean active) {
        ReflexRule rule = new ReflexRule();
        rule.setRuleName("Covid-Reflex");
        rule.setAnalyteId(Integer.valueOf(RULE_ANALYTE));
        rule.setActive(active);
        return rule;
    }

    @Test
    public void anActiveRuleExecutes() {
        when(reflexRuleDAO.getReflexRuleByAnalyteId(RULE_ANALYTE)).thenReturn(rule(true));

        assertTrue(service.isReflexRuleActive(reflex));
    }

    @Test
    public void aDeactivatedRuleDoesNotExecuteEvenThoughItsRowSurvives() {
        // The row is right here, fully configured. The rule behind it is off.
        when(reflexRuleDAO.getReflexRuleByAnalyteId(RULE_ANALYTE)).thenReturn(rule(false));

        assertFalse(service.isReflexRuleActive(reflex));
    }

    @Test
    public void deactivatingThenReactivatingIsFollowedOnTheNextResult() {
        // Each result asks again, so a rule switched off after it last fired
        // stops, and one switched back on resumes — no restart, no cache to
        // clear.
        when(reflexRuleDAO.getReflexRuleByAnalyteId(RULE_ANALYTE)).thenReturn(rule(true), rule(false), rule(true));

        assertTrue("active", service.isReflexRuleActive(reflex));
        assertFalse("deactivated", service.isReflexRuleActive(reflex));
        assertTrue("reactivated", service.isReflexRuleActive(reflex));
    }

    @Test
    public void aRuleWithNoFlagSetIsTreatedAsOff() {
        // Nothing writes NULL today, but "not switched on" is the safe reading of
        // a rule that does not say it is.
        when(reflexRuleDAO.getReflexRuleByAnalyteId(RULE_ANALYTE)).thenReturn(rule(null));

        assertFalse(service.isReflexRuleActive(reflex));
    }

    @Test
    public void aRowWithNoRuleBehindItKeepsWorking() {
        // The reflexes seeded before the rule builder existed have no rule to
        // consult, and neither do the ones the user-choice path builds in memory.
        lenient().when(reflexRuleDAO.getReflexRuleByAnalyteId(RULE_ANALYTE)).thenReturn(null);

        assertTrue("no rule owns this row", service.isReflexRuleActive(reflex));
        assertTrue("no analyte to look a rule up by", service.isReflexRuleActive(new TestReflex()));
        assertTrue(service.isReflexRuleActive(null));
    }
}
