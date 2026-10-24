package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroBreakpointDefaultForm;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;

public interface MicroBreakpointService {
    MicroBreakpointDefaultForm getDefaultStandard();

    MicroBreakpointDefaultForm setDefaultStandard(String standardId, String actorId);

    MicroBreakpointStandard getActiveStandard(String authority, String version);

    /** Returns the active breakpoint standards available when an AST run starts. */
    List<MicroBreakpointStandard> getActiveStandards();

    MicroBreakpointStandard getStandard(String standardId);

    MicroBreakpointRule findBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType);

    MicroBreakpointRule findContextualBreakpointRule(String standardId, String organismId, String organismGroup,
            String antibioticId, String method, String specimenTypeId, String breakpointType, String clinicalContext);
}
