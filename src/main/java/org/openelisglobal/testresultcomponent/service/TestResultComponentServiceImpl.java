package org.openelisglobal.testresultcomponent.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.dao.TestResultComponentDAO;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestResultComponentServiceImpl extends AuditableBaseObjectServiceImpl<TestResultComponent, String>
        implements TestResultComponentService {

    private static final String PRIMARY_CODE = "PRIMARY";

    @Autowired
    protected TestResultComponentDAO baseObjectDAO;

    @Autowired
    private TestResultInterpretationService interpretationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private ResultLimitService resultLimitService;

    TestResultComponentServiceImpl() {
        super(TestResultComponent.class);
    }

    @Override
    protected TestResultComponentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultComponent> getComponentsByTestId(String testId) {
        return baseObjectDAO.getComponentsByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultComponent> getActiveComponentsByTestId(String testId) {
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResultComponent getByTestIdAndCode(String testId, String code) {
        return baseObjectDAO.getByTestIdAndCode(testId, code);
    }

    @Override
    @Transactional
    public List<TestResultComponent> saveComponentsForTest(String testId, List<TestResultComponent> desired,
            String sysUserId) {
        List<TestResultComponent> existing = baseObjectDAO.getActiveComponentsByTestId(testId);
        Map<String, TestResultComponent> existingById = new HashMap<>();
        for (TestResultComponent e : existing) {
            existingById.put(e.getId(), e);
        }
        Set<String> keptIds = new HashSet<>();
        for (TestResultComponent d : desired) {
            TestResultComponent match = (d.getId() != null && existingById.containsKey(d.getId()))
                    ? baseObjectDAO.get(d.getId()).orElse(null)
                    : null;
            if (match != null) {
                match.setCode(d.getCode());
                match.setLabel(d.getLabel());
                match.setDisplayOrder(d.getDisplayOrder());
                match.setResultType(d.getResultType());
                match.setUomId(d.getUomId());
                match.setSignificantDigits(d.getSignificantDigits());
                match.setDefaultResult(d.getDefaultResult());
                match.setAllowMultipleReadings(d.getAllowMultipleReadings());
                match.setIsPrimary(d.getIsPrimary());
                match.setShowOnReport(d.getShowOnReport());
                match.setSysUserId(sysUserId);
                update(match);
                keptIds.add(match.getId());
            } else {
                // The (test_id, code) UNIQUE slot may already be occupied — by a
                // soft-deleted row (re-added code) or by an active row the caller
                // referenced without an id (e.g. the FR-56 pre-seeded PRIMARY).
                // Either way the code is the natural key: reconcile that row in
                // place — inserting a fresh one would violate the constraint.
                TestResultComponent slot = baseObjectDAO.getByTestIdAndCode(testId, d.getCode());
                if (slot != null) {
                    slot.setLabel(d.getLabel());
                    slot.setDisplayOrder(d.getDisplayOrder());
                    slot.setResultType(d.getResultType());
                    slot.setUomId(d.getUomId());
                    slot.setSignificantDigits(d.getSignificantDigits());
                    slot.setDefaultResult(d.getDefaultResult());
                    slot.setAllowMultipleReadings(d.getAllowMultipleReadings());
                    slot.setIsPrimary(d.getIsPrimary());
                    slot.setShowOnReport(d.getShowOnReport());
                    slot.setIsActive("Y");
                    slot.setSysUserId(sysUserId);
                    update(slot);
                    keptIds.add(slot.getId());
                } else {
                    d.setId(UUID.randomUUID().toString());
                    d.setTestId(testId);
                    d.setIsActive("Y");
                    d.setSysUserId(sysUserId);
                    insert(d);
                }
            }
        }
        for (TestResultComponent e : existing) {
            if (!keptIds.contains(e.getId())) {
                TestResultComponent fresh = baseObjectDAO.get(e.getId()).orElse(null);
                if (fresh != null) {
                    fresh.setIsActive("N");
                    fresh.setSysUserId(sysUserId);
                    update(fresh);
                }
            }
        }
        ensureSinglePrimary(testId, sysUserId);
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }

    @Override
    @Transactional
    public List<TestResultComponent> saveSampleResults(String testId, List<TestResultComponent> components,
            Map<String, List<TestResultInterpretation>> interpretationsByComponentCode,
            Map<String, List<TestResult>> optionsByComponentCode, String sysUserId) {
        // One transaction: components first (so newly inserted rows get ids), then
        // each component's interpretations + select-list options, keyed by the
        // component's unique code.
        saveComponentsForTest(testId, components, sysUserId);
        Map<String, String> codeToId = new HashMap<>();
        for (TestResultComponent c : baseObjectDAO.getActiveComponentsByTestId(testId)) {
            codeToId.put(c.getCode(), c.getId());
        }
        if (interpretationsByComponentCode != null) {
            for (Map.Entry<String, List<TestResultInterpretation>> entry : interpretationsByComponentCode.entrySet()) {
                String componentId = codeToId.get(entry.getKey());
                if (componentId != null) {
                    interpretationService.saveInterpretationsForComponent(componentId, entry.getValue(), sysUserId);
                }
            }
        }
        if (optionsByComponentCode != null && !optionsByComponentCode.isEmpty()) {
            Test test = testService.getTestById(testId);
            for (Map.Entry<String, List<TestResult>> entry : optionsByComponentCode.entrySet()) {
                String componentId = codeToId.get(entry.getKey());
                if (componentId != null) {
                    testResultService.saveOptionsForComponent(test, componentId, entry.getValue(), sysUserId);
                }
            }
        }
        syncLegacyTestFields(testId, sysUserId);
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }

    /**
     * Mirror the PRIMARY component's unit-of-measure and significant digits back
     * onto the legacy columns the old Test Modify page still reads from
     * ({@code test.uom_id} and {@code test_result.significant_digits}). The M1
     * backfill seeded the PRIMARY component <em>from</em> those columns; this is
     * the inverse, keeping both editors consistent during the OGC-949 transition.
     * Falls back to the lowest-display-order component when no PRIMARY code exists.
     */
    private void syncLegacyTestFields(String testId, String sysUserId) {
        TestResultComponent primary = findPrimaryComponent(testId);
        if (primary == null) {
            return;
        }
        Test test = testService.getTestById(testId);
        if (test == null) {
            return;
        }
        UnitOfMeasure uom = primary.getUomId() == null ? null
                : unitOfMeasureService.getUnitOfMeasureById(primary.getUomId());
        test.setUnitOfMeasure(uom);
        test.setSysUserId(sysUserId);
        testService.update(test);

        // Every non-dictionary component gets its own test_result row (typed per
        // component, linked via component_id) so result entry can bind one Result
        // field per component. Dictionary components get their rows from their
        // select-list options. Rows with a NULL component_id are legacy rows and
        // belong to the primary.
        List<TestResultComponent> components = baseObjectDAO.getActiveComponentsByTestId(testId);
        List<TestResult> testResults = testResultService.getAllActiveTestResultsPerTest(test);
        for (TestResultComponent component : components) {
            boolean isPrimaryComponent = component.getId().equals(primary.getId());
            List<TestResult> componentRows = new ArrayList<>();
            for (TestResult tr : testResults) {
                if (component.getId().equals(tr.getComponentId())
                        || (isPrimaryComponent && tr.getComponentId() == null)) {
                    componentRows.add(tr);
                }
            }
            String resultType = component.getResultType();
            String significantDigits = component.getSignificantDigits() == null ? null
                    : String.valueOf(component.getSignificantDigits());
            boolean dictionary = resultType != null
                    && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(resultType);

            if (dictionary) {
                // Option rows carry their own type + dictionary value; a value-less
                // row here is a stale placeholder from a previous non-dictionary
                // type — deactivate it rather than leaving a broken dictionary row.
                for (TestResult tr : componentRows) {
                    boolean hasValue = tr.getValue() != null && !tr.getValue().trim().isEmpty();
                    if (!hasValue) {
                        tr.setIsActive(false);
                        tr.setSysUserId(sysUserId);
                        testResultService.update(tr);
                    }
                }
                continue;
            }

            if (componentRows.isEmpty()) {
                if (resultType != null) {
                    TestResult tr = new TestResult();
                    tr.setTest(test);
                    tr.setTestResultType(resultType);
                    tr.setSortOrder(
                            String.valueOf(component.getDisplayOrder() == null ? 1 : component.getDisplayOrder() + 1));
                    tr.setIsActive(true);
                    tr.setSignificantDigits(significantDigits);
                    tr.setComponentId(component.getId());
                    tr.setSysUserId(sysUserId);
                    testResultService.insert(tr);
                }
            } else {
                for (TestResult tr : componentRows) {
                    tr.setSignificantDigits(significantDigits);
                    if (resultType != null) {
                        tr.setTestResultType(resultType);
                    }
                    if (tr.getComponentId() == null) {
                        tr.setComponentId(component.getId());
                    }
                    tr.setSysUserId(sysUserId);
                    testResultService.update(tr);
                }
            }
        }
    }

    @Override
    @Transactional
    public void syncPrimaryComponentFromLegacy(String testId, String sysUserId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            return;
        }
        List<TestResult> testResults = new ArrayList<>(testResultService.getActiveTestResultsByTest(testId));
        testResults.sort((a, b) -> Long.compare(parseId(b.getId()), parseId(a.getId())));
        String uomId = test.getUnitOfMeasure() == null ? null : test.getUnitOfMeasure().getId();
        String resultType = latestResultType(testResults);
        Integer significantDigits = latestSignificantDigits(testResults);

        TestResultComponent primary = findPrimaryComponent(testId);
        if (primary == null) {
            // Legacy created this test outside the new editor (or before the M1
            // backfill ran), so it has no component yet — create its PRIMARY.
            primary = new TestResultComponent();
            primary.setTestId(testId);
            primary.setCode(PRIMARY_CODE);
            primary.setLabel(primaryLabel(test));
            primary.setDisplayOrder(0);
            primary.setResultType(resultType);
            primary.setUomId(uomId);
            primary.setSignificantDigits(significantDigits);
            primary.setIsActive("Y");
            primary.setIsPrimary(true);
            primary.setSysUserId(sysUserId);
            insert(primary);
        } else {
            primary.setUomId(uomId);
            primary.setResultType(resultType);
            primary.setSignificantDigits(significantDigits);
            primary.setSysUserId(sysUserId);
            update(primary);
        }

        // Legacy writes options (test_result) and ranges (result_limits) with a NULL
        // component_id; repoint those onto the PRIMARY component so the new editor,
        // which scopes both by component_id, surfaces them.
        String primaryId = primary.getId();
        for (TestResult tr : testResults) {
            if (tr.getComponentId() == null) {
                tr.setComponentId(primaryId);
                tr.setSysUserId(sysUserId);
                testResultService.update(tr);
            }
        }
        for (ResultLimit rl : resultLimitService.getAllResultLimitsForTest(testId)) {
            if (rl.getComponentId() == null) {
                rl.setComponentId(primaryId);
                rl.setSysUserId(sysUserId);
                resultLimitService.update(rl);
            }
        }
        ensureSinglePrimary(testId, sysUserId);
    }

    private TestResultComponent findPrimaryComponent(String testId) {
        List<TestResultComponent> components = baseObjectDAO.getActiveComponentsByTestId(testId);
        return pickPrimary(components);
    }

    /**
     * The primary component: the explicit is_primary flag first, then the legacy
     * PRIMARY code, then the lowest-display-order component (defensive fallback for
     * data predating the flag).
     */
    private TestResultComponent pickPrimary(List<TestResultComponent> components) {
        if (components == null || components.isEmpty()) {
            return null;
        }
        for (TestResultComponent c : components) {
            if (c.getIsPrimary()) {
                return c;
            }
        }
        for (TestResultComponent c : components) {
            if (PRIMARY_CODE.equals(c.getCode())) {
                return c;
            }
        }
        return components.stream()
                .min(Comparator
                        .comparingInt(c -> c.getDisplayOrder() == null ? Integer.MAX_VALUE : c.getDisplayOrder()))
                .orElse(components.get(0));
    }

    /**
     * Guarantee exactly one active component carries is_primary. Called after any
     * component-set change so the flag stays consistent even though the editor DTO
     * doesn't send it.
     */
    private void ensureSinglePrimary(String testId, String sysUserId) {
        List<TestResultComponent> components = baseObjectDAO.getActiveComponentsByTestId(testId);
        TestResultComponent primary = pickPrimary(components);
        if (primary == null) {
            return;
        }
        for (TestResultComponent c : components) {
            boolean shouldBePrimary = c.getId().equals(primary.getId());
            if (c.getIsPrimary() != shouldBePrimary) {
                c.setIsPrimary(shouldBePrimary);
                c.setSysUserId(sysUserId);
                update(c);
            }
        }
    }

    private static String latestResultType(List<TestResult> newestFirst) {
        for (TestResult tr : newestFirst) {
            if (tr.getTestResultType() != null && !tr.getTestResultType().trim().isEmpty()) {
                return tr.getTestResultType();
            }
        }
        return null;
    }

    private static Integer latestSignificantDigits(List<TestResult> newestFirst) {
        for (TestResult tr : newestFirst) {
            Integer parsed = parseSignificantDigits(tr.getSignificantDigits());
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static long parseId(String id) {
        if (id == null) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            return Long.MIN_VALUE;
        }
    }

    private static String primaryLabel(Test test) {
        String name = test.getName();
        return name == null || name.trim().isEmpty() ? PRIMARY_CODE : name;
    }

    private static Integer parseSignificantDigits(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @Transactional
    public void copyComponentsFromTest(String sourceTestId, String targetTestId, String sysUserId) {
        Test target = testService.getTestById(targetTestId);
        for (TestResultComponent src : baseObjectDAO.getActiveComponentsByTestId(sourceTestId)) {
            TestResultComponent existing = getByTestIdAndCode(targetTestId, src.getCode());
            // A configured component (it has a result type) is never clobbered.
            // A blank row — the FR-56 pre-seeded PRIMARY, or a soft-deleted stub —
            // is reconciled from the source instead, so variant creation copies
            // the source's primary configuration onto the pre-seeded row.
            if (existing != null && existing.getResultType() != null && !existing.getResultType().isBlank()
                    && "Y".equals(existing.getIsActive())) {
                continue;
            }
            TestResultComponent copy = existing != null ? existing : new TestResultComponent();
            copy.setTestId(targetTestId);
            copy.setCode(src.getCode());
            copy.setLabel(src.getLabel());
            copy.setDisplayOrder(src.getDisplayOrder());
            copy.setResultType(src.getResultType());
            copy.setUomId(src.getUomId());
            copy.setSignificantDigits(src.getSignificantDigits());
            copy.setDefaultResult(src.getDefaultResult());
            copy.setAllowMultipleReadings(src.getAllowMultipleReadings());
            copy.setIsPrimary(src.getIsPrimary());
            copy.setShowOnReport(src.getShowOnReport());
            copy.setIsActive("Y");
            copy.setSysUserId(sysUserId);
            if (existing != null) {
                update(copy);
            } else {
                insert(copy);
            }

            List<TestResultInterpretation> interpCopies = new ArrayList<>();
            for (TestResultInterpretation si : interpretationService.getActiveByComponentId(src.getId())) {
                TestResultInterpretation ci = new TestResultInterpretation();
                ci.setValueMatch(si.getValueMatch());
                ci.setInterpretationText(si.getInterpretationText());
                ci.setSeverity(si.getSeverity());
                ci.setColor(si.getColor());
                ci.setDisplayOrder(si.getDisplayOrder());
                interpCopies.add(ci);
            }
            interpretationService.saveInterpretationsForComponent(copy.getId(), interpCopies, sysUserId);

            List<TestResult> optionCopies = new ArrayList<>();
            for (TestResult so : testResultService.getActiveOptionsByComponentId(src.getId())) {
                TestResult co = new TestResult();
                co.setValue(so.getValue());
                co.setSortOrder(so.getSortOrder());
                co.setIsNormal(so.getIsNormal());
                co.setTestResultType(so.getTestResultType());
                optionCopies.add(co);
            }
            testResultService.saveOptionsForComponent(target, copy.getId(), optionCopies, sysUserId);

            // Non-dictionary components need their type-carrying test_result
            // placeholder row (result entry derives the widget from it) — the
            // option copy above only covers dictionary rows, so create it here,
            // mirroring the saveSampleResults reconciliation.
            String type = copy.getResultType();
            if (type != null && !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type)) {
                TestResult placeholder = new TestResult();
                placeholder.setTest(target);
                placeholder.setTestResultType(type);
                placeholder
                        .setSortOrder(String.valueOf(copy.getDisplayOrder() == null ? 1 : copy.getDisplayOrder() + 1));
                placeholder.setIsActive(true);
                placeholder.setSignificantDigits(
                        copy.getSignificantDigits() == null ? null : String.valueOf(copy.getSignificantDigits()));
                placeholder.setComponentId(copy.getId());
                placeholder.setSysUserId(sysUserId);
                testResultService.insert(placeholder);
            }
        }
        ensureSinglePrimary(targetTestId, sysUserId);
    }
}
