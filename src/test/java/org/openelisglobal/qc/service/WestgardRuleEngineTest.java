package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class WestgardRuleEngineTest {

    private WestgardRuleEngine engine;
    private BigDecimal mean;
    private BigDecimal sd;
    private List<String> allRules;

    @Before
    public void setUp() {
        engine = new WestgardRuleEngine();
        mean = new BigDecimal("100");
        sd = new BigDecimal("5");
        allRules = WestgardRuleEngine.ALL_RULE_CODES;
    }

    @Test
    public void testRule12s_Violation() {
        // Value > mean + 2SD (110 > 100 + 10)
        List<BigDecimal> values = List.of(new BigDecimal("110.1"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_12S));
    }

    @Test
    public void testRule12s_NoViolation() {
        // Value within 2SD
        List<BigDecimal> values = List.of(new BigDecimal("108"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_12S));
    }

    @Test
    public void testRule13s_Violation() {
        // Value > mean + 3SD (115 > 100 + 15)
        List<BigDecimal> values = List.of(new BigDecimal("115.1"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_13S));
    }

    @Test
    public void testRule13s_NoViolation() {
        // Value within 3SD but beyond 2SD
        List<BigDecimal> values = List.of(new BigDecimal("114"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_13S));
    }

    @Test
    public void testRule22s_Violation() {
        // Two consecutive values above mean + 2SD (same side)
        List<BigDecimal> values = List.of(new BigDecimal("111"), new BigDecimal("112"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_22S));
    }

    @Test
    public void testRule22s_NoViolation_DifferentSides() {
        // Two values beyond 2SD but on different sides
        List<BigDecimal> values = List.of(new BigDecimal("111"), new BigDecimal("89"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_22S));
    }

    @Test
    public void testRuleR4s_Violation() {
        // Range between consecutive values > 4SD (20 > 20)
        List<BigDecimal> values = List.of(new BigDecimal("89"), new BigDecimal("111"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_R4S));
    }

    @Test
    public void testRuleR4s_NoViolation() {
        // Range = 10 < 4*5=20
        List<BigDecimal> values = List.of(new BigDecimal("95"), new BigDecimal("105"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_R4S));
    }

    @Test
    public void testRule41s_Violation() {
        // Four consecutive values on same side beyond 1SD
        List<BigDecimal> values = List.of(new BigDecimal("106"), new BigDecimal("107"), new BigDecimal("108"),
                new BigDecimal("109"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_41S));
    }

    @Test
    public void testRule41s_NoViolation_MixedSides() {
        List<BigDecimal> values = List.of(new BigDecimal("106"), new BigDecimal("94"), new BigDecimal("108"),
                new BigDecimal("109"));
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_41S));
    }

    @Test
    public void testRule10x_Violation() {
        // 10 consecutive values on same side of mean
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            values.add(new BigDecimal("101"));
        }
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertTrue(violations.contains(WestgardRuleEngine.RULE_10X));
    }

    @Test
    public void testRule10x_NoViolation_TooFew() {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            values.add(new BigDecimal("101"));
        }
        List<String> violations = engine.evaluate(values, mean, sd, allRules);
        assertFalse(violations.contains(WestgardRuleEngine.RULE_10X));
    }

    @Test
    public void testEmptyValues() {
        List<String> violations = engine.evaluate(List.of(), mean, sd, allRules);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testNullValues() {
        List<String> violations = engine.evaluate(null, mean, sd, allRules);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testZeroSD() {
        List<BigDecimal> values = List.of(new BigDecimal("110"));
        List<String> violations = engine.evaluate(values, mean, BigDecimal.ZERO, allRules);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testDisabledRules() {
        // Only enable 12s — value should only trigger 12s, not 13s
        List<BigDecimal> values = List.of(new BigDecimal("116"));
        List<String> violations = engine.evaluate(values, mean, sd, List.of(WestgardRuleEngine.RULE_12S));
        assertTrue(violations.contains(WestgardRuleEngine.RULE_12S));
        assertFalse(violations.contains(WestgardRuleEngine.RULE_13S));
    }

    @Test
    public void testGetRuleDescriptions() {
        assertEquals(6, engine.getRuleDescriptions().size());
        assertTrue(engine.getRuleDescriptions().containsKey(WestgardRuleEngine.RULE_12S));
    }
}
