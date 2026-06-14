package org.openelisglobal.testactivation.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testactivation.dao.TestActivationAcknowledgmentDAO;
import org.openelisglobal.testactivation.valueholder.TestActivationAcknowledgment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestActivationAcknowledgmentServiceImpl
        extends AuditableBaseObjectServiceImpl<TestActivationAcknowledgment, String>
        implements TestActivationAcknowledgmentService {

    @Autowired
    protected TestActivationAcknowledgmentDAO baseObjectDAO;

    TestActivationAcknowledgmentServiceImpl() {
        super(TestActivationAcknowledgment.class);
    }

    @Override
    protected TestActivationAcknowledgmentDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
