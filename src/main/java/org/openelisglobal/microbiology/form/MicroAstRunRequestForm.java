package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroAstRunRequestForm {

    public String isolateId;
    public String panelId;
    public String panelAdjustmentReason;
    public String breakpointStandardId;
    public String attemptType;
    public String reason;
    public String technique;
    public boolean awaitAnalyzerResults;
    public String analyzerInstrumentId;
    public String analyzerCardId;
    public List<String> orderedAntibioticIds;
    public List<MicroLotSelectionRequestForm> lotSelections = new ArrayList<>();
}
