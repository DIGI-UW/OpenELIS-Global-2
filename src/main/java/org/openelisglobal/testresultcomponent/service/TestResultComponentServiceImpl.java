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
                match.setLod(d.getLod());
                match.setLoq(d.getLoq());
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
                    slot.setLod(d.getLod());
                    slot.setLoq(d.getLoq());
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
        Map<String, Map<String, String>> storedValueByTypedValue = new HashMap<>();
        if (optionsByComponentCode != null && !optionsByComponentCode.isEmpty()) {
            Test test = testService.getTestById(testId);
            for (Map.Entry<String, List<TestResult>> entry : optionsByComponentCode.entrySet()) {
                String componentId = codeToId.get(entry.getKey());
                if (componentId != null) {
                    List<String> typed = new ArrayList<>();
                    for (TestResult option : entry.getValue()) {
                        typed.add(option.getValue());
                    }
                    testResultService.saveOptionsForComponent(test, componentId, entry.getValue(), sysUserId);
                    Map<String, String> stored = new HashMap<>();
                    for (int i = 0; i < typed.size(); i++) {
                        if (typed.get(i) != null) {
                            stored.put(typed.get(i).trim(), entry.getValue().get(i).getValue());
                        }
                    }
                    storedValueByTypedValue.put(componentId, stored);
                }
            }
        }
        syncLegacyTestFields(testId, sysUserId);
        if (optionsByComponentCode != null) {
            syncDefaultTestResult(testId, storedValueByTypedValue, sysUserId);
            syncDictionaryNormals(testId, storedValueByTypedValue.keySet(), sysUserId);
        }
        dropUnreachableDictionaryRanges(testId, sysUserId);
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }

    /**
     * Result entry pre-selects {@code test.default_test_result_id}, so the primary
     * select-list component's default is mirrored onto it as the option row holding
     * that value. A default typed as a new free-text option is first repointed at
     * the dictionary entry the option was stored as. Any other primary type, or a
     * default no option offers, clears it.
     */
    private void syncDefaultTestResult(String testId, Map<String, Map<String, String>> storedValueByTypedValue,
            String sysUserId) {
        TestResultComponent primary = findPrimaryComponent(testId);
        Test test = testService.getTestById(testId);
        if (primary == null || test == null) {
            return;
        }
        TestResult defaultOption = null;
        String defaultValue = primary.getDefaultResult() == null ? "" : primary.getDefaultResult().trim();
        if (!defaultValue.isEmpty() && primary.getResultType() != null
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(primary.getResultType())) {
            String stored = storedValueByTypedValue.getOrDefault(primary.getId(), Map.of()).get(defaultValue);
            if (stored != null && !stored.equals(primary.getDefaultResult())) {
                primary.setDefaultResult(stored);
                primary.setSysUserId(sysUserId);
                update(primary);
                defaultValue = stored;
            }
            for (TestResult option : testResultService.getActiveOptionsByComponentId(primary.getId())) {
                if (defaultValue.equals(option.getValue())) {
                    defaultOption = option;
                    break;
                }
            }
        }
        TestResult current = test.getDefaultTestResult();
        String currentId = current == null ? null : current.getId();
        String wantedId = defaultOption == null ? null : defaultOption.getId();
        if (java.util.Objects.equals(currentId, wantedId)) {
            return;
        }
        test.setDefaultTestResult(defaultOption);
        test.setSysUserId(sysUserId);
        testService.update(test);
    }

    /**
     * Result flagging, validation and reflex rules judge a select-list result
     * normal when it equals the reference limit's {@code dictionaryNormalId}; the
     * option marked Normal is that value. For each saved select-list component, a
     * limit on a value the component still offers but no longer marks Normal is
     * pointed at the Normal option, one is created when no limit holds it, and all
     * are removed when no option is marked Normal. Limits on values the component
     * stopped offering are left to {@link #dropUnreachableDictionaryRanges}. Legacy
     * limits without a component id belong to the primary.
     */
    private void syncDictionaryNormals(String testId, Set<String> savedComponentIds, String sysUserId) {
        List<TestResultComponent> components = baseObjectDAO.getActiveComponentsByTestId(testId);
        TestResultComponent primary = pickPrimary(components);
        List<ResultLimit> limits = resultLimitService.getAllResultLimitsForTest(testId);
        for (TestResultComponent component : components) {
            String type = component.getResultType();
            if (!savedComponentIds.contains(component.getId()) || type == null
                    || !TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type)) {
                continue;
            }
            String normalValue = null;
            Set<String> offered = new HashSet<>();
            for (TestResult option : testResultService.getActiveOptionsByComponentId(component.getId())) {
                if (option.getValue() == null || option.getValue().isBlank()) {
                    continue;
                }
                offered.add(option.getValue().trim());
                if (normalValue == null && Boolean.TRUE.equals(option.getIsNormal())) {
                    normalValue = option.getValue().trim();
                }
            }
            boolean isPrimary = primary != null && primary.getId().equals(component.getId());
            boolean normalHeld = false;
            for (ResultLimit limit : limits) {
                boolean owned = component.getId().equals(limit.getComponentId())
                        || (isPrimary && limit.getComponentId() == null);
                if (!owned || limit.getDictionaryNormalId() == null || limit.getDictionaryNormalId().isBlank()) {
                    continue;
                }
                String held = limit.getDictionaryNormalId().trim();
                if (normalValue == null) {
                    resultLimitService.delete(limit.getId(), sysUserId);
                } else if (normalValue.equals(held)) {
                    normalHeld = true;
                } else if (offered.contains(held)) {
                    limit.setDictionaryNormalId(normalValue);
                    limit.setSysUserId(sysUserId);
                    resultLimitService.update(limit);
                    normalHeld = true;
                }
            }
            if (normalValue != null && !normalHeld) {
                ResultLimit limit = new ResultLimit();
                limit.setTestId(testId);
                limit.setComponentId(component.getId());
                limit.setResultTypeId(resultTypeId(type));
                limit.setDictionaryNormalId(normalValue);
                limit.setSysUserId(sysUserId);
                resultLimitService.insert(limit);
            }
        }
    }

    private static String resultTypeId(String type) {
        for (TypeOfTestResultServiceImpl.ResultType resultType : TypeOfTestResultServiceImpl.ResultType.values()) {
            if (resultType.matches(type)) {
                return resultType.getId();
            }
        }
        return TypeOfTestResultServiceImpl.ResultType.DICTIONARY.getId();
    }

    /**
     * OGC-1234: a select-list range names its normal value by dictionary id. Once
     * the component no longer offers that value (the option was removed, a "Copy
     * from test" replaced the options, or the component is no longer a select
     * list), no result can ever equal it, so every result would be judged abnormal
     * and the screens would show a reference value the test does not offer. Such a
     * range is removed. Legacy option rows without a component id count as the
     * primary's. Ranges of inactive components are left alone: nothing reads them.
     */
    private void dropUnreachableDictionaryRanges(String testId, String sysUserId) {
        List<TestResultComponent> components = baseObjectDAO.getActiveComponentsByTestId(testId);
        TestResultComponent primary = pickPrimary(components);
        Map<String, Set<String>> offeredByComponent = new HashMap<>();
        for (TestResultComponent component : components) {
            Set<String> offered = new HashSet<>();
            if (component.getResultType() != null
                    && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(component.getResultType())) {
                for (TestResult option : testResultService.getActiveOptionsByComponentId(component.getId())) {
                    if (option.getValue() != null) {
                        offered.add(option.getValue().trim());
                    }
                }
            }
            offeredByComponent.put(component.getId(), offered);
        }
        if (primary != null) {
            for (TestResult legacy : testResultService.getActiveTestResultsByTest(testId)) {
                if (legacy.getComponentId() == null && legacy.getValue() != null
                        && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(legacy.getTestResultType())) {
                    offeredByComponent.get(primary.getId()).add(legacy.getValue().trim());
                }
            }
        }
        for (ResultLimit range : resultLimitService.getAllResultLimitsForTest(testId)) {
            String normalValue = range.getDictionaryNormalId();
            if (normalValue == null || normalValue.isBlank()) {
                continue;
            }
            String componentId = range.getComponentId() != null ? range.getComponentId()
                    : primary == null ? null : primary.getId();
            Set<String> offered = componentId == null ? null : offeredByComponent.get(componentId);
            if (offered != null && !offered.contains(normalValue.trim())) {
                resultLimitService.delete(range.getId(), sysUserId);
            }
        }
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
        TestResultComponent primary = findPrimaryComponent(testId);
        // The primary's type and digits come from its own options, not from those a
        // catalog row put on another component.
        String primaryId = primary == null ? null : primary.getId();
        List<TestResult> testResults = new ArrayList<>(testResultService.getActiveTestResultsByTest(testId).stream()
                .filter(tr -> tr.getComponentId() == null || tr.getComponentId().equals(primaryId)).toList());
        testResults.sort((a, b) -> Long.compare(parseId(b.getId()), parseId(a.getId())));
        String uomId = test.getUnitOfMeasure() == null ? null : test.getUnitOfMeasure().getId();
        String resultType = latestResultType(testResults);
        Integer significantDigits = latestSignificantDigits(testResults);

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
        for (TestResult tr : testResults) {
            if (tr.getComponentId() == null) {
                tr.setComponentId(primary.getId());
                tr.setSysUserId(sysUserId);
                testResultService.update(tr);
            }
        }
        for (ResultLimit rl : resultLimitService.getAllResultLimitsForTest(testId)) {
            if (rl.getComponentId() == null) {
                rl.setComponentId(primary.getId());
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
            copy.setLod(src.getLod());
            copy.setLoq(src.getLoq());
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
