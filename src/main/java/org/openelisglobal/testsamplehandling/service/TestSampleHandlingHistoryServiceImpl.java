package org.openelisglobal.testsamplehandling.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testsamplehandling.dao.TestSampleHandlingHistoryDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandlingHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestSampleHandlingHistoryServiceImpl extends
        AuditableBaseObjectServiceImpl<TestSampleHandlingHistory, String> implements TestSampleHandlingHistoryService {

    @Autowired
    protected TestSampleHandlingHistoryDAO baseObjectDAO;

    TestSampleHandlingHistoryServiceImpl() {
        super(TestSampleHandlingHistory.class);
    }

    @Override
    protected TestSampleHandlingHistoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSampleHandlingHistory> getByHandlingId(String handlingId) {
        return baseObjectDAO.getByHandlingId(handlingId);
    }
}
