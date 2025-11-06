package org.openelisglobal.program.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.ProgramSample;

public interface ProgramSampleService extends BaseObjectService<ProgramSample, Integer> {
    ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName);
}
