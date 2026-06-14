package org.openelisglobal.testresultcomponent.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.dao.TestResultComponentDAO;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestResultComponentServiceImpl extends AuditableBaseObjectServiceImpl<TestResultComponent, String>
        implements TestResultComponentService {

    @Autowired
    protected TestResultComponentDAO baseObjectDAO;

    @Autowired
    private TestResultInterpretationService interpretationService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestResultService testResultService;

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
            TestResultComponent match = d.getId() == null ? null : existingById.get(d.getId());
            if (match != null) {
                // Update the service-loaded (managed) row, never a request-bound entity.
                match.setCode(d.getCode());
                match.setLabel(d.getLabel());
                match.setDisplayOrder(d.getDisplayOrder());
                match.setResultType(d.getResultType());
                match.setUomId(d.getUomId());
                match.setSignificantDigits(d.getSignificantDigits());
                match.setDefaultResult(d.getDefaultResult());
                match.setAllowMultipleReadings(d.getAllowMultipleReadings());
                match.setSysUserId(sysUserId);
                update(match);
                keptIds.add(match.getId());
            } else {
                d.setId(UUID.randomUUID().toString());
                d.setTestId(testId);
                d.setIsActive("Y");
                d.setSysUserId(sysUserId);
                insert(d);
            }
        }
        for (TestResultComponent e : existing) {
            if (!keptIds.contains(e.getId())) {
                e.setIsActive("N");
                e.setSysUserId(sysUserId);
                update(e);
            }
        }
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
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }

    @Override
    @Transactional
    public void copyComponentsFromTest(String sourceTestId, String targetTestId, String sysUserId) {
        Test target = testService.getTestById(targetTestId);
        for (TestResultComponent src : baseObjectDAO.getActiveComponentsByTestId(sourceTestId)) {
            if (getByTestIdAndCode(targetTestId, src.getCode()) != null) {
                continue; // a component with this code already exists on the target
            }
            TestResultComponent copy = new TestResultComponent();
            copy.setTestId(targetTestId);
            copy.setCode(src.getCode());
            copy.setLabel(src.getLabel());
            copy.setDisplayOrder(src.getDisplayOrder());
            copy.setResultType(src.getResultType());
            copy.setUomId(src.getUomId());
            copy.setSignificantDigits(src.getSignificantDigits());
            copy.setDefaultResult(src.getDefaultResult());
            copy.setAllowMultipleReadings(src.getAllowMultipleReadings());
            copy.setIsActive("Y");
            copy.setSysUserId(sysUserId);
            insert(copy);

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
        }
    }
}
