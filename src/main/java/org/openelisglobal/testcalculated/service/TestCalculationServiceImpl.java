package org.openelisglobal.testcalculated.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.dao.TestCalculationDAO;
import org.openelisglobal.testcalculated.valueholder.Calculation;
import org.openelisglobal.testcalculated.valueholder.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCalculationServiceImpl extends AuditableBaseObjectServiceImpl<Calculation, Integer>
        implements TestCalculationService {

    public static final String DUPLICATE_NAME = "calculation.error.duplicateName";
    public static final String DUPLICATE_NAME_INACTIVE = "calculation.error.duplicateNameInactive";
    public static final String SELF_REFERENCE = "calculation.error.selfReference";
    public static final String DUPLICATE_DESTINATION = "calculation.error.duplicateDestination";
    public static final String CYCLE = "calculation.error.cycle";

    @Autowired
    TestCalculationDAO testCalculationDAOdao;

    @Autowired
    private RuleResultScope ruleResultScope;

    @Autowired
    private TestService testService;

    public TestCalculationServiceImpl() {
        super(Calculation.class);
    }

    @Override
    protected TestCalculationDAO getBaseObjectDAO() {
        return testCalculationDAOdao;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRule(Calculation calculation, boolean willBeActive) {
        List<Calculation> others = getAll().stream().filter(other -> !sameRule(other, calculation))
                .collect(Collectors.toList());
        String name = StringUtils.trimToEmpty(calculation.getName());
        for (Calculation other : others) {
            if (!name.isEmpty() && name.equals(StringUtils.trim(other.getName()))) {
                throw new CalculationRuleViolation(
                        Boolean.TRUE.equals(other.getActive()) ? DUPLICATE_NAME : DUPLICATE_NAME_INACTIVE,
                        Map.of("name", name));
            }
        }
        if (calculation.getTestId() == null) {
            return;
        }
        Map<String, String> primaries = new HashMap<>();
        Measurement destination = destinationOf(calculation, primaries);
        for (Measurement operand : operandsOf(calculation, primaries)) {
            if (operand.sameAs(destination)) {
                throw new CalculationRuleViolation(SELF_REFERENCE,
                        Map.of("rule", name, "test", testName(destination.testId())));
            }
        }
        if (!willBeActive) {
            return;
        }
        List<Calculation> active = others.stream()
                .filter(other -> Boolean.TRUE.equals(other.getActive()) && other.getTestId() != null)
                .collect(Collectors.toList());
        for (Calculation other : active) {
            if (destinationOf(other, primaries).sameAs(destination)) {
                throw new CalculationRuleViolation(DUPLICATE_DESTINATION, Map.of("rule", name, "test",
                        testName(destination.testId()), "other", StringUtils.defaultString(other.getName())));
            }
        }
        List<Calculation> loop = loopBackTo(calculation, calculation, active, primaries, new ArrayList<>(),
                new HashSet<>());
        if (loop != null) {
            List<String> names = new ArrayList<>();
            names.add(name);
            loop.forEach(step -> names.add(StringUtils.defaultString(step.getName())));
            names.add(name);
            throw new CalculationRuleViolation(CYCLE, Map.of("rule", name, "cycle", String.join(" → ", names)));
        }
    }

    /**
     * The active rules, in order, that take the candidate's result and lead back to
     * one of its own inputs; null when there is no such chain.
     */
    private List<Calculation> loopBackTo(Calculation candidate, Calculation from, List<Calculation> active,
            Map<String, String> primaries, List<Calculation> path, Set<Integer> visited) {
        for (Calculation next : active) {
            if (visited.contains(next.getId()) || !feeds(from, next, primaries)) {
                continue;
            }
            visited.add(next.getId());
            path.add(next);
            if (feeds(next, candidate, primaries)) {
                return path;
            }
            List<Calculation> found = loopBackTo(candidate, next, active, primaries, path, visited);
            if (found != null) {
                return found;
            }
            path.remove(path.size() - 1);
        }
        return null;
    }

    /**
     * Whether the result {@code writer} produces is one of {@code reader}'s inputs.
     */
    private boolean feeds(Calculation writer, Calculation reader, Map<String, String> primaries) {
        Measurement written = destinationOf(writer, primaries);
        return operandsOf(reader, primaries).stream().anyMatch(operand -> operand.sameAs(written));
    }

    private Measurement destinationOf(Calculation calculation, Map<String, String> primaries) {
        String testId = String.valueOf(calculation.getTestId());
        return new Measurement(testId, componentOrPrimary(testId, calculation.getComponentId(), primaries),
                calculation.getSampleId() == null ? null : calculation.getSampleId().toString());
    }

    private List<Measurement> operandsOf(Calculation calculation, Map<String, String> primaries) {
        List<Measurement> operands = new ArrayList<>();
        if (calculation.getOperations() == null) {
            return operands;
        }
        for (Operation operation : calculation.getOperations()) {
            if (Operation.OperationType.TEST_RESULT.equals(operation.getType())
                    && StringUtils.isNotBlank(operation.getValue())) {
                String testId = operation.getValue().trim();
                operands.add(new Measurement(testId, componentOrPrimary(testId, operation.getComponentId(), primaries),
                        operation.getScopedSampleTypeId()));
            }
        }
        return operands;
    }

    /**
     * A rule that names no component reads or writes the test's primary, so both
     * spellings of the same measurement compare equal.
     */
    private String componentOrPrimary(String testId, String componentId, Map<String, String> primaries) {
        if (StringUtils.isNotBlank(componentId)) {
            return componentId;
        }
        return primaries.computeIfAbsent(testId,
                id -> StringUtils.defaultString(ruleResultScope.primaryComponentId(id)));
    }

    private String testName(String testId) {
        Test test = testService.get(testId);
        return test == null ? testId : TestServiceImpl.getLocalizedTestNameWithType(test);
    }

    private static boolean sameRule(Calculation a, Calculation b) {
        return a.getId() != null && a.getId().equals(b.getId());
    }

    /**
     * One measurement: a test, its component, and the specimen it is taken on. A
     * rule that names no specimen covers every specimen of the test.
     */
    private record Measurement(String testId, String componentId, String sampleTypeId) {
        boolean sameAs(Measurement other) {
            return testId.equals(other.testId)
                    && Objects.equals(StringUtils.trimToEmpty(componentId), StringUtils.trimToEmpty(other.componentId))
                    && (sampleTypeId == null || other.sampleTypeId == null || sampleTypeId.equals(other.sampleTypeId));
        }
    }
}
