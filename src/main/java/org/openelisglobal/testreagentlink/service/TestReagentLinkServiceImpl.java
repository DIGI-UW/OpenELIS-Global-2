package org.openelisglobal.testreagentlink.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testreagentlink.dao.TestReagentLinkDAO;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestReagentLinkServiceImpl extends AuditableBaseObjectServiceImpl<TestReagentLink, String>
        implements TestReagentLinkService {

    @Autowired
    protected TestReagentLinkDAO baseObjectDAO;

    TestReagentLinkServiceImpl() {
        super(TestReagentLink.class);
    }

    @Override
    protected TestReagentLinkDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReagentLink> getByTestId(String testId) {
        return baseObjectDAO.getByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public TestReagentLink getByTestIdAndReagentId(String testId, Long reagentId) {
        return baseObjectDAO.getByTestIdAndReagentId(testId, reagentId);
    }
}
