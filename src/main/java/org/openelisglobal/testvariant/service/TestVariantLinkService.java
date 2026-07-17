package org.openelisglobal.testvariant.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testvariant.valueholder.TestVariantLink;

public interface TestVariantLinkService extends BaseObjectService<TestVariantLink, String> {

    /** The variant link for a test, or null if it is not a member of any group. */
    TestVariantLink getByTestId(String testId);

    /** All links (members) of a group. */
    List<TestVariantLink> getByGroupId(String groupId);

    /** All variant links across the catalog. */
    List<TestVariantLink> getAllLinks();

    /**
     * FR-51 — link ≥2 tests into one assay group. If any of them is already in a
     * group, all the others join that existing group; otherwise a new group id is
     * minted. No-op for fewer than two distinct test ids. Returns the group id.
     */
    String linkTests(List<String> testIds, String sysUserId);

    /**
     * FR-52/54 — add a single test to the same group as {@code sourceTestId},
     * creating the group if the source isn't yet grouped. Returns the group id.
     */
    String addToGroupOf(String sourceTestId, String newTestId, String sysUserId);

    /** FR-51 — remove a test from its group without touching any of its fields. */
    void unlink(String testId, String sysUserId);
}
