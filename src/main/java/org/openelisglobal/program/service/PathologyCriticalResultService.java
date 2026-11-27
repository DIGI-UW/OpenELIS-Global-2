package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * Whether a pathology case holds a critical result: a coded diagnosis whose
 * code starts with one of the laboratory's critical starts, or the
 * pathologist's own flag.
 */
public interface PathologyCriticalResultService {

    /**
     * What makes a case critical. {@code flagged} is the case's own flag;
     * {@code conclusionDictionaryIds} are the coded diagnoses whose code matches,
     * in stored order; {@code critical} is either.
     */
    record CriticalResult(boolean critical, boolean flagged, List<String> conclusionDictionaryIds) {
    }

    /** Reads the case's conclusions, so it runs inside the caller's transaction. */
    CriticalResult of(PathologySample pathologySample);
}
