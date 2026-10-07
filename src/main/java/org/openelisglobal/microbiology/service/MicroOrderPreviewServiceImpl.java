package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm;
import org.openelisglobal.microbiology.form.MicroOrderPreviewForm.*;
import org.openelisglobal.microbiology.form.MicroOrderPreviewRequestForm;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MicroOrderPreviewServiceImpl implements MicroOrderPreviewService {
    private final MicroOrderRoutingService routingService;
    private final TestService testService;
    private final TypeOfSampleService sampleTypeService;
    private final UserService userService;
    private final RoleService roleService;
    private final TestReflexService reflexService;

    public MicroOrderPreviewServiceImpl(MicroOrderRoutingService routingService, TestService testService,
            TypeOfSampleService sampleTypeService, UserService userService, RoleService roleService,
            TestReflexService reflexService) {
        this.routingService = routingService;
        this.testService = testService;
        this.sampleTypeService = sampleTypeService;
        this.userService = userService;
        this.roleService = roleService;
        this.reflexService = reflexService;
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
            return new CaseLine(group.key().testSectionId(), tests.get(0).getTestSection().getTestSectionName(),
                    group.specimenIndexes().stream().map(i -> new SpecimenLine(i, sampleNames.get(i))).toList(),
                    tests.stream().map(Test::getLocalizedName).toList(),
                    tests.stream().anyMatch(Test::isCollectedInSets));
        }).toList();
        if (cases.isEmpty()) {
            return new MicroOrderPreviewForm(cases, ordinary, List.of(), List.of());
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
                        && rule.getConditions().stream().anyMatch(c -> catalog.containsKey(c.getTestId())))
                .filter(rule -> rule.getActions() != null)
                .map(rule -> new ReflexLine(rule.getRuleName(),
                        rule.getActions().stream().filter(action -> action.getReflexTestId() != null)
                                .map(action -> testService.get(action.getReflexTestId()))
                                .filter(java.util.Objects::nonNull).map(Test::getLocalizedName).distinct().sorted()
                                .toList()))
                .filter(rule -> !rule.addedTests().isEmpty()).toList();
        return new MicroOrderPreviewForm(cases, ordinary, warnings, rules);
    }
}
