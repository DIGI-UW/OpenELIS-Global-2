package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroInventoryUsageLink;

public interface MicroInventoryUsageLinkDAO extends BaseDAO<MicroInventoryUsageLink, String> {

    List<MicroInventoryUsageLink> getByCaseId(String caseId);
}
