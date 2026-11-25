package org.openelisglobal.microbiology.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstQcLot;

public interface MicroAstQcLotDAO extends BaseDAO<MicroAstQcLot, String> {
    List<String> getControlIdsByLotIds(Collection<Long> lotIds);
}
