package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;

public interface MicroAstService {

    MicroAstRun startRun(String isolateId, String panelId, String performedBy);

    /**
     * Starts a run with an explicit breakpoint standard snapshotted for that run.
     * Readings interpret against {@code breakpointStandardId} instead of the
     * configured default.
     */
    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId, String performedBy);

    MicroAstRun startRun(String isolateId, String panelId, String breakpointStandardId,
            List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, String performedBy);

    MicroAstRun startRepeatRun(String sourceRunId, MicroAstAttemptType attemptType, String reason,
            MicroAstMethod method, List<MicroLotSelection> lotSelections, String performedBy);

    MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy);

    MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy);

    MicroAstRun reviewRun(String runId, String performedBy);

    MicroAstRun selectReportableRun(String runId, String performedBy);

    List<MicroAstRun> getRunsForIsolate(String isolateId);

    List<MicroAstReading> getReadingsForRun(String runId);
}
