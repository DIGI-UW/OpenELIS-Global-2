package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.MicroAstAnalyzerResultRequestForm;

public record MicroAstAnalyzerEventCommand(String externalEventId, String eventType, String analyzerId, String sourceId,
        String targetRunId, MicroAstAnalyzerResultRequestForm payload) {
}
