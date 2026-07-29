package org.openelisglobal.testreagentlink.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;

public interface TestReagentLinkDAO extends BaseDAO<TestReagentLink, String> {

    /** All reagent links for a test, ordered for display. */
    List<TestReagentLink> getByTestId(String testId);

    /**
     * The link for a (test, reagent) pair, or null. Backs the (test_id, reagent_id)
     * uniqueness invariant and the per-reagent PUT/DELETE lookups.
     */
    TestReagentLink getByTestIdAndReagentId(String testId, Long reagentId);
}
