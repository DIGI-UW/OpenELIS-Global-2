package org.openelisglobal.containertype.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.containertype.dao.TestExpectedContainerDAO;
import org.openelisglobal.containertype.form.ContainerTypeView;
import org.openelisglobal.containertype.form.SampleTypeContainersView;
import org.openelisglobal.containertype.form.TestContainersForm;
import org.openelisglobal.containertype.form.TestContainersView;
import org.openelisglobal.containertype.valueholder.TestExpectedContainer;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestContainerServiceImpl implements TestContainerService {

    private static final int MAX_COUNT = 99;
    private static final BigDecimal MAX_VOLUME = new BigDecimal("99999999.99");

    @Autowired
    private TestExpectedContainerDAO expectedContainerDAO;
    @Autowired
    private ContainerTypeService containerTypeService;
    @Lazy
    @Autowired
    private TestService testService;
    @Lazy
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Lazy
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    @Transactional(readOnly = true)
    public Optional<TestContainersView> forTest(String testId) {
        return findTest(testId).map(this::view);
    }

    @Override
    @Transactional
    public TestContainersView save(String testId, TestContainersForm form, String sysUserId) {
        Test test = findTest(testId).orElseThrow(() -> new TestContainersRefusedException("testId", "notFound", null));
        Integer id = Integer.valueOf(test.getId());
        expectedContainerDAO.lockTest(id);
        Set<String> ownTypes = primarySampleTypeIds(test);
        Map<Long, ContainerTypeView> containers = containersById();
        Set<Long> listedBefore = new HashSet<>();
        expectedContainerDAO.getForTest(id).forEach(row -> listedBefore.add(row.getContainerTypeId()));
        String domain = domainOf(test);
        List<TestContainersForm.Row> rows = form == null || form.containers() == null ? List.of() : form.containers();
        List<TestExpectedContainer> accepted = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int preferred = 0;
        for (int index = 0; index < rows.size(); index++) {
            TestContainersForm.Row row = rows.get(index);
            ContainerTypeView container = row == null ? null : containers.get(row.containerTypeId());
            if (container == null) {
                throw new TestContainersRefusedException("containerTypeId", "containerUnknown", index);
            }
            if (!seen.add(container.id())) {
                throw new TestContainersRefusedException("containerTypeId", "containerDuplicate", index);
            }
            if (!container.active() && !listedBefore.contains(container.id())) {
                throw new TestContainersRefusedException("containerTypeId", "containerInactive", index);
            }
            if (!container.domains().contains(domain)) {
                throw new TestContainersRefusedException("containerTypeId", "containerDomain", index);
            }
            String role = StringUtils.trimToEmpty(row.role()).toUpperCase(Locale.ROOT);
            if (!TestExpectedContainer.PREFERRED.equals(role) && !TestExpectedContainer.ALTERNATE.equals(role)) {
                throw new TestContainersRefusedException("role", "roleInvalid", index);
            }
            boolean yieldsOwnType = container.sampleTypes().stream().anyMatch(type -> ownTypes.contains(type.getId()));
            String usedAs = StringUtils.trimToNull(row.usedAsSampleTypeId());
            if (usedAs != null && yieldsOwnType) {
                throw new TestContainersRefusedException("usedAsSampleTypeId", "usedAsNotNeeded", index);
            }
            if (usedAs == null && !yieldsOwnType) {
                throw new TestContainersRefusedException("containerTypeId", "containerSampleType", index);
            }
            if (usedAs != null && !ownTypes.contains(usedAs)) {
                throw new TestContainersRefusedException("usedAsSampleTypeId", "usedAsNotTestType", index);
            }
            if (usedAs != null && TestExpectedContainer.PREFERRED.equals(role)) {
                throw new TestContainersRefusedException("role", "usedAsPreferred", index);
            }
            int count = row.count() == null ? 1 : row.count();
            if (count < 1 || count > MAX_COUNT) {
                throw new TestContainersRefusedException("count", "countInvalid", index);
            }
            BigDecimal minVolume = row.minVolume();
            if (minVolume != null
                    && (minVolume.compareTo(BigDecimal.ZERO) <= 0 || minVolume.compareTo(MAX_VOLUME) > 0)) {
                throw new TestContainersRefusedException("minVolume", "volumeInvalid", index);
            }
            String unit = StringUtils.trimToNull(row.volumeUnit());
            if (minVolume != null && unit == null) {
                throw new TestContainersRefusedException("volumeUnit", "unitRequired", index);
            }
            if (unit != null && unit.length() > 10) {
                throw new TestContainersRefusedException("volumeUnit", "tooLong", index);
            }
            if (TestExpectedContainer.PREFERRED.equals(role)) {
                preferred++;
            }
            TestExpectedContainer expected = new TestExpectedContainer();
            expected.setTestId(id);
            expected.setContainerTypeId(container.id());
            expected.setRole(role);
            expected.setCount(count);
            expected.setMinVolume(minVolume);
            expected.setVolumeUnit(minVolume == null ? null : unit);
            expected.setUsedAsSampleTypeId(usedAs == null ? null : Integer.valueOf(usedAs));
            expected.setDisplayOrder(index);
            expected.setSysUserId(sysUserId);
            accepted.add(expected);
        }
        if (!accepted.isEmpty() && preferred == 0) {
            throw new TestContainersRefusedException("role", "preferredRequired", null);
        }
        if (preferred > 1) {
            throw new TestContainersRefusedException("role", "onePreferred", null);
        }
        expectedContainerDAO.deleteForTest(id);
        accepted.forEach(expectedContainerDAO::insert);
        expectedContainerDAO.setNeedsOwnContainer(id, form != null && form.needsOwnContainer());
        return view(test);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SampleTypeContainersView> forSampleType(String sampleTypeId) {
        return findSampleType(sampleTypeId).map(this::sampleTypeView);
    }

    @Override
    @Transactional
    public SampleTypeContainersView setDefaultContainer(String sampleTypeId, Long containerTypeId) {
        TypeOfSample sampleType = findSampleType(sampleTypeId)
                .orElseThrow(() -> new TestContainersRefusedException("sampleTypeId", "notFound", null));
        Integer id = Integer.valueOf(sampleType.getId());
        if (containerTypeId != null) {
            ContainerTypeView container = containersById().get(containerTypeId);
            if (container == null) {
                throw new TestContainersRefusedException("defaultContainerTypeId", "containerUnknown", null);
            }
            boolean unchanged = expectedContainerDAO.defaultContainerOf(id).filter(containerTypeId::equals).isPresent();
            if (!container.active() && !unchanged) {
                throw new TestContainersRefusedException("defaultContainerTypeId", "containerInactive", null);
            }
            if (container.sampleTypes().stream().noneMatch(type -> sampleType.getId().equals(type.getId()))) {
                throw new TestContainersRefusedException("defaultContainerTypeId", "defaultContainerSampleType", null);
            }
        }
        expectedContainerDAO.setDefaultContainer(id, containerTypeId);
        return sampleTypeView(sampleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> testNamesUsing(Long containerTypeId) {
        return expectedContainerDAO.testIdsUsing(containerTypeId).stream()
                .map(testId -> testService.getTestById(String.valueOf(testId))).filter(Objects::nonNull)
                .map(TestServiceImpl::getLocalizedTestNameWithType).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private TestContainersView view(Test test) {
        Integer id = Integer.valueOf(test.getId());
        String domain = domainOf(test);
        Set<String> ownTypes = primarySampleTypeIds(test);
        Map<Long, ContainerTypeView> containers = containersById();
        Map<String, String> sampleTypeNames = new HashMap<>();
        List<TestContainersView.Row> rows = new ArrayList<>();
        List<TestContainersView.SecondarySampleType> secondary = new ArrayList<>();
        for (TestExpectedContainer expected : expectedContainerDAO.getForTest(id)) {
            ContainerTypeView container = containers.get(expected.getContainerTypeId());
            if (container == null) {
                continue;
            }
            String usedAs = expected.getUsedAsSampleTypeId() == null ? null
                    : String.valueOf(expected.getUsedAsSampleTypeId());
            String usedAsName = usedAs == null ? null : sampleTypeName(usedAs, sampleTypeNames);
            rows.add(new TestContainersView.Row(container.id(), container.code(), container.name(),
                    container.capColourHex(), container.capColourName(), container.active(), expected.getRole(),
                    expected.getCount(), expected.getMinVolume(), expected.getVolumeUnit(), usedAs, usedAsName,
                    container.sampleTypes()));
            if (usedAs != null) {
                for (IdValuePair yielded : container.sampleTypes()) {
                    if (!ownTypes.contains(yielded.getId())) {
                        secondary.add(new TestContainersView.SecondarySampleType(yielded.getId(), yielded.getValue(),
                                container.name(), usedAsName));
                    }
                }
            }
        }
        List<IdValuePair> primary = testService.getTypeOfSamples(test).stream()
                .map(type -> new IdValuePair(type.getId(), type.getLocalizedName())).toList();
        List<ContainerTypeView> yielding = new ArrayList<>();
        List<ContainerTypeView> other = new ArrayList<>();
        containers.values().stream().filter(ContainerTypeView::active)
                .filter(container -> container.domains().contains(domain)).forEach(container -> {
                    if (container.sampleTypes().stream().anyMatch(type -> ownTypes.contains(type.getId()))) {
                        yielding.add(container);
                    } else if (!container.sampleTypes().isEmpty()) {
                        other.add(container);
                    }
                });
        return new TestContainersView(test.getId(), domain, expectedContainerDAO.needsOwnContainer(id), rows, primary,
                secondary, yielding, other);
    }

    private SampleTypeContainersView sampleTypeView(TypeOfSample sampleType) {
        Integer id = Integer.valueOf(sampleType.getId());
        Map<Long, ContainerTypeView> containers = containersById();
        Long defaultContainer = expectedContainerDAO.defaultContainerOf(id).orElse(null);
        List<ContainerTypeView> choices = containers.values().stream()
                .filter(container -> container.active() || container.id().equals(defaultContainer))
                .filter(container -> container.sampleTypes().stream()
                        .anyMatch(type -> sampleType.getId().equals(type.getId())))
                .toList();
        Set<String> primaryTestIds = new HashSet<>();
        List<IdValuePair> primary = new ArrayList<>();
        for (TypeOfSampleTest link : typeOfSampleTestService.getTypeOfSampleTestsForSampleType(sampleType.getId())) {
            Test test = testService.getTestById(link.getTestId());
            if (test != null && primaryTestIds.add(test.getId())) {
                primary.add(new IdValuePair(test.getId(), TestServiceImpl.getLocalizedTestNameWithType(test)));
            }
        }
        primary.sort(Comparator.comparing(IdValuePair::getValue, String.CASE_INSENSITIVE_ORDER));
        Map<String, String> sampleTypeNames = new HashMap<>();
        List<SampleTypeContainersView.SecondaryTest> secondary = new ArrayList<>();
        for (TestExpectedContainer expected : expectedContainerDAO.getUsedAs()) {
            ContainerTypeView container = containers.get(expected.getContainerTypeId());
            String testId = String.valueOf(expected.getTestId());
            if (container == null || primaryTestIds.contains(testId)
                    || container.sampleTypes().stream().noneMatch(type -> sampleType.getId().equals(type.getId()))) {
                continue;
            }
            Test test = testService.getTestById(testId);
            if (test != null) {
                secondary.add(new SampleTypeContainersView.SecondaryTest(testId,
                        TestServiceImpl.getLocalizedTestNameWithType(test), container.name(),
                        sampleTypeName(String.valueOf(expected.getUsedAsSampleTypeId()), sampleTypeNames)));
            }
        }
        secondary.sort(
                Comparator.comparing(SampleTypeContainersView.SecondaryTest::testName, String.CASE_INSENSITIVE_ORDER));
        return new SampleTypeContainersView(sampleType.getId(), defaultContainer, choices, primary, secondary);
    }

    private Map<Long, ContainerTypeView> containersById() {
        Map<Long, ContainerTypeView> byId = new LinkedHashMap<>();
        containerTypeService.list(null, null, null, true).forEach(container -> byId.put(container.id(), container));
        return byId;
    }

    private Set<String> primarySampleTypeIds(Test test) {
        Set<String> ids = new HashSet<>();
        testService.getTypeOfSamples(test).forEach(type -> ids.add(type.getId()));
        return ids;
    }

    private String sampleTypeName(String sampleTypeId, Map<String, String> cache) {
        return cache.computeIfAbsent(sampleTypeId, id -> {
            TypeOfSample type = typeOfSampleService.getTypeOfSampleById(id);
            return type == null ? null : type.getLocalizedName();
        });
    }

    private static String domainOf(Test test) {
        return StringUtils.defaultIfBlank(test.getDomain(), "CLINICAL").toUpperCase(Locale.ROOT);
    }

    private Optional<Test> findTest(String testId) {
        if (!StringUtils.isNumeric(testId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(testService.getTestById(testId)).filter(test -> test.getId() != null);
    }

    private Optional<TypeOfSample> findSampleType(String sampleTypeId) {
        if (!StringUtils.isNumeric(sampleTypeId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(typeOfSampleService.getTypeOfSampleById(sampleTypeId));
    }
}
