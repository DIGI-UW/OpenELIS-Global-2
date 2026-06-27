package org.openelisglobal.testterminology.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testterminology.dao.TestTerminologyMappingDAO;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestTerminologyMappingDAOImpl extends BaseDAOImpl<TestTerminologyMapping, String>
        implements TestTerminologyMappingDAO {

    public TestTerminologyMappingDAOImpl() {
        super(TestTerminologyMapping.class);
    }
}
