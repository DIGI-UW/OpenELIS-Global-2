package org.openelisglobal.program.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.ProgramSample;

public interface ProgramSampleDAO extends BaseDAO<ProgramSample, Integer> {
    ProgramSample getProgrammeSampleBySample(Integer sampleId, String programName);
}
