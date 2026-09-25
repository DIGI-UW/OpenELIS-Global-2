package org.openelisglobal.test.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestSectionService extends BaseObjectService<TestSection, String> {

    /**
     * Name of the seeded sentinel section meaning "the orderer chooses the test
     * section at entry time" (see SampleEntryTestsForTypeProvider and the indicator
     * reports, which look it up by this name). It is configuration plumbing, not a
     * real lab unit: admin surfaces must not list, rename, re-domain, reorder, or
     * deactivate it.
     */
    String USER_SENTINEL_SECTION_NAME = "user";

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getData(TestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getTestSections(String filter);

    // Name -> section lookup, used by the order-entry test picker to place each
    // orderable test under its section. A catalogue read.
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    TestSection getTestSectionByName(String testSection);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TestSection getTestSectionByName(TestSection testSection);

    // Test sections name where a test is run; resolving the one an order chose is a
// catalogue read. Configuring sections keeps PRIV_TEST_CONFIGURE on the writes.
    @PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_CATALOGUE_VIEW')")
    List<TestSection> getPageOfTestSections(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalTestSectionCount();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getAllTestSections();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getTestSectionsBySysUserId(String filter, int sysUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getAllTestSectionsBySysUserId(int sysUserId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TestSection getTestSectionById(String testSectionId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestSection> getAllInActiveTestSections();

    // The active test sections, as a catalogue list. The order dashboard reads it
    // to scope which orders a user may see (OrderSearchRestController
    // .resolveAllowedSectionIds), so gating it on PRIV_RESULT_VIEW denied the whole
    // clinical/environmental/vector dashboard to Reception, the 500 that greeted
    // the role whose entry point it is.
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<TestSection> getAllActiveTestSections();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsInSection(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getUserLocalizedTesSectionName(TestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();

    /**
     * Move a lab unit (test section) to a 1-based position in the display order and
     * densely renumber the whole sequence. Returns the full re-ordered list.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestSection> moveToSortOrderPosition(String testSectionId, int position, String sysUserId);
}
