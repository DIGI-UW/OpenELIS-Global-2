package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

public record MicroIncomingDelivery(List<AnalyzerResults> rows, String actor) {
}
