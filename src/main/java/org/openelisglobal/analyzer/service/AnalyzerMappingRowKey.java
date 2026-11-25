package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;

/**
 * Identifies one reported record in an analyzer mapping: the instrument's code
 * and the record's sub-identity under it, in the vendor's HL7 sub-ID notation
 * ({@code HIV-1&Ct}, {@code &LOG}); empty for the main result.
 */
public record AnalyzerMappingRowKey(String sourceRowKey, String subIdentity) {

    public AnalyzerMappingRowKey {
        subIdentity = subIdentity == null ? "" : subIdentity;
    }

    /** The record as an operator reads it: its code, then its sub-identity. */
    public String label() {
        return subIdentity.isEmpty() ? sourceRowKey : sourceRowKey + " " + subIdentity;
    }

    public static AnalyzerMappingRowKey main(String sourceRowKey) {
        return new AnalyzerMappingRowKey(sourceRowKey, "");
    }

    public static AnalyzerMappingRowKey of(AnalyzerMappingTest row) {
        return new AnalyzerMappingRowKey(row.getId().getSourceRowKey(), row.getId().getSubIdentity());
    }

    public static AnalyzerMappingRowKey of(AnalyzerMappingResult row) {
        return new AnalyzerMappingRowKey(row.getId().getSourceRowKey(), row.getId().getSubIdentity());
    }
}
