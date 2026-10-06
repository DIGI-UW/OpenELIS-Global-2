package org.openelisglobal.analyzer.service;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/** An analyzer's own mapping to the local catalog, as append-only revisions. */
public interface AnalyzerMappingService {

    /**
     * Pins the analyzer to an active Bridge profile revision and writes revision 1
     * of its mapping: the profile's defaults resolved against the catalog as it is
     * now. The analyzer must already be saved. Does not move the analyzer's mapping
     * in force; the caller applies the returned revision.
     */
    AnalyzerMappingSnapshot assignProfile(Analyzer analyzer, String profileId, int profileRevision, String actor);

    /**
     * Saves the draft as the analyzer's next revision, unless it equals the newest
     * one.
     */
    AnalyzerMappingSnapshot appendRevision(Analyzer analyzer, AnalyzerMappingDraft draft, String actor);

    /**
     * The analyzer's newest revision: its working draft, or the one in force when
     * nothing is newer.
     */
    Optional<AnalyzerMappingSnapshot> findLatestByAnalyzerId(String analyzerId);

    Optional<AnalyzerMappingSnapshot> findById(String mappingId);
}
