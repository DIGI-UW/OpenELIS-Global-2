package org.openelisglobal.analyzerresults.service;

import java.util.List;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;

/**
 * Orchestrates the "accept analyzer results" workflow: extracts actionable
 * items from the UI list, builds the domain objects (Sample, Analysis, Result,
 * etc.), and delegates persistence to {@link AnalyzerResultsService}.
 *
 * <p>
 * Extracted from AnalyzerResultsController so that business logic lives in the
 * service layer where it belongs.
 */
public interface AnalyzerResultsAcceptService {

    /**
     * Accept the user-selected analyzer results and persist them into the OE
     * results system.
     *
     * @param allResults every {@link AnalyzerResultItem} on the current page
     *                   (accepted, rejected, deleted, and untouched)
     * @param sysUserId  the authenticated user's system id
     */
    void acceptAndPersist(List<AnalyzerResultItem> allResults, String sysUserId);
}
