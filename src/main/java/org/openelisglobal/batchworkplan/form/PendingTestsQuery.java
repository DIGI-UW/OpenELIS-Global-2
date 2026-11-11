package org.openelisglobal.batchworkplan.form;

import java.time.LocalDate;
import java.util.List;

/**
 * One page of the Pending Tests list as the user narrowed it (OGC-1588): the
 * filters, the search text, the sort and the page. Every part is applied in the
 * query, so the whole pending pool is reachable and not only its oldest rows.
 *
 * @param search    matched against lab number, patient, test, panel, lab unit
 *                  and sample type; blank for none
 * @param sort      a column key of the list, or null for lab number order
 * @param ascending sort direction
 */
public record PendingTestsQuery(int page, int pageSize, String search, List<String> testIds, List<String> panelIds,
        List<String> testSectionIds, List<String> priorities, LocalDate fromDate, LocalDate toDate, String sort,
        boolean ascending) {
}
