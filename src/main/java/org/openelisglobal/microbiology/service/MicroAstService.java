package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroAstOverrideEventForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;

public interface MicroAstService {

    MicroAstRun startRun(MicroAstRunSetupCommand command, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String performedBy);

    /**
     * Starts a run with an explicit breakpoint standard (M-05: selectable and
     * snapshotted per run). Readings on this run interpret against
     * {@code breakpointStandardId} instead of the configured default.
     */
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, List<String> orderedAntibioticIds,
            String performedBy);

    MicroAstSetupForm getSetup(String isolateId);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, List<String> orderedAntibioticIds,
            String performedBy);

    MicroAstReading recordReading(String runId, String antibioticId, BigDecimal rawValue, String performedBy);

    MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy);

    MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy);

    MicroAstReading revertOverride(String readingId, String reason, String performedBy);

    List<MicroAstOverrideEventForm> getOverrideHistoryForRun(String runId);

    MicroAstRun reviewRun(String runId, String performedBy);

    MicroAstRun applyAnalyzerResults(MicroAstAnalyzerResultBatch batch, String performedBy);

    MicroAstRun recordAnalyzerQcFailure(String runId, String instrumentQcReference, List<String> messageCodes,
            String sourceEventId, String performedBy);

    MicroAstRun acknowledgeAnalyzerFlags(String runId, String reason, String performedBy);

    MicroAstRun overrideQcFailure(String runId, String reason, String performedBy);

    MicroAstRun invalidateAndRepeat(String runId, String reason, String analyzerCardId, String performedBy);

    MicroAstRun selectReportableRun(String runId, String performedBy);

    List<MicroAstRun> getRunsForIsolate(String isolateId);

    List<MicroAstReading> getReadingsForRun(String runId);

    List<MicroAstRunAntibiotic> getOrderedAntibioticsForRun(String runId);

    List<MicroAstPanelAntibiotic> getPanelAntibiotics(String panelId);
}
