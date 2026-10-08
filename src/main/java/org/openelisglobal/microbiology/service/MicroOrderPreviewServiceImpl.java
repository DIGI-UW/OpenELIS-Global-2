package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm.*;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroOrderPreviewServiceImpl implements MicroOrderPreviewService {
    private final org.openelisglobal.dictionary.service.SampleContainerClassificationService containers;
    private final MicroCultureSetWarningService setWarningService;
    private final MicroOrderRoutingService routingService;
    private final TestService testService;
    private final TypeOfSampleService sampleTypeService;
    private final UserService userService;
    private final RoleService roleService;
    private final TestReflexService reflexService;
    private final RuleResultScope ruleScope;
    private final DictionaryService dictionaryService;
    private final TestResultComponentService componentService;

    public MicroOrderPreviewServiceImpl(MicroOrderRoutingService routingService, TestService testService,
            TypeOfSampleService sampleTypeService, UserService userService, RoleService roleService,
            TestReflexService reflexService, RuleResultScope ruleScope, DictionaryService dictionaryService,
            TestResultComponentService componentService, MicroCultureSetWarningService setWarningService,
            org.openelisglobal.dictionary.service.SampleContainerClassificationService containers) {
        this.setWarningService = setWarningService;
        this.containers = containers;
        this.routingService = routingService;
        this.testService = testService;
        this.sampleTypeService = sampleTypeService;
        this.userService = userService;
        this.roleService = roleService;
        this.reflexService = reflexService;
        this.ruleScope = ruleScope;
        this.dictionaryService = dictionaryService;
        this.componentService = componentService;
    }

    @Override
    public MicroOrderPreviewForm preview(MicroOrderPreviewRequestForm request, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (request == null || request.specimens == null) {
            throw new IllegalArgumentException("specimens is required");
        }
        var reception = roleService.getRoleByName(Constants.ROLE_RECEPTION);
        var units = reception == null ? List.<IdValuePair>of()
                : userService.getUserTestSections(userId, reception.getId());
        Set<String> permitted = units == null ? Set.of()
                : units.stream().map(IdValuePair::getId).collect(Collectors.toSet());
        Map<String, Test> catalog = new LinkedHashMap<>();
        List<MicroOrderDraftGrouping.Selection> selections = new ArrayList<>();
        List<TestLine> ordinary = new ArrayList<>();
        List<String> sampleNames = new ArrayList<>();
        for (int index = 0; index < request.specimens.size(); index++) {
            var input = request.specimens.get(index);
            if (input == null || input.sampleTypeId == null || input.testIds == null) {
                throw new IllegalArgumentException("Each specimen requires sampleTypeId and testIds");
            }
            var type = sampleTypeService.get(input.sampleTypeId);
            if (type == null) {
                throw new IllegalArgumentException("Unknown sample type");
            }
            SampleItem specimen = new SampleItem();
            specimen.setTypeOfSample(type);
            sampleNames.add(type.getLocalizedName());
            List<Test> selected = new ArrayList<>();
            for (String testId : new LinkedHashSet<>(input.testIds)) {
                if (testId == null || testId.isBlank()) {
                    throw new IllegalArgumentException("Each test requires a catalog ID");
                }
                Test test = catalog.computeIfAbsent(testId, testService::get);
                if (test == null) {
                    throw new IllegalArgumentException("Unknown preview test");
                }
                if (test.getTestSection() == null || !permitted.contains(test.getTestSection().getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
                selected.add(test);
                if (!test.isOpensMicrobiologyCase()) {
                    ordinary.add(new TestLine(index, test.getId(), test.getLocalizedName()));
                }
            }
            selections.add(new MicroOrderDraftGrouping.Selection(specimen, selected));
        }
        List<CaseLine> cases = routingService.previewNewOrder(selections).stream().map(group -> {
            List<Test> tests = group.testIds().stream().map(catalog::get).toList();
            List<org.openelisglobal.microbiology.form.MicroCaseSpecimenForm> bottles = group.specimenIndexes().stream()
                    .filter(i -> selections.get(i).tests().stream()
                            .anyMatch(test -> test.isCollectedInSets() && group.testIds().contains(test.getId())))
                    .map(i -> {
                        var bottle = new org.openelisglobal.microbiology.form.MicroCaseSpecimenForm();
                        bottle.collectedInSets = true;
                        bottle.cultureSetNumber = request.specimens.get(i).cultureSetNumber;
                        if (bottle.cultureSetNumber != null && bottle.cultureSetNumber < 1) {
                            throw new IllegalArgumentException("Set number must be positive");
                        }
                        var input = request.specimens.get(i);
                        bottle.containerType = input.container;
                        bottle.containerPopulation = containers.population(input.container);
                        bottle.bodySite = input.bodySite;
                        if (hasText(input.collectionDate) && hasText(input.collectionTime)) {
                            var date = java.time.LocalDate.parse(input.collectionDate);
                            var time = java.time.LocalTime.parse(input.collectionTime);
                            bottle.collectionDate = java.sql.Timestamp.valueOf(date.atTime(time));
                        }
                        bottle.specimenType = sampleNames.get(i);
                        return bottle;
                    }).toList();
            return new CaseLine(group.key().testSectionId(), tests.get(0).getTestSection().getTestSectionName(),
                    group.specimenIndexes().stream().map(i -> new SpecimenLine(i, sampleNames.get(i))).toList(),
                    tests.stream().map(Test::getLocalizedName).toList(),
                    tests.stream().anyMatch(Test::isCollectedInSets), bottles, setWarningService.evaluate(bottles));
        }).toList();
        if (cases.isEmpty()) {
            return new MicroOrderPreviewForm(cases, ordinary, List.of(), List.of(), List.of());
        }
        List<SplitWarning> warnings = new ArrayList<>();
        for (int index = 0; index < selections.size(); index++) {
            int specimenIndex = index;
            var labs = cases.stream().filter(c -> c.specimens().stream().anyMatch(s -> s.index() == specimenIndex))
                    .collect(Collectors.toMap(CaseLine::labUnitId, CaseLine::labUnitName, (first, ignored) -> first,
                            LinkedHashMap::new));
            if (labs.size() > 1) {
                warnings.add(new SplitWarning(index, List.copyOf(labs.values())));
            }
        }
        List<ReflexLine> rules = reflexService.getAllReflexRules().stream()
                .filter(rule -> !Boolean.FALSE.equals(rule.getActive()) && rule.getConditions() != null
                        && rule.getConditions().stream().anyMatch(c -> request.specimens.stream()
                                .anyMatch(sample -> sample.testIds.contains(c.getTestId())
                                        && (!hasText(c.getSampleId()) || c.getSampleId().equals(sample.sampleTypeId)))))
                .filter(rule -> rule.getActions() != null)
                .map(rule -> new ReflexLine(rule.getRuleName(),
                        rule.getOverall() == null ? "ANY" : rule.getOverall().name(),
                        rule.getConditions().stream().map(this::describeCondition)
                                .sorted(java.util.Comparator.comparing(ReflexCondition::testName)
                                        .thenComparing(ReflexCondition::componentLabel))
                                .toList(),
                        rule.getActions().stream().filter(action -> action.getReflexTestId() != null)
                                .map(action -> testService.get(action.getReflexTestId()))
                                .filter(java.util.Objects::nonNull).map(Test::getLocalizedName).distinct().sorted()
                                .toList()))
                .filter(rule -> !rule.addedTests().isEmpty()).toList();
        Map<String, Long> workByUnit = selections.stream().flatMap(selection -> selection.tests().stream())
                .collect(Collectors.groupingBy(test -> test.getTestSection().getId(), Collectors.counting()));
        List<NewUnitWarning> newUnits = cases.stream()
                .filter(line -> workByUnit.getOrDefault(line.labUnitId(), 0L) == 1L)
                .map(line -> new NewUnitWarning(line.labUnitId(), line.labUnitName(), line.testNames().get(0)))
                .toList();
        return new MicroOrderPreviewForm(cases, ordinary, warnings, rules, newUnits);
    }

    private ReflexCondition describeCondition(ReflexRuleCondition condition) {
        Test test = testService.get(condition.getTestId());
        if (test == null || condition.getRelation() == null) {
            throw new IllegalArgumentException("Reflex condition requires an existing test and relation");
        }
        String componentLabel = "";
        if (hasText(condition.getComponentId())) {
            var component = componentService.get(condition.getComponentId());
            if (component == null || !test.getId().equals(component.getTestId())) {
                throw new IllegalArgumentException("Reflex condition component must belong to its test");
            }
            componentLabel = component.getLabel();
        }
        String sampleTypeName = "";
        if (hasText(condition.getSampleId())) {
            var type = sampleTypeService.get(condition.getSampleId());
            if (type == null) {
                throw new IllegalArgumentException("Reflex condition sample type no longer exists");
            }
            sampleTypeName = type.getLocalizedName();
        }
        String value = condition.getValue();
        String relation = condition.getRelation().name();
        boolean range = "INSIDE_NORMAL_RANGE".equals(relation) || "OUTSIDE_NORMAL_RANGE".equals(relation);
        String resultType = ruleScope.resultTypeForComponent(test.getId(), condition.getComponentId(),
                testService.getResultType(test));
        if (!range && "D".equals(resultType)) {
            var dictionary = dictionaryService.getDictionaryById(value);
            if (dictionary == null) {
                throw new IllegalArgumentException("Reflex condition dictionary answer no longer exists");
            }
            value = dictionary.getLocalizedName();
        }
        return new ReflexCondition(test.getLocalizedName(), sampleTypeName, componentLabel, relation,
                range ? "" : value, "BETWEEN".equals(relation) ? condition.getValue2() : "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
