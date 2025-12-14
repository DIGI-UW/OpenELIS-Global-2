package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.pharmaceutical.valueholder.ChainOfCustodyEvent;

public interface ChainOfCustodyEventDAO extends BaseDAO<ChainOfCustodyEvent, Integer> {

    List<ChainOfCustodyEvent> findBySampleId(Integer sampleId);

    List<ChainOfCustodyEvent> findByAliquotId(Integer aliquotId);

    List<ChainOfCustodyEvent> findByAction(ChainOfCustodyEvent.CustodyAction action);

    List<ChainOfCustodyEvent> findPendingApprovals();
}
