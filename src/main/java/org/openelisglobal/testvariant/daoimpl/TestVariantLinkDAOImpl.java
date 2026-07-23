package org.openelisglobal.testvariant.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.testvariant.dao.TestVariantLinkDAO;
import org.openelisglobal.testvariant.valueholder.TestVariantLink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestVariantLinkDAOImpl extends BaseDAOImpl<TestVariantLink, String> implements TestVariantLinkDAO {

    public TestVariantLinkDAOImpl() {
        super(TestVariantLink.class);
    }
}
