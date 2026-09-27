package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;

public interface MicroCaseInoculationDAO extends BaseDAO<MicroCaseInoculation, String> {

    List<MicroCaseInoculation> getByCaseId(String caseId);

    List<MicroCaseInoculation> getByContainerIdentifier(String containerIdentifier);
}
