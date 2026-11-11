package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public record MicroAstAnalyzerResultBatch(String runId, String sourceEventId, String analyzerInstrumentId,
        String analyzerCardId, String analyzerSoftwareVersion, String analyzerOrganismId, String analyzerOrganismName,
        BigDecimal analyzerOrganismConfidence, List<String> analyzerExpertFlags, String instrumentQcReference,
        Boolean qcPassed, Timestamp loadedAt, Timestamp completedAt, List<String> analyzerMessageCodes,
        List<MicroAstAnalyzerReading> readings) {

    public MicroAstAnalyzerResultBatch {
        analyzerExpertFlags = analyzerExpertFlags == null ? List.of() : List.copyOf(analyzerExpertFlags);
        analyzerMessageCodes = analyzerMessageCodes == null ? List.of() : List.copyOf(analyzerMessageCodes);
        readings = readings == null ? List.of() : List.copyOf(readings);
    }
}
