package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.ProgramSample;

public interface ProgramSampleService extends BaseObjectService<ProgramSample, Integer> {
    ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName);

    List<ProgramSample> getPaginatedProgramSamples(Integer startIndex, Integer pageSize);

    List<ProgramSample> searchProgramSamples(String filter, Integer startIndex, Integer pageSize);

}
