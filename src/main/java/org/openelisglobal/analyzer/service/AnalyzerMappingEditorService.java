package org.openelisglobal.analyzer.service;

/**
 * Reads, edits and confirms one analyzer's own mapping, and previews a
 * profile's defaults.
 */
public interface AnalyzerMappingEditorService {

    /**
     * The analyzer's newest mapping revision with what each row resolves to today.
     */
    AnalyzerMappingView getMapping(String analyzerId);

    /**
     * Saves the edit as the analyzer's next revision. A row the operator changed is
     * recorded as an override; the rest keep their origin.
     */
    AnalyzerMappingView saveMapping(String analyzerId, AnalyzerMappingUpdate update, String actor);

    AnalyzerMappingConfirmationView confirmMapping(String analyzerId, AnalyzerMappingConfirmationRequest request,
            String actor);

    /**
     * The defaults a new analyzer on this profile revision would get against the
     * catalog as it is now. Read-only; nothing is saved.
     */
    AnalyzerMappingView getDefaults(String profileId, int profileRevision);

    /**
     * The analyzer's mapping as it would read on another revision of its profile
     * with these decisions. Read-only; nothing is saved.
     */
    AnalyzerMappingView preview(String analyzerId, int profileRevision, AnalyzerMappingDraft decisions);
}
