package org.openelisglobal.program.service;

import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorResults;
import org.openelisglobal.program.valueholder.ProgramSample;

/**
 * The prior results panel of the pathology, immunohistochemistry and cytology
 * case views: the patient's earlier anatomic-pathology cases, found from the
 * case on screen and never from a patient id.
 */
public interface PriorResultsService {

    /** The case of that bench with that id, or null when that bench has none. */
    ProgramSample caseOf(AnatomicPathologyBench bench, Integer id);

    /**
     * The patient's cases earlier than the case on screen that the user may read,
     * newest first: the first rows of the site's setting, or every row when
     * {@code all} holds. An unknown case, or an order with no patient, has none.
     */
    PriorResults priorResultsOf(AnatomicPathologyBench bench, Integer id, boolean all, String sysUserId);
}
