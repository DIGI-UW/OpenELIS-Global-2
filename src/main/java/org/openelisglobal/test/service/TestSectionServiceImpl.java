package org.openelisglobal.test.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.LocaleChangeListener;
import org.openelisglobal.internationalization.GlobalLocaleResolver;
import org.openelisglobal.systemusersection.service.SystemUserSectionService;
import org.openelisglobal.systemusersection.valueholder.SystemUserSection;
import org.openelisglobal.test.dao.TestSectionDAO;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.LocaleResolver;

@Service
@DependsOn({ "springContext" })
public class TestSectionServiceImpl extends AuditableBaseObjectServiceImpl<TestSection, String>
        implements TestSectionService, LocaleChangeListener {

    private Map<String, String> testUnitIdToNameMap;

    @Autowired
    private TestSectionDAO baseObjectDAO;
    @Autowired
    private SystemUserSectionService systemUserSectionService;
    @Autowired
    private LocaleResolver localeResolver;

    @PostConstruct
    private void initializeGlobalVariables() {
        createTestIdToNameMap();
    }

    @PostConstruct
    private void initialize() {
        if (localeResolver instanceof GlobalLocaleResolver) {
            ((GlobalLocaleResolver) localeResolver).addLocalChangeListener(this);
        }
    }

    public TestSectionServiceImpl() {
        super(TestSection.class);
        this.auditTrailLog = true;
    }

    @Override
    protected TestSectionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllActiveTestSections() {
        return baseObjectDAO.getAllMatchingOrdered("isActive", "Y", "sortOrderInt", false);
    }

    @Override
    public void localeChanged(String locale) {
        testNamesChanged();
    }

    @Override
    public void refreshNames() {
        testNamesChanged();
    }

    public void testNamesChanged() {
        createTestIdToNameMap();
    }

    @Transactional(readOnly = true)
    public String getSortOrder(TestSection testSection) {
        return testSection == null ? "0" : testSection.getSortOrder();
    }

    @Override
    public String getUserLocalizedTesSectionName(TestSection testSection) {
        if (testSection == null) {
            return "";
        }

        return getUserLocalizedTestSectionName(testSection.getId());
    }

    public synchronized String getUserLocalizedTestSectionName(String testSectionId) {
        String name = testUnitIdToNameMap.get(testSectionId);
        return name == null ? "" : name;
    }

    private synchronized void createTestIdToNameMap() {
        testUnitIdToNameMap = new HashMap<>();

        List<TestSection> testSections = baseObjectDAO.getAllTestSections();

        for (TestSection testSection : testSections) {
            testUnitIdToNameMap.put(testSection.getId(), buildTestSectionName(testSection).replace("\n", " "));
        }
    }

    private String buildTestSectionName(TestSection testSection) {
        return testSection.getLocalization().getLocalizedValue();
    }

    @Override
    public List<Test> getTestsInSection(String id) {
        return TestServiceImpl.getTestsInTestSectionById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestSection testSection) {
        getBaseObjectDAO().getData(testSection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getTestSections(String filter) {
        return getBaseObjectDAO().getTestSections(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionByName(String testSection) {
        return getBaseObjectDAO().getTestSectionByName(testSection);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionByName(TestSection testSection) {
        return getBaseObjectDAO().getTestSectionByName(testSection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getPageOfTestSections(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTestSections(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestSectionCount() {
        return getBaseObjectDAO().getTotalTestSectionCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllTestSections() {
        return baseObjectDAO.getAllTestSections();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getTestSectionsBySysUserId(String filter, int sysUserId) {
        List<String> sectionIdList = new ArrayList<>();

        List<SystemUserSection> userTestSectionList = systemUserSectionService
                .getAllSystemUserSectionsBySystemUserId(sysUserId);
        for (int i = 0; i < userTestSectionList.size(); i++) {
            SystemUserSection sus = userTestSectionList.get(i);
            sectionIdList.add(sus.getTestSection().getId());
        }
        return getBaseObjectDAO().getTestSectionsBySysUserId(filter, sysUserId, sectionIdList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllTestSectionsBySysUserId(int sysUserId) {
        List<SystemUserSection> userTestSectionList = systemUserSectionService
                .getAllSystemUserSectionsBySystemUserId(sysUserId);
        List<String> sectionIds = new ArrayList<>();
        for (int i = 0; i < userTestSectionList.size(); i++) {
            SystemUserSection sus = userTestSectionList.get(i);
            sectionIds.add(sus.getTestSection().getId());
        }
        return getBaseObjectDAO().getAllTestSectionsBySysUserId(sysUserId, sectionIds);
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionById(String testSectionId) {
        return getBaseObjectDAO().getTestSectionById(testSectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllInActiveTestSections() {
        return getBaseObjectDAO().getAllInActiveTestSections();
    }

    @Override
    public String insert(TestSection testSection) {
        if (duplicateTestSectionExists(testSection)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testSection.getTestSectionName());
        }
        return super.insert(testSection);
    }

    @Override
    public TestSection save(TestSection testSection) {
        if (duplicateTestSectionExists(testSection)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testSection.getTestSectionName());
        }
        return super.save(testSection);
    }

    @Override
    public TestSection update(TestSection testSection) {
        if (duplicateTestSectionExists(testSection)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + testSection.getTestSectionName());
        }
        return super.update(testSection);
    }

    private boolean duplicateTestSectionExists(TestSection testSection) {
        return baseObjectDAO.duplicateTestSectionExists(testSection);
    }

    @Override
    @Transactional
    public List<TestSection> moveToSortOrderPosition(String testSectionId, int position, String sysUserId) {
        List<TestSection> ordered = new ArrayList<>(baseObjectDAO.getAllTestSections());
        // Legacy rows may share a sortOrder (creates land at Integer.MAX_VALUE),
        // so order deterministically before renumbering densely.
        ordered.sort(Comparator.comparingInt(TestSection::getSortOrderInt).thenComparing(TestSection::getId,
                Comparator.comparing(id -> Integer.parseInt(id))));
        TestSection target = null;
        for (TestSection section : ordered) {
            if (section.getId().equals(testSectionId)) {
                target = section;
                break;
            }
        }
        if (target == null) {
            throw new LIMSRuntimeException("Test section not found: " + testSectionId);
        }
        ordered.remove(target);
        int index = Math.max(0, Math.min(position - 1, ordered.size()));
        ordered.add(index, target);

        for (int i = 0; i < ordered.size(); i++) {
            TestSection section = ordered.get(i);
            if (section.getSortOrderInt() != i + 1) {
                section.setSortOrderInt(i + 1);
                section.setSysUserId(sysUserId);
                baseObjectDAO.update(section);
            }
        }
        return ordered;
    }
}
