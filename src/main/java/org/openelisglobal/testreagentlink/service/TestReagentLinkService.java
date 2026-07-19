package org.openelisglobal.testreagentlink.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;

public interface TestReagentLinkService extends BaseObjectService<TestReagentLink, String> {

    List<TestReagentLink> getByTestId(String testId);

    TestReagentLink getByTestIdAndReagentId(String testId, Long reagentId);
}
