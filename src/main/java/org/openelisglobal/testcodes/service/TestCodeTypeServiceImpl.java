package org.openelisglobal.testcodes.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testcodes.dao.TestCodeTypeDAO;
import org.openelisglobal.testcodes.valueholder.TestCodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCodeTypeServiceImpl extends AuditableBaseObjectServiceImpl<TestCodeType, String>
        implements TestCodeTypeService {
    @Autowired
    protected TestCodeTypeDAO baseObjectDAO;

    TestCodeTypeServiceImpl() {
        super(TestCodeType.class);
    }

    @Override
    protected TestCodeTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public TestCodeType getTestCodeTypeById(String id) {
        return getBaseObjectDAO().getTestCodeTypeById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCodeType getTestCodeTypeByName(String name) {
        return getBaseObjectDAO().getTestCodeTypeByName(name);
    }
}
