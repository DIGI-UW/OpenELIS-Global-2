package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ProgramSampleService extends BaseObjectService<ProgramSample, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ProgramSample> getProgramSamplesByAccessionNumberOrProgramName(String filter);
}
