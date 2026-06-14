package org.openelisglobal.testresultcomponent.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
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
            Map<String, List<TestResultInterpretation>> interpretationsByComponentCode, String sysUserId) {
        // One transaction: components first (so newly inserted rows get ids), then
        // each component's interpretations, keyed by the component's unique code.
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
        return baseObjectDAO.getActiveComponentsByTestId(testId);
    }
}
