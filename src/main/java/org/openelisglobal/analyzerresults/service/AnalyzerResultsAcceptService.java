package org.openelisglobal.analyzerresults.service;

import java.util.List;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.result.action.util.ResultEntryAlert;

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

    /**
     * The same, recording in the same transaction each acknowledgement the reviewer
     * gave for a value they retyped (OGC-1417).
     */
    void acceptAndPersist(List<AnalyzerResultItem> allResults, String sysUserId, List<ResultEntryAlert> alerts);

    /**
     * The decimal places a staged number is shown and saved with: its component's
     * when it is on one, otherwise the test's first active result definition's.
     * Null when the result has no test or no value, or the definition sets none.
     */
    String significantDigitsFor(AnalyzerResults staged);
}
