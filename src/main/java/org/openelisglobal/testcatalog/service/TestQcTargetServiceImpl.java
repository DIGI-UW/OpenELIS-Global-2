package org.openelisglobal.testcatalog.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.testcatalog.dao.TestQcTargetDAO;
import org.openelisglobal.testcatalog.valueholder.TestQcTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestQcTargetServiceImpl extends BaseObjectServiceImpl<TestQcTarget, String>
        implements TestQcTargetService {

    @Autowired
    protected TestQcTargetDAO baseObjectDAO;

    public TestQcTargetServiceImpl() {
        super(TestQcTarget.class);
    }

    @Override
    protected TestQcTargetDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestQcTarget> getByTestId(String testId) {
        return baseObjectDAO.getByTestId(testId);
    }

    @Override
    @Transactional
    public List<TestQcTarget> saveTargetsForTest(String testId, List<TestQcTarget> desired, String sysUserId) {
        Map<String, TestQcTarget> existingById = new HashMap<>();
        for (TestQcTarget existing : baseObjectDAO.getByTestId(testId)) {
            existingById.put(existing.getId(), existing);
        }
        for (TestQcTarget d : desired) {
            TestQcTarget match = d.getId() == null ? null : existingById.get(d.getId());
            if (match != null) {
                match.setComponentId(blankToNull(d.getComponentId()));
                match.setControlLevel(d.getControlLevel());
                match.setQcControlLotId(blankToNull(d.getQcControlLotId()));
                match.setExpectedValue(d.getExpectedValue());
                match.setUncertainty(d.getUncertainty());
                match.setExpectedDictResultId(blankToNull(d.getExpectedDictResultId()));
                match.setActive(d.isActive());
                match.setSysUserId(sysUserId);
                update(match);
            } else {
                d.setId(UUID.randomUUID().toString());
                d.setTestId(testId);
                d.setComponentId(blankToNull(d.getComponentId()));
                d.setQcControlLotId(blankToNull(d.getQcControlLotId()));
                d.setExpectedDictResultId(blankToNull(d.getExpectedDictResultId()));
                d.setSysUserId(sysUserId);
                insert(d);
            }
        }
        return baseObjectDAO.getByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestQcTarget resolveEffectiveTarget(String testId, String componentId, String controlLevel,
            String qcControlLotId) {
        if (GenericValidator.isBlankOrNull(controlLevel)) {
            return null;
        }
        String component = blankToNull(componentId);
        String lot = blankToNull(qcControlLotId);
        TestQcTarget levelDefault = null;
        for (TestQcTarget target : baseObjectDAO.getByTestId(testId)) {
            if (!target.isActive() || !controlLevel.equalsIgnoreCase(target.getControlLevel())
                    || !Objects.equals(component, blankToNull(target.getComponentId()))) {
                continue;
            }
            String targetLot = blankToNull(target.getQcControlLotId());
            if (lot != null && lot.equals(targetLot)) {
                return target;
            }
            if (targetLot == null && levelDefault == null) {
                levelDefault = target;
            }
        }
        return levelDefault;
    }

    private static String blankToNull(String value) {
        return GenericValidator.isBlankOrNull(value) ? null : value.trim();
    }
}
