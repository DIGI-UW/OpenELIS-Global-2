package org.openelisglobal.test.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.test.dao.AssignableTestDAO;
import org.openelisglobal.test.valueholder.AssignableTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssignableTestServiceImpl extends AuditableBaseObjectServiceImpl<AssignableTest, String>
        implements AssignableTestService {
    @Autowired
    protected AssignableTestDAO baseObjectDAO;

    public AssignableTestServiceImpl() {
        super(AssignableTest.class);
    }

    @Override
    protected AssignableTestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
