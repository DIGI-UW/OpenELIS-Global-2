package org.openelisglobal.test.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

public interface TestSectionService extends BaseObjectService<TestSection, String> {

    /**
     * Name of the seeded sentinel section meaning "the orderer chooses the test
     * section at entry time" (see SampleEntryTestsForTypeProvider and the indicator
     * reports, which look it up by this name). It is configuration plumbing, not a
     * real lab unit: admin surfaces must not list, rename, re-domain, reorder, or
     * deactivate it.
     */
    String USER_SENTINEL_SECTION_NAME = "user";

    void getData(TestSection testSection);

    List<TestSection> getTestSections(String filter);

    TestSection getTestSectionByName(String testSection);

    TestSection getTestSectionByName(TestSection testSection);

    List<TestSection> getPageOfTestSections(int startingRecNo);

    Integer getTotalTestSectionCount();

    List<TestSection> getAllTestSections();

    List<TestSection> getTestSectionsBySysUserId(String filter, int sysUserId);

    List<TestSection> getAllTestSectionsBySysUserId(int sysUserId);

    TestSection getTestSectionById(String testSectionId);

    List<TestSection> getAllInActiveTestSections();

    List<TestSection> getAllActiveTestSections();

    List<Test> getTestsInSection(String id);

    String getUserLocalizedTesSectionName(TestSection testSection);

    void refreshNames();

    /**
     * Move a lab unit (test section) to a 1-based position in the display order and
     * densely renumber the whole sequence. Returns the full re-ordered list.
     */
    List<TestSection> moveToSortOrderPosition(String testSectionId, int position, String sysUserId);
}
