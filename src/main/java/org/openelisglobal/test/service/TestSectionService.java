package org.openelisglobal.test.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestSectionService extends BaseObjectService<TestSection, String> {
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getData(TestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getTestSections(String filter);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TestSection getTestSectionByName(String testSection);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TestSection getTestSectionByName(TestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
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

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestSection> getAllActiveTestSections();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsInSection(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getUserLocalizedTesSectionName(TestSection testSection);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshNames();
}
