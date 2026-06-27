package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

public interface MicroBreakpointService {
    MicroBreakpointStandard getActiveStandard(String authority, String version);

    MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType);
}
