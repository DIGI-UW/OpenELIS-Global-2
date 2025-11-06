package org.openelisglobal.test.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

public interface TestSectionService extends BaseObjectService<TestSection, String> {
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
}
