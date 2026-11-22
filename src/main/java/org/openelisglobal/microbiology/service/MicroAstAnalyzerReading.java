package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;

public record MicroAstAnalyzerReading(String antibioticId, BigDecimal rawValue, String units,
        String instrumentInterpretation, String analyzerResultReference) {
}
