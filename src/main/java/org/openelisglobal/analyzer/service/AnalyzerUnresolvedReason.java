package org.openelisglobal.analyzer.service;

/** Why a default could not be resolved by exact standard-code match. */
public enum AnalyzerUnresolvedReason {
    /** No local test, or no local answer, carries the profile's code. */
    NO_MATCH,
    /** More than one usable local candidate carries the profile's code. */
    AMBIGUOUS,
    /** Candidates carry the code, but none can hold the result. */
    INCOMPATIBLE
}
