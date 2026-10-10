package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;

public interface MicroAstInterpretationService {

    MicroAstInterpretation interpret(MicroBreakpointRule rule, MicroAstMethod method, BigDecimal rawValue);

    void validateOverride(MicroAstInterpretation overrideInterpretation, String overrideReason);
}
