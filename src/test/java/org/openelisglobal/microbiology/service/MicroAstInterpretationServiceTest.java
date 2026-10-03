package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;

public class MicroAstInterpretationServiceTest {

    private MicroAstInterpretationService service;
    private MicroBreakpointRule rule;

    @Before
    public void setUp() {
        service = new MicroAstInterpretationServiceImpl();
        rule = new MicroBreakpointRule();
        rule.setSusceptibleValue(new BigDecimal("8"));
        rule.setIntermediateLowerValue(new BigDecimal("16"));
        rule.setIntermediateUpperValue(new BigDecimal("16"));
        rule.setResistantValue(new BigDecimal("32"));
    }

    @Test
    public void micInterpretationUsesLowValueAsSusceptible() {
        assertEquals(MicroAstInterpretation.SUSCEPTIBLE,
                service.interpret(rule, MicroAstMethod.MIC, new BigDecimal("4")));
        assertEquals(MicroAstInterpretation.INTERMEDIATE,
                service.interpret(rule, MicroAstMethod.MIC, new BigDecimal("16")));
        assertEquals(MicroAstInterpretation.RESISTANT,
                service.interpret(rule, MicroAstMethod.MIC, new BigDecimal("64")));
    }

    @Test
    public void zoneInterpretationUsesHighValueAsSusceptible() {
        MicroBreakpointRule zoneRule = new MicroBreakpointRule();
        zoneRule.setSusceptibleValue(new BigDecimal("24"));
        zoneRule.setIntermediateLowerValue(new BigDecimal("16"));
        zoneRule.setIntermediateUpperValue(new BigDecimal("20"));
        zoneRule.setResistantValue(new BigDecimal("12"));

        assertEquals(MicroAstInterpretation.SUSCEPTIBLE,
                service.interpret(zoneRule, MicroAstMethod.ZONE, new BigDecimal("32")));
        assertEquals(MicroAstInterpretation.INTERMEDIATE,
                service.interpret(zoneRule, MicroAstMethod.ZONE, new BigDecimal("16")));
        assertEquals(MicroAstInterpretation.RESISTANT,
                service.interpret(zoneRule, MicroAstMethod.ZONE, new BigDecimal("4")));
    }

    @Test
    public void missingBreakpointReturnsNoBreakpoint() {
        assertEquals(MicroAstInterpretation.NO_BREAKPOINT,
                service.interpret(null, MicroAstMethod.MIC, new BigDecimal("4")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void overrideRequiresReason() {
        service.validateOverride(MicroAstInterpretation.RESISTANT, " ");
    }
}
