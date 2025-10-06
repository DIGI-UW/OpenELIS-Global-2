package org.openelisglobal.program.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.Program;

public interface ProgramDAO extends BaseDAO<Program, String> {
    /**
     * Returns all programs except those whose names match any in the excluded list
     * (case-insensitive).
     */
    List<Program> getGeneralPrograms(List<String> excludedNames);
}
