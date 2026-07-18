package org.openelisglobal.testsamplehandling.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testsamplehandling.dao.TestSampleHandlingDAO;
import org.openelisglobal.testsamplehandling.valueholder.TestSampleHandling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestSampleHandlingDAOImpl extends BaseDAOImpl<TestSampleHandling, String>
        implements TestSampleHandlingDAO {

    public TestSampleHandlingDAOImpl() {
        super(TestSampleHandling.class);
    }
}
