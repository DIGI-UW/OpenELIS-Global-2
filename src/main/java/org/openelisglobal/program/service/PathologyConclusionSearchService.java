package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.pathology.ConclusionEntries;

public interface PathologyConclusionSearchService {

    /**
     * The active diagnoses of the dictionary whose name or code contains the text,
     * case and accents ignored; a blank text matches all. The first entries by
     * name, with the count of every match.
     */
    ConclusionEntries search(String query);
}
