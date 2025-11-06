package org.openelisglobal.testdictionary.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testdictionary.dao.TestDictionaryDAO;
import org.openelisglobal.testdictionary.valueholder.TestDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDictionaryServiceImpl extends AuditableBaseObjectServiceImpl<TestDictionary, String>
        implements TestDictionaryService {
    @Autowired
    protected TestDictionaryDAO baseObjectDAO;

    TestDictionaryServiceImpl() {
        super(TestDictionary.class);
    }

    @Override
    protected TestDictionaryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TestDictionary getTestDictionaryForTestId(String testId) {
        return getBaseObjectDAO().getTestDictionaryForTestId(testId);
    }
}
