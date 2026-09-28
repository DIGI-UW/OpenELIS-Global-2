package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;

public interface MicroCriticalCommunicationDAO extends BaseDAO<MicroCriticalCommunication, String> {

    List<MicroCriticalCommunication> getByCaseId(String caseId);

    List<MicroCriticalCommunication> getByCaseIds(List<String> caseIds);

    MicroCriticalCommunication getByAlertId(Long alertId);
}
