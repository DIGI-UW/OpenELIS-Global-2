package org.openelisglobal.program.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.generalProgram.GeneralProgramDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeneralProgramDisplayServiceImpl implements GeneralProgramDisplayService {

    @Autowired
    private ProgramSampleService programSampleService;

    @Override
    public GeneralProgramDisplayItem convertToDisplayItem(Program program) {
        GeneralProgramDisplayItem item = new GeneralProgramDisplayItem();

        if (program != null) {
            item.setProgramId(program.getId());
            item.setProgramName(program.getProgramName());
            item.setProgramCode(program.getCode());
            item.setDescription(program.getProgramName()); // Can be enhanced with actual description field
            item.setHasQuestionnaire(program.getQuestionnaireUUID() != null);

            Long orderCount = programSampleService.getCountByProgramId(program.getId());
            item.setOrderCount(orderCount != null ? orderCount : 0L);

            if (program.getTestSection() != null) {
                item.setTestSectionName(program.getTestSection().getTestSectionName());
            }
        }

        return item;
    }

    @Override
    public List<GeneralProgramDisplayItem> convertToDisplayItems(List<Program> programs) {
        return programs.stream().map(this::convertToDisplayItem).collect(Collectors.toList());
    }
}
