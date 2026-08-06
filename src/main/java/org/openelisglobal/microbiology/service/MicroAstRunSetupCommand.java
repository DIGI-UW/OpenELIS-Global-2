package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;

public record MicroAstRunSetupCommand(String isolateId, String panelId, String breakpointStandardId,
        String panelAdjustmentReason, MicroAstTechnique technique, List<MicroLotSelection> lotSelections,
        List<String> orderedAntibioticIds, boolean awaitAnalyzerResults, String analyzerInstrumentId,
        String analyzerCardId) {

    public MicroAstRunSetupCommand {
        lotSelections = lotSelections == null ? List.of() : List.copyOf(lotSelections);
        orderedAntibioticIds = orderedAntibioticIds == null ? null : List.copyOf(orderedAntibioticIds);
    }
}
