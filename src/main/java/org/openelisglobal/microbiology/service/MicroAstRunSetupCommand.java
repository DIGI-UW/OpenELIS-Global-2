package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;

public record MicroAstRunSetupCommand(String isolateId, String panelId, String breakpointStandardId,
        String panelAdjustmentReason, MicroAstTechnique technique, List<MicroLotSelection> lotSelections,
        List<String> orderedAntibioticIds, boolean awaitAnalyzerResults, String analyzerInstrumentId,
        String analyzerCardId, String clinicalContext) {

    public MicroAstRunSetupCommand(String isolateId, String panelId, String breakpointStandardId,
            String panelAdjustmentReason, MicroAstTechnique technique, List<MicroLotSelection> lotSelections,
            List<String> orderedAntibioticIds, boolean awaitAnalyzerResults, String analyzerInstrumentId,
            String analyzerCardId) {
        this(isolateId, panelId, breakpointStandardId, panelAdjustmentReason, technique, lotSelections,
                orderedAntibioticIds, awaitAnalyzerResults, analyzerInstrumentId, analyzerCardId, null);
    }

    public MicroAstRunSetupCommand {
        lotSelections = lotSelections == null ? List.of() : List.copyOf(lotSelections);
        orderedAntibioticIds = orderedAntibioticIds == null ? null : List.copyOf(orderedAntibioticIds);
    }
}
