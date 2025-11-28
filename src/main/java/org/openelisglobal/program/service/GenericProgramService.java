package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.valueholder.ProgramUtil;

public interface GenericProgramService {
    List<ProgramUtil> getAllProgramEntries();

    ProgramUtil getProgramEntry(Integer programSampleId);

    DashboardSummary getDashboardSummary();
}
