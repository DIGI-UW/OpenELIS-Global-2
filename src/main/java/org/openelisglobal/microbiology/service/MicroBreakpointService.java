package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

public interface MicroBreakpointService {
    MicroBreakpointStandard getActiveStandard(String authority, String version);

    /** Selectable breakpoint standards for AST run setup (M-05 FRS §4). */
    List<MicroBreakpointStandard> getActiveStandards();

    MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType);
}
