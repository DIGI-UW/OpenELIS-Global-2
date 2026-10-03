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
        if (rawValue.compareTo(rule.getResistantValue()) >= 0) {
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
        if (rawValue.compareTo(rule.getResistantValue()) <= 0) {
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
