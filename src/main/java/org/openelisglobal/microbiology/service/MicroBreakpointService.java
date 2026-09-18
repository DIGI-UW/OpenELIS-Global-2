package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroBreakpointService {
    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroBreakpointStandard getActiveStandard(String authority, String version);

    /** Returns the active breakpoint standards available when an AST run starts. */
    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroBreakpointStandard> getActiveStandards();

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroBreakpointStandard getStandard(String standardId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType);
}
