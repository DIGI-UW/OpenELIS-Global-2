package org.openelisglobal.testcodes.service;

import java.util.ArrayList;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testcodes.dao.TestCodeDAO;
import org.openelisglobal.testcodes.valueholder.TestCode;
import org.openelisglobal.testcodes.valueholder.TestSchemaPK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestCodeServiceImpl extends AuditableBaseObjectServiceImpl<TestCode, TestSchemaPK>
        implements TestCodeService {
    @Autowired
    protected TestCodeDAO baseObjectDAO;

    TestCodeServiceImpl() {
        super(TestCode.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected TestCodeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
