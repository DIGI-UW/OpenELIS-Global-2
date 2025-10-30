package org.openelisglobal.typeofsample.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestComparator;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO.SampleDomain;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class TypeOfSampleServiceImpl extends AuditableBaseObjectServiceImpl<TypeOfSample, String>
        implements TypeOfSampleService {

    private Map<String, List<Test>> sampleIdTestMap = new HashMap<>();
    private Map<String, String> typeOfSampleIdToNameMap;
    private Map<String, String> typeOfSampleWellKnownNameToIdMap;
    private Map<String, List<TypeOfSample>> testIdToTypeOfSampleMap = null;
    private Map<String, List<TypeOfSample>> panelIdToTypeOfSampleMap = null;
    // The purpose of this map is to make sure all the references refer to the same
    // instances of the TypeOfSample objects
    // Without this comparisons may fail
    private Map<String, TypeOfSample> typeOfSampleIdtoTypeOfSampleMap = null;

    @Autowired
    protected TypeOfSampleDAO baseObjectDAO;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;
    @Autowired
    private TestService testService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private PanelService panelService;

    @PostConstruct
    private synchronized void initializeGlobalVariables() {
        if (typeOfSampleIdtoTypeOfSampleMap == null) {
            createTypeOfSampleIdentityMap();
        }
    }

    TypeOfSampleServiceImpl() {
        super(TypeOfSample.class);
    }

    @Override
    protected TypeOfSampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTypeOfSampleByDescriptionAndDomain(TypeOfSample typeOfSample, boolean ignoreCase) {
        return baseObjectDAO.getTypeOfSampleByDescriptionAndDomain(typeOfSample, ignoreCase);
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized List<Test> getActiveTestsBySampleTypeId(String sampleTypeId, boolean orderableOnly) {

        List<Test> testList = sampleIdTestMap.get(sampleTypeId);

        if (testList == null) {
            testList = createSampleIdTestMap(sampleTypeId);
        }

        if (orderableOnly) {
            return filterByOrderable(testList);
        } else {
            return testList;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized List<Test> getActiveTestsBySampleTypeIdAndTestUnit(String sampleType, boolean b,
            List<String> testUnitIds) {
        List<Test> testList = getActiveTestsBySampleTypeId(sampleType, b);
        return testList.stream().filter(test -> testUnitIds.contains(test.getTestSection().getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTestsBySampleTypeId(String sampleTypeId) {
        List<Test> testList = new ArrayList<>();

        List<TypeOfSampleTest> testLinks = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleTypeId);

        for (TypeOfSampleTest link : testLinks) {
            testList.add(testService.getTestById(link.getTestId()));
        }

        Collections.sort(testList, TestComparator.NAME_COMPARATOR);
        return testList;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTransientTypeOfSampleById(String id) {
        return baseObjectDAO.getTypeOfSampleById(id);
    }

    private List<Test> filterByOrderable(List<Test> testList) {
        List<Test> filteredList = new ArrayList<>();

        for (Test test : testList) {
            if (test.getOrderable()) {
                filteredList.add(test);
            }
        }

        return filteredList;
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized List<TypeOfSample> getTypeOfSampleForTest(String testId) {
        if (testIdToTypeOfSampleMap == null) {
            createTestIdToTypeOfSampleMap();
        }

        return testIdToTypeOfSampleMap.get(testId);
    }

    private synchronized void createTestIdToTypeOfSampleMap() {
        testIdToTypeOfSampleMap = new HashMap<>();

        List<TypeOfSampleTest> typeOfSampleTestList = typeOfSampleTestService.getAllTypeOfSampleTests();

        for (TypeOfSampleTest typeTest : typeOfSampleTestList) {
            String testId = typeTest.getTestId();
            TypeOfSample typeOfSample = typeOfSampleIdtoTypeOfSampleMap
                    .get(baseObjectDAO.getTypeOfSampleById(typeTest.getTypeOfSampleId()).getId());
            if (testIdToTypeOfSampleMap.containsKey(testId)) {
                testIdToTypeOfSampleMap.get(testId).add(typeOfSample);
            } else {
                testIdToTypeOfSampleMap.put(testId, new ArrayList<>(Arrays.asList(typeOfSample)));
            }
        }
    }

    private synchronized List<Test> createSampleIdTestMap(String sampleTypeId) {
        List<Test> testList;
        List<TypeOfSampleTest> tests = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleTypeId);

        testList = new ArrayList<>();

        for (TypeOfSampleTest link : tests) {
            Test test = testService.getActiveTestById(Integer.valueOf(link.getTestId()));
            if (test != null) {
                testList.add(test);
            }
        }

        Collections.sort(testList, TestComparator.NAME_COMPARATOR);

        sampleIdTestMap.put(sampleTypeId, testList);
        return testList;
    }

    /**
     * This class keeps lists of tests for each type of sample. If the DB of tests
     * changes, we need to invalidate such lists.
     */
    @Override
    public synchronized void clearCache() {
        sampleIdTestMap.clear();
        createTypeOfSampleIdentityMap();
        typeOfSampleIdToNameMap = null;
        typeOfSampleWellKnownNameToIdMap = null;
        testIdToTypeOfSampleMap = null;
    }

    private synchronized void createTypeOfSampleIdentityMap() {
        typeOfSampleIdtoTypeOfSampleMap = new HashMap<>();

        List<TypeOfSample> typeOfSampleList = baseObjectDAO.getAllTypeOfSamples();

        for (TypeOfSample typeOfSample : typeOfSampleList) {
            typeOfSampleIdtoTypeOfSampleMap.put(typeOfSample.getId(), typeOfSample);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized String getTypeOfSampleNameForId(String id) {
        if (typeOfSampleIdToNameMap == null) {
            createSampleNameIDMaps();
        }

        return typeOfSampleIdToNameMap.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized String getTypeOfSampleIdForLocalAbbreviation(String name) {
        if (typeOfSampleWellKnownNameToIdMap == null) {
            createSampleNameIDMaps();
        }

        return typeOfSampleWellKnownNameToIdMap.get(name);
    }

    private synchronized void createSampleNameIDMaps() {
        typeOfSampleIdToNameMap = new HashMap<>();
        typeOfSampleWellKnownNameToIdMap = new HashMap<>();

        List<TypeOfSample> allTypes = baseObjectDAO.getAllTypeOfSamples();
        for (TypeOfSample typeOfSample : allTypes) {
            typeOfSampleIdToNameMap.put(typeOfSample.getId(), typeOfSample.getLocalizedName());
            typeOfSampleWellKnownNameToIdMap.put(typeOfSample.getLocalAbbreviation(), typeOfSample.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public synchronized List<TypeOfSample> getTypeOfSampleForPanelId(String id) {
        if (panelIdToTypeOfSampleMap == null) {
            panelIdToTypeOfSampleMap = new HashMap<>();

            List<Panel> panels = panelService.getAllActivePanels();

            for (Panel panel : panels) {
                List<TypeOfSamplePanel> typeOfSamplePanels = typeOfSamplePanelService
                        .getTypeOfSamplePanelsForPanel(panel.getId());
                List<TypeOfSample> typeOfSampleList = new ArrayList<>();
                for (TypeOfSamplePanel typeOfSamplePanel : typeOfSamplePanels) {
                    typeOfSampleList.add(typeOfSampleIdtoTypeOfSampleMap
                            .get(baseObjectDAO.getTypeOfSampleById(typeOfSamplePanel.getTypeOfSampleId()).getId()));
                }
                panelIdToTypeOfSampleMap.put(panel.getId(), typeOfSampleList);
            }
        }

        return panelIdToTypeOfSampleMap.get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfSample typeOfSample) {
        getBaseObjectDAO().getData(typeOfSample);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNameForTypeOfSampleId(String id) {
        return getBaseObjectDAO().getNameForTypeOfSampleId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getAllTypeOfSamples() {
        return baseObjectDAO.getAllTypeOfSamples();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getAllTypeOfSamplesSortOrdered() {
        return getBaseObjectDAO().getAllTypeOfSamplesSortOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getTypesForDomain(SampleDomain domain) {
        return getBaseObjectDAO().getTypesForDomain(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTypeOfSampleCount() {
        return getBaseObjectDAO().getTotalTypeOfSampleCount();
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTypeOfSampleById(String typeOfSampleId) {
        return getBaseObjectDAO().getTypeOfSampleById(typeOfSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getSampleTypeFromTest(Test test) {
        return getBaseObjectDAO().getSampleTypeFromTest(test);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getTypesForDomainBySortOrder(SampleDomain human) {
        return getBaseObjectDAO().getTypesForDomainBySortOrder(human);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getPageOfTypeOfSamples(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTypeOfSamples(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getTypes(String filter, String domain) {
        return getBaseObjectDAO().getTypes(filter, domain);
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTypeOfSampleByLocalAbbrevAndDomain(String localAbbrev, String domain) {
        return getBaseObjectDAO().getTypeOfSampleByLocalAbbrevAndDomain(localAbbrev, domain);
    }

    @Override
    @Transactional
    public void delete(TypeOfSample typeOfSample) {
        super.delete(typeOfSample);
        getBaseObjectDAO().clearMap();
    }

    @Override
    @Transactional
    public String insert(TypeOfSample typeOfSample) {
        if (duplicateTypeOfSampleExists(typeOfSample)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfSample.getDescription());
        }
        baseObjectDAO.clearMap();
        return super.insert(typeOfSample);
    }

    @Override
    @Transactional
    public TypeOfSample save(TypeOfSample typeOfSample) {
        if (duplicateTypeOfSampleExists(typeOfSample)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfSample.getDescription());
        }
        baseObjectDAO.clearMap();
        return super.save(typeOfSample);
    }

    @Override
    @Transactional
    public TypeOfSample update(TypeOfSample typeOfSample) {
        if (duplicateTypeOfSampleExists(typeOfSample)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + typeOfSample.getDescription());
        }
        baseObjectDAO.clearMap();
        return super.update(typeOfSample);
    }

    private boolean duplicateTypeOfSampleExists(TypeOfSample typeOfSample) {
        return baseObjectDAO.duplicateTypeOfSampleExists(typeOfSample);
    }

    @Override
    @Transactional(readOnly = true)
    public Localization getLocalizationForSampleType(String id) {
        TypeOfSample typeOfSample = getTypeOfSampleById(id);
        Localization localization = typeOfSample != null ? typeOfSample.getLocalization() : null;
        Hibernate.initialize(localization);
        return localization;
    }

    @Override
    public TypeOfSample getTypeOfSampleByLocalizedName(String typeOfSampleName, Locale locale) {
        return baseObjectDAO.getTypeOfSampleByLocalizedName(typeOfSampleName, locale);
    }
}
