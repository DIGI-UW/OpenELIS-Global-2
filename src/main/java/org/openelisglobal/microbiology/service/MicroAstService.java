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
import org.springframework.security.access.prepost.PreAuthorize;

public interface MicroAstService {

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(MicroAstRunSetupCommand command, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String performedBy);

    /**
     * Starts a run with an explicit breakpoint standard snapshotted for that run.
     * Readings interpret against {@code breakpointStandardId} instead of the
     * configured default.
     */
    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String panelAdjustmentReason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, List<String> orderedAntibioticIds,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstSetupForm getSetup(String isolateId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstTechnique technique, List<MicroLotSelection> lotSelections, List<String> orderedAntibioticIds,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstReading recordReading(String runId, String antibioticId, BigDecimal rawValue, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstReading revertOverride(String readingId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstOverrideEventForm> getOverrideHistoryForRun(String runId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun reviewRun(String runId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun applyAnalyzerResults(MicroAstAnalyzerResultBatch batch, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun recordAnalyzerQcFailure(String runId, String instrumentQcReference, List<String> messageCodes,
            String sourceEventId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun acknowledgeAnalyzerFlags(String runId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun overrideQcFailure(String runId, String reason, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun invalidateAndRepeat(String runId, String reason, String analyzerCardId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_BENCH')")
    MicroAstRun selectReportableRun(String runId, String performedBy);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstRun> getRunsForIsolate(String isolateId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstReading> getReadingsForRun(String runId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstRunAntibiotic> getOrderedAntibioticsForRun(String runId);

    @PreAuthorize("hasAuthority('PRIV_MICRO_VIEW')")
    List<MicroAstPanelAntibiotic> getPanelAntibiotics(String panelId);
}
