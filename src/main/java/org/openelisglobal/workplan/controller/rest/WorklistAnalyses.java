package org.openelisglobal.workplan.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;

/**
 * Which analyses a worklist shows. A test referred to a reference laboratory is
 * not this laboratory's work: it stays out of every worklist until its result
 * comes back (OGC-1423).
 */
final class WorklistAnalyses {

    private WorklistAnalyses() {
    }

    static List<Analysis> withoutReferredOut(List<Analysis> analyses) {
        if (analyses == null) {
            return new ArrayList<>();
        }
        List<Analysis> inHouse = new ArrayList<>();
        for (Analysis analysis : analyses) {
            if (!analysis.isReferredOut()) {
                inHouse.add(analysis);
            }
        }
        return inHouse;
    }
}
