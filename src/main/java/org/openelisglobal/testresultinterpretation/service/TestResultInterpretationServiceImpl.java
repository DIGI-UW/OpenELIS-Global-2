package org.openelisglobal.testresultinterpretation.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testresultinterpretation.dao.TestResultInterpretationDAO;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestResultInterpretationServiceImpl extends
        AuditableBaseObjectServiceImpl<TestResultInterpretation, String> implements TestResultInterpretationService {

    @Autowired
    protected TestResultInterpretationDAO baseObjectDAO;

    TestResultInterpretationServiceImpl() {
        super(TestResultInterpretation.class);
    }

    @Override
    protected TestResultInterpretationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultInterpretation> getByComponentId(String componentId) {
        return baseObjectDAO.getByComponentId(componentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultInterpretation> getActiveByComponentId(String componentId) {
        return baseObjectDAO.getActiveByComponentId(componentId);
    }

    @Override
    @Transactional
    public List<TestResultInterpretation> saveInterpretationsForComponent(String componentId,
            List<TestResultInterpretation> desired, String sysUserId) {
        List<TestResultInterpretation> existing = baseObjectDAO.getActiveByComponentId(componentId);
        Map<String, TestResultInterpretation> existingById = new HashMap<>();
        for (TestResultInterpretation e : existing) {
            existingById.put(e.getId(), e);
        }
        Set<String> keptIds = new HashSet<>();
        for (TestResultInterpretation d : desired) {
            TestResultInterpretation match = d.getId() == null ? null : existingById.get(d.getId());
            if (match != null) {
                match.setValueMatch(d.getValueMatch());
                match.setInterpretationText(d.getInterpretationText());
                match.setSeverity(d.getSeverity());
                match.setColor(d.getColor());
                match.setDisplayOrder(d.getDisplayOrder());
                match.setSysUserId(sysUserId);
                update(match);
                keptIds.add(match.getId());
            } else {
                d.setId(UUID.randomUUID().toString());
                d.setComponentId(componentId);
                d.setIsActive("Y");
                d.setSysUserId(sysUserId);
                insert(d);
            }
        }
        for (TestResultInterpretation e : existing) {
            if (!keptIds.contains(e.getId())) {
                e.setIsActive("N");
                e.setSysUserId(sysUserId);
                update(e);
            }
        }
        return baseObjectDAO.getActiveByComponentId(componentId);
    }
}
