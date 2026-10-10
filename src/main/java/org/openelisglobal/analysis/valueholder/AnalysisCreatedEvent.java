package org.openelisglobal.analysis.valueholder;

/**
 * Published inside the insertion transaction for clinical ownership consumers.
 */
public record AnalysisCreatedEvent(Analysis analysis, String actor) {
}
