package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.generalProgram.GeneralProgramDisplayItem;

public interface GeneralProgramDisplayService {
    /**
     * Converts a Program to a GeneralProgramDisplayItem with additional display
     * information
     */
    GeneralProgramDisplayItem convertToDisplayItem(Program program);

    /**
     * Converts a list of Programs to GeneralProgramDisplayItems
     */
    List<GeneralProgramDisplayItem> convertToDisplayItems(List<Program> programs);
}
