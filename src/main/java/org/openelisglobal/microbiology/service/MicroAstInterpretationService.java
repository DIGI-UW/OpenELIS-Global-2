package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroAstInterpretationService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstInterpretation interpret(MicroBreakpointRule rule, MicroAstMethod method, BigDecimal rawValue);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    void validateOverride(MicroAstInterpretation overrideInterpretation, String overrideReason);
}
