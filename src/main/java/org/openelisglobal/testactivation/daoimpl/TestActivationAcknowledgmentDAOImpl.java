package org.openelisglobal.testactivation.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testactivation.dao.TestActivationAcknowledgmentDAO;
import org.openelisglobal.testactivation.valueholder.TestActivationAcknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestActivationAcknowledgmentDAOImpl extends BaseDAOImpl<TestActivationAcknowledgment, String>
        implements TestActivationAcknowledgmentDAO {

    public TestActivationAcknowledgmentDAOImpl() {
        super(TestActivationAcknowledgment.class);
    }
}
