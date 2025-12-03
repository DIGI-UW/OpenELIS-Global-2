package org.openelisglobal.program.service;

import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.valueholder.ProgramSampleDisplayItem;

public interface GenericProgramDisplayService {

    ProgramSampleDisplayItem getProgramSampleById(Integer programSampleId);

    DashboardSummary getAllProgramSamples();
}
