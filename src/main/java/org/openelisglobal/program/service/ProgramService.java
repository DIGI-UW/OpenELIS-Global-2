package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.Program;

public interface ProgramService extends BaseObjectService<Program, String> {
    /**
     * Returns all programs except those whose names match any in the excluded list
     * (case-insensitive).
     */
    List<Program> getGeneralPrograms(List<String> excludedNames);
}
