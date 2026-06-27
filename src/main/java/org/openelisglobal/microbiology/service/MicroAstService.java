package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;

public interface MicroAstService {

    MicroAstRun startRun(String isolateId, String panelId, String performedBy);

    MicroAstReading recordReading(String runId, String antibioticId, MicroAstMethod method, BigDecimal rawValue,
            String performedBy);

    MicroAstReading overrideReading(String readingId, MicroAstInterpretation overrideInterpretation,
            String overrideReason, String performedBy);

    MicroAstRun reviewRun(String runId, String performedBy);

    List<MicroAstRun> getRunsForIsolate(String isolateId);

    List<MicroAstReading> getReadingsForRun(String runId);
}
