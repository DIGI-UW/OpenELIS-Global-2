package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.springframework.stereotype.Service;

@Service
public class MicroAstInterpretationServiceImpl implements MicroAstInterpretationService {

    @Override
    public MicroAstInterpretation interpret(MicroBreakpointRule rule, MicroAstMethod method, BigDecimal rawValue) {
        if (rule == null || method == null || rawValue == null || rule.getSusceptibleValue() == null
                || rule.getResistantValue() == null) {
            return MicroAstInterpretation.NO_BREAKPOINT;
        }
        if (method == MicroAstMethod.ZONE) {
            return interpretZone(rule, rawValue);
        }
        return interpretMic(rule, rawValue);
    }

    @Override
    public MicroAstInterpretation interpret(MicroBreakpointRule rule, MicroAstMethod method, BigDecimal rawValue,
            String authority) {
        if ("CLSI".equals(authority) && method == MicroAstMethod.MIC && rawValue != null) {
            // AMR for R's CLSI MIC path uses this conventional dilution ladder.
            // Keep the measured value in the reading; normalize only for interpretation.
            for (String dilution : new String[] { "0.0001", "0.0002", "0.0005", "0.001", "0.002", "0.004", "0.008",
                    "0.016", "0.032", "0.064", "0.125", "0.25", "0.5", "1", "2", "4", "8", "16", "32", "64", "128",
                    "256", "512", "1024", "2048", "4096" }) {
                BigDecimal level = new BigDecimal(dilution);
                if (level.compareTo(rawValue) >= 0) {
                    rawValue = level;
                    break;
                }
            }
        }
        return interpret(rule, method, rawValue);
    }

    @Override
    public void validateOverride(MicroAstInterpretation overrideInterpretation, String overrideReason) {
        if (overrideInterpretation != null && (overrideReason == null || overrideReason.trim().isEmpty())) {
            throw new IllegalArgumentException("overrideReason is required");
        }
    }

    private MicroAstInterpretation interpretMic(MicroBreakpointRule rule, BigDecimal rawValue) {
        if (isIntermediate(rule, rawValue)) {
            return MicroAstInterpretation.INTERMEDIATE;
        }
        if (rawValue.compareTo(rule.getSusceptibleValue()) <= 0) {
            return MicroAstInterpretation.SUSCEPTIBLE;
        }
        if (rawValue.compareTo(rule.getResistantValue()) > 0
                || (rule.isResistantInclusive() && rawValue.compareTo(rule.getResistantValue()) == 0)) {
            return MicroAstInterpretation.RESISTANT;
        }
        return MicroAstInterpretation.INTERMEDIATE;
    }

    private MicroAstInterpretation interpretZone(MicroBreakpointRule rule, BigDecimal rawValue) {
        if (isIntermediate(rule, rawValue)) {
            return MicroAstInterpretation.INTERMEDIATE;
        }
        if (rawValue.compareTo(rule.getSusceptibleValue()) >= 0) {
            return MicroAstInterpretation.SUSCEPTIBLE;
        }
        if (rawValue.compareTo(rule.getResistantValue()) < 0
                || (rule.isResistantInclusive() && rawValue.compareTo(rule.getResistantValue()) == 0)) {
            return MicroAstInterpretation.RESISTANT;
        }
        return MicroAstInterpretation.INTERMEDIATE;
    }

    private boolean isIntermediate(MicroBreakpointRule rule, BigDecimal rawValue) {
        return rule.getIntermediateLowerValue() != null && rule.getIntermediateUpperValue() != null
                && rawValue.compareTo(rule.getIntermediateLowerValue()) >= 0
                && rawValue.compareTo(rule.getIntermediateUpperValue()) <= 0;
    }
}
