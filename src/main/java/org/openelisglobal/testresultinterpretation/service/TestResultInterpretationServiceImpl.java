package org.openelisglobal.testresultinterpretation.service;

import java.util.List;
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
}
