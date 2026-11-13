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
            String antibioticId, String method, String specimenTypeId, String breakpointType, String clinicalContext,
            String diskContent);

    /**
     * The closest WHONET organism level decides, then the specimen (its WHONET
     * specimen code) and the indication, then WHONET's order of sites.
     */
    MicroBreakpointRule findWhonetBreakpointRule(String standardId, String organismId, String antibioticId,
            String breakpointType, String diskContent, String specimenCode, String clinicalContext);
}
